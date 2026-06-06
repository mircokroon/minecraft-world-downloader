"""
Web management UI for the Minecraft World Downloader.

Runs as the container's main process. Serves a login-protected dashboard that mirrors every
command-line option of the downloader, starts/stops/restarts the (headless) Java process, streams
its logs, reports status, and lets you download the saved world as a zip.

Configuration via environment variables:
  WEB_USERNAME   dashboard login user           (default: admin)
  WEB_PASSWORD   dashboard login password        (default: changeme)
  WEB_PORT       port for this web UI            (default: 8080)
  SECRET_KEY     Flask session secret            (default: generated + persisted under DATA_DIR)
  JAR_PATH       path to world-downloader.jar    (default: /app/world-downloader.jar)
  DATA_DIR       working dir for worlds/cache    (default: /data)
"""

import io
import os
import time
import json
import hmac
import secrets
import tarfile
import zipfile
import threading
import subprocess
from collections import deque
from functools import wraps

from flask import (
    Flask, request, session, redirect, url_for, render_template,
    jsonify, send_file, abort,
)

import auth

# --------------------------------------------------------------------------------------
# Configuration
# --------------------------------------------------------------------------------------
JAR_PATH = os.environ.get("JAR_PATH", "/app/world-downloader.jar")
DATA_DIR = os.environ.get("DATA_DIR", "/data")
WEB_PORT = int(os.environ.get("WEB_PORT", "8080"))
USERNAME = os.environ.get("WEB_USERNAME", "admin")
# The console itself has no login by default. Set WEB_PASSWORD to put the dashboard behind a
# username/password gate (useful if you expose it beyond localhost). The Minecraft account login
# (Microsoft / token / offline) is always required to download from online-mode servers.
PASSWORD = os.environ.get("WEB_PASSWORD", "")
LOGIN_ENABLED = bool(PASSWORD)

os.makedirs(DATA_DIR, exist_ok=True)
CONFIG_FILE = os.path.join(DATA_DIR, "manager-config.json")


def _load_secret_key():
    key = os.environ.get("SECRET_KEY")
    if key:
        return key
    path = os.path.join(DATA_DIR, ".secret_key")
    if os.path.exists(path):
        with open(path) as fh:
            return fh.read().strip()
    key = secrets.token_hex(32)
    try:
        with open(path, "w") as fh:
            fh.write(key)
        os.chmod(path, 0o600)
    except OSError:
        pass
    return key


app = Flask(__name__)
app.secret_key = _load_secret_key()

# --------------------------------------------------------------------------------------
# Option schema -- mirrors config/Config.java one-to-one
# --------------------------------------------------------------------------------------
# type: text | int | password | bool
OPTIONS = [
    # group, key, flag, type, default, label, help
    ("Connection", "server", "--server", "text", "", "Server address",
     "Remote server hostname or IP (without port). Required."),
    ("Connection", "portLocal", "--local-port", "int", "25565", "Local proxy port",
     "Port the downloader's proxy listens on. Connect Minecraft here (default 25565, mapped in compose)."),
    ("Connection", "disableSrvLookup", "--disable-srv-lookup", "bool", False, "Disable SRV lookup",
     "Disable checking the true address using DNS SRV records."),

    ("World output", "worldOutputDir", "--output", "text", "world", "Output directory",
     "World output directory (relative to /data). Existing worlds are updated."),
    ("World output", "levelSeed", "--seed", "text", "", "Level seed",
     "Numeric level seed for the output world."),
    ("World output", "centerX", "--center-x", "int", "", "Center X",
     "Offsets the world: this X is placed at origin (rounded to 512). Requires Center Z."),
    ("World output", "centerZ", "--center-z", "int", "", "Center Z",
     "Offsets the world: this Z is placed at origin (rounded to 512). Requires Center X."),
    ("World output", "disableWorldGen", "--disable-world-gen", "bool", False, "Superflat void",
     "Set world type to a superflat void to prevent new chunks from generating."),
    ("World output", "disableWriteChunks", "--disable-chunk-saving", "bool", False, "Disable chunk saving",
     "Do not write chunks to disk (debugging)."),
    ("World output", "ignoreBlockChanges", "--ignore-block-changes", "bool", False, "Ignore block changes",
     "Ignore changes to chunks after they have been loaded."),

    ("Render distance & map", "extendedRenderDistance", "--extended-render-distance", "int", "0",
     "Extended render distance",
     "Send downloaded chunks back to the client to extend render distance (0 = off)."),
    ("Render distance & map", "renderOtherPlayers", "--render-players", "bool", False, "Render players",
     "Show other players on the overview map."),
    ("Render distance & map", "markNewChunks", "--mark-new-chunks", "bool", False, "Mark new chunks",
     "Mark new chunks with an orange outline."),
    ("Render distance & map", "markOldChunks", "--mark-old-chunks", "bool", True, "Mark old chunks",
     "Grey out old chunks on the map (on by default; cannot be turned off via CLI)."),
    ("Render distance & map", "disableMarkUnsavedChunks", "--disable-mark-unsaved", "bool", False,
     "Disable unsaved marking", "Disable marking unsaved chunks in red on the map."),
    ("Render distance & map", "drawExtendedChunks", "--draw-extended-chunks", "bool", False,
     "Draw extended chunks", "Draw extended (re-sent) chunks to the map."),
    ("Render distance & map", "enableCaveRenderMode", "--enable-cave-mode", "bool", False, "Cave render mode",
     "Automatically switch to cave render mode when underground."),

    ("Advanced", "disableInfoMessages", "--disable-messages", "bool", False, "Disable info messages",
     "Disable various info messages (e.g. chest saving)."),
    ("Advanced", "devMode", "--dev-mode", "bool", False, "Developer mode",
     "Enable developer mode."),
]

BOOL_TRUE = {"1", "true", "on", "yes"}


def options_by_group():
    groups = {}
    for opt in OPTIONS:
        group, key, flag, typ, default, label, help_text = opt
        groups.setdefault(group, []).append({
            "key": key, "flag": flag, "type": typ, "default": default,
            "label": label, "help": help_text,
        })
    return groups


# --------------------------------------------------------------------------------------
# Persisted config
# --------------------------------------------------------------------------------------
def load_config():
    if os.path.exists(CONFIG_FILE):
        try:
            with open(CONFIG_FILE) as fh:
                return json.load(fh)
        except (OSError, ValueError):
            pass
    cfg = {}
    for _g, key, _f, typ, default, _l, _h in OPTIONS:
        cfg[key] = bool(default) if typ == "bool" else ("" if default is None else str(default))
    return cfg


def save_config(cfg):
    # never persist the access token to disk
    safe = dict(cfg)
    safe.pop("accessToken", None)
    try:
        with open(CONFIG_FILE, "w") as fh:
            json.dump(safe, fh, indent=2)
    except OSError:
        pass


# --------------------------------------------------------------------------------------
# Downloader process management
# --------------------------------------------------------------------------------------
class Downloader:
    def __init__(self):
        self.proc = None
        self.lock = threading.Lock()
        self.log = deque(maxlen=5000)
        self.log_total = 0
        self.log_lock = threading.Lock()
        self.started_at = None
        self.last_config = {}

    # ---- logging ----
    def _append_log(self, line):
        with self.log_lock:
            self.log.append(line)
            self.log_total += 1

    def logs_since(self, since):
        with self.log_lock:
            start = self.log_total - len(self.log)
            if since < start:
                since = start
            offset = since - start
            return self.log_total, list(self.log)[offset:]

    def _reader(self, proc):
        try:
            for line in iter(proc.stdout.readline, ""):
                if line == "":
                    break
                self._append_log(line.rstrip("\n"))
        finally:
            code = proc.wait()
            self._append_log("")
            self._append_log("=== downloader process exited (code %s) ===" % code)

    # ---- state ----
    def is_running(self):
        return self.proc is not None and self.proc.poll() is None

    def status(self):
        running = self.is_running()
        return {
            "running": running,
            "pid": self.proc.pid if running else None,
            "uptime": int(time.time() - self.started_at) if running and self.started_at else 0,
            "config": {k: v for k, v in self.last_config.items() if k != "accessToken"},
            "jar": JAR_PATH,
            "proxy_port": self.last_config.get("portLocal", "25565") if running else None,
        }

    # ---- command construction ----
    def build_command(self, cfg, creds=None):
        cmd = ["java", "-Djava.awt.headless=true", "-jar", JAR_PATH, "--no-gui"]
        server = (cfg.get("server") or "").strip()
        if not server:
            raise ValueError("Server address is required.")
        cmd += ["--server", server]

        cx = (str(cfg.get("centerX", "")) or "").strip()
        cz = (str(cfg.get("centerZ", "")) or "").strip()
        include_center = cx != "" and cz != ""

        for _g, key, flag, typ, _default, _label, _help in OPTIONS:
            if key == "server":
                continue
            if key in ("centerX", "centerZ"):
                continue
            raw = cfg.get(key)
            if typ == "bool":
                if (str(raw).lower() in BOOL_TRUE) or raw is True:
                    cmd.append(flag)
            else:
                val = "" if raw is None else str(raw).strip()
                if val != "":
                    cmd += [flag, val]

        if include_center:
            cmd += ["--center-x", cx, "--center-z", cz]

        if creds:
            if creds.get("username"):
                cmd += ["--username", creds["username"]]
            if creds.get("mc_token"):
                cmd += ["--token", creds["mc_token"]]
        return cmd

    # ---- lifecycle ----
    def start(self, cfg, creds=None):
        with self.lock:
            if self.is_running():
                return False, "Downloader is already running."
            cmd = self.build_command(cfg, creds)
            with self.log_lock:
                self.log.clear()
                self.log_total = 0
            token = (creds or {}).get("mc_token")
            redacted = " ".join("***" if token and c == token else c for c in cmd)
            self._append_log("$ " + redacted)
            try:
                self.proc = subprocess.Popen(
                    cmd, cwd=DATA_DIR, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                    text=True, bufsize=1,
                )
            except OSError as exc:
                self._append_log("Failed to start: %s" % exc)
                return False, str(exc)
            self.started_at = time.time()
            self.last_config = dict(cfg)
            threading.Thread(target=self._reader, args=(self.proc,), daemon=True).start()
            return True, "Started."

    def stop(self):
        with self.lock:
            if not self.is_running():
                return False, "Downloader is not running."
            self._append_log("=== stopping downloader ===")
            try:
                self.proc.terminate()
                try:
                    self.proc.wait(timeout=10)
                except subprocess.TimeoutExpired:
                    self.proc.kill()
            except OSError:
                pass
            return True, "Stopped."


downloader = Downloader()


# --------------------------------------------------------------------------------------
# Minecraft account store (Microsoft / manual / offline) -- persisted to /data/auth.json
# --------------------------------------------------------------------------------------
class AuthStore:
    FILE = os.path.join(DATA_DIR, "auth.json")

    def __init__(self):
        self.lock = threading.Lock()
        try:
            with open(self.FILE) as fh:
                self.data = json.load(fh)
        except (OSError, ValueError):
            self.data = {}

    def _save(self):
        try:
            with open(self.FILE, "w") as fh:
                json.dump(self.data, fh, indent=2)
            os.chmod(self.FILE, 0o600)
        except OSError:
            pass

    def set(self, data):
        with self.lock:
            self.data = data
            self._save()

    def clear(self):
        with self.lock:
            self.data = {}
            try:
                os.remove(self.FILE)
            except OSError:
                pass

    def public_status(self):
        with self.lock:
            d = self.data
            return {
                "authenticated": bool(d.get("method")),
                "method": d.get("method"),
                "username": d.get("username"),
                "uuid": d.get("uuid"),
            }

    def credentials_for_launch(self):
        """Return {username, mc_token?}, refreshing the Microsoft token if it is getting old."""
        with self.lock:
            d = dict(self.data)
        method = d.get("method")
        if not method:
            return {}
        if method == "offline":
            return {"username": d.get("username")}
        if method == "manual":
            return {"username": d.get("username"), "mc_token": d.get("mc_token")}
        if method == "microsoft":
            age = time.time() - d.get("mc_obtained", 0)
            if age > 20 * 3600 and d.get("ms_refresh_token"):
                refreshed = auth.refresh_microsoft(d["ms_refresh_token"])
                d.update({
                    "mc_token": refreshed["mc_token"],
                    "username": refreshed["username"],
                    "uuid": refreshed["uuid"],
                    "ms_refresh_token": refreshed.get("ms_refresh_token", d["ms_refresh_token"]),
                    "mc_obtained": time.time(),
                })
                self.set(d)
            return {"username": d.get("username"), "mc_token": d.get("mc_token")}
        return {}


auth_store = AuthStore()


# --------------------------------------------------------------------------------------
# Web-console login (separate from Minecraft account auth above)
# --------------------------------------------------------------------------------------
def login_required(view):
    @wraps(view)
    def wrapped(*args, **kwargs):
        if LOGIN_ENABLED and not session.get("auth"):
            if request.path.startswith("/api/"):
                return jsonify({"error": "unauthorized"}), 401
            return redirect(url_for("login", next=request.path))
        return view(*args, **kwargs)
    return wrapped


@app.route("/login", methods=["GET", "POST"])
def login():
    if not LOGIN_ENABLED:
        return redirect(url_for("index"))
    error = None
    if request.method == "POST":
        user_ok = hmac.compare_digest(request.form.get("username", ""), USERNAME)
        pass_ok = hmac.compare_digest(request.form.get("password", ""), PASSWORD)
        if user_ok and pass_ok:
            session["auth"] = True
            session.permanent = True
            nxt = request.args.get("next") or url_for("index")
            return redirect(nxt)
        error = "Invalid username or password."
    return render_template("login.html", error=error)


@app.route("/logout")
def logout():
    session.clear()
    return redirect(url_for("login"))


# --------------------------------------------------------------------------------------
# Dashboard + API
# --------------------------------------------------------------------------------------
@app.route("/")
@login_required
def index():
    cfg = load_config()
    return render_template(
        "dashboard.html",
        groups=options_by_group(),
        config=cfg,
        username=USERNAME,
        login_enabled=LOGIN_ENABLED,
    )


@app.route("/api/status")
@login_required
def api_status():
    return jsonify(downloader.status())


@app.route("/api/logs")
@login_required
def api_logs():
    since = request.args.get("since", default=0, type=int)
    total, lines = downloader.logs_since(since)
    return jsonify({"total": total, "lines": lines})


def _collect_cfg():
    cfg = {}
    for _g, key, _f, typ, _default, _label, _help in OPTIONS:
        if typ == "bool":
            cfg[key] = request.form.get(key, "").lower() in BOOL_TRUE
        else:
            cfg[key] = request.form.get(key, "").strip()
    return cfg


def _launch(cfg):
    try:
        creds = auth_store.credentials_for_launch()
    except Exception as exc:  # noqa: BLE001 - surface any auth/refresh failure to the UI
        return jsonify({"ok": False, "message": "Account error: %s" % exc,
                        "status": downloader.status()}), 409
    ok, msg = downloader.start(cfg, creds)
    return jsonify({"ok": ok, "message": msg, "status": downloader.status()}), (200 if ok else 409)


@app.route("/api/save", methods=["POST"])
@login_required
def api_save():
    save_config(_collect_cfg())
    return jsonify({"ok": True, "message": "Settings saved."})


@app.route("/api/start", methods=["POST"])
@login_required
def api_start():
    cfg = _collect_cfg()
    save_config(cfg)
    return _launch(cfg)


@app.route("/api/stop", methods=["POST"])
@login_required
def api_stop():
    ok, msg = downloader.stop()
    return jsonify({"ok": ok, "message": msg, "status": downloader.status()}), (200 if ok else 409)


@app.route("/api/restart", methods=["POST"])
@login_required
def api_restart():
    downloader.stop()
    time.sleep(1.0)
    cfg = _collect_cfg() if request.form else load_config()
    save_config(cfg)
    return _launch(cfg)


# ---- Minecraft account authentication ----
@app.route("/api/auth/status")
@login_required
def api_auth_status():
    return jsonify(auth_store.public_status())


@app.route("/api/auth/microsoft/start", methods=["POST"])
@login_required
def api_auth_ms_start():
    try:
        return jsonify(auth.begin_device_flow())
    except Exception as exc:  # noqa: BLE001
        return jsonify({"error": str(exc)}), 502


@app.route("/api/auth/microsoft/poll")
@login_required
def api_auth_ms_poll():
    flow_id = request.args.get("flow", "")
    try:
        state, payload = auth.poll_device_flow(flow_id)
    except Exception as exc:  # noqa: BLE001
        return jsonify({"state": "error", "message": str(exc)})
    if state == "ok":
        auth_store.set({
            "method": "microsoft",
            "username": payload.get("username"),
            "uuid": payload.get("uuid"),
            "mc_token": payload.get("mc_token"),
            "ms_refresh_token": payload.get("ms_refresh_token"),
            "mc_obtained": time.time(),
        })
        return jsonify({"state": "ok", "username": payload.get("username")})
    return jsonify({"state": state, "message": payload.get("message", "")})


@app.route("/api/auth/manual", methods=["POST"])
@login_required
def api_auth_manual():
    token = request.form.get("token", "").strip()
    if not token:
        return jsonify({"ok": False, "message": "Access token is required."}), 400
    try:
        profile = auth.profile_from_token(token)
    except Exception as exc:  # noqa: BLE001
        return jsonify({"ok": False, "message": str(exc)}), 400
    auth_store.set({
        "method": "manual",
        "username": profile.get("username"),
        "uuid": profile.get("uuid"),
        "mc_token": token,
    })
    return jsonify({"ok": True, "username": profile.get("username")})


@app.route("/api/auth/offline", methods=["POST"])
@login_required
def api_auth_offline():
    username = request.form.get("username", "").strip()
    if not username:
        return jsonify({"ok": False, "message": "Username is required."}), 400
    auth_store.set({"method": "offline", "username": username})
    return jsonify({"ok": True, "username": username})


@app.route("/api/auth/logout", methods=["POST"])
@login_required
def api_auth_logout():
    auth_store.clear()
    return jsonify({"ok": True})


def _world_path():
    cfg = load_config()
    world = cfg.get("worldOutputDir") or "world"
    path = os.path.normpath(os.path.join(DATA_DIR, world))
    if not path.startswith(os.path.normpath(DATA_DIR)):
        abort(400)
    return path


@app.route("/api/world-info")
@login_required
def api_world_info():
    path = _world_path()
    exists = os.path.isdir(path)
    size, files = 0, 0
    if exists:
        for root, _d, fs in os.walk(path):
            for f in fs:
                try:
                    size += os.path.getsize(os.path.join(root, f))
                    files += 1
                except OSError:
                    pass
    return jsonify({"exists": exists, "path": path, "size": size, "files": files,
                    "has_world": exists and files > 0})


def _has_world_files(path):
    if not os.path.isdir(path):
        return False
    for _root, _dirs, files in os.walk(path):
        if files:
            return True
    return False


@app.route("/api/download")
@login_required
def api_download():
    path = _world_path()
    if not _has_world_files(path):
        abort(404)
    fmt = request.args.get("fmt", "zip")
    name = os.path.basename(path) or "world"
    buf = io.BytesIO()
    if fmt == "tar":
        with tarfile.open(fileobj=buf, mode="w:gz") as tf:
            tf.add(path, arcname=name)
        buf.seek(0)
        return send_file(buf, mimetype="application/gzip", as_attachment=True,
                         download_name="%s.tar.gz" % name)
    with zipfile.ZipFile(buf, "w", zipfile.ZIP_DEFLATED) as zf:
        for root, _dirs, files in os.walk(path):
            for fname in files:
                full = os.path.join(root, fname)
                zf.write(full, os.path.relpath(full, os.path.dirname(path)))
    buf.seek(0)
    return send_file(buf, mimetype="application/zip", as_attachment=True,
                     download_name="%s.zip" % name)


@app.route("/api/export-dir", methods=["POST"])
@login_required
def api_export_dir():
    """Copy the world into /data/exports/<name>-<ts> so it can be taken from the mounted volume."""
    import shutil
    path = _world_path()
    if not os.path.isdir(path):
        return jsonify({"ok": False, "message": "No world directory yet."}), 404
    ts = time.strftime("%Y%m%d-%H%M%S", time.gmtime())
    dest_root = os.path.join(DATA_DIR, "exports")
    os.makedirs(dest_root, exist_ok=True)
    dest = os.path.join(dest_root, "%s-%s" % (os.path.basename(path) or "world", ts))
    shutil.copytree(path, dest)
    return jsonify({"ok": True, "message": "Copied to %s (available in the mounted ./data volume)." % dest})


@app.route("/healthz")
def healthz():
    return jsonify({"ok": True, "running": downloader.is_running()})


if __name__ == "__main__":
    print("Minecraft World Downloader web manager on :%d (console login %s)" % (
        WEB_PORT, "enabled" if LOGIN_ENABLED else "disabled"), flush=True)
    try:
        from waitress import serve
        serve(app, host="0.0.0.0", port=WEB_PORT, threads=8)
    except ImportError:
        app.run(host="0.0.0.0", port=WEB_PORT, threaded=True)
