"""
Minecraft account authentication for the web console.

Supports three login methods (mirroring the desktop app's options):
  * microsoft - OAuth 2.0 device-code flow (ideal for headless/web), then the Xbox Live -> XSTS ->
                Minecraft Services token chain. The resulting Minecraft access token + username are
                passed to the downloader via --token / --username.
  * manual    - paste an existing Minecraft access token; we look up the profile for the username/uuid.
  * offline   - just a username (for offline-mode / cracked servers); no token.

Microsoft tokens are refreshed automatically using the stored refresh token.

The client id defaults to the public Minecraft launcher client id, overridable with MS_CLIENT_ID.
"""

import os
import time
import json
import threading

import requests

MS_CLIENT_ID = os.environ.get("MS_CLIENT_ID", "00000000402b5328")
# The public Minecraft launcher client id is a legacy login.live.com client, so we use the live.com
# device-code endpoints with the MBI_SSL scope (and a "t=" RPS ticket below). If you register your own
# Azure AD app instead, set MS_CLIENT_ID / MS_AUTHORITY / MS_SCOPE accordingly.
DEVICE_CODE_URL = os.environ.get("MS_DEVICE_CODE_URL", "https://login.live.com/oauth20_connect.srf")
TOKEN_URL = os.environ.get("MS_TOKEN_URL", "https://login.live.com/oauth20_token.srf")
SCOPE = os.environ.get("MS_SCOPE", "service::user.auth.xboxlive.com::MBI_SSL")

XBL_URL = "https://user.auth.xboxlive.com/user/authenticate"
XSTS_URL = "https://xsts.auth.xboxlive.com/xsts/authorize"
MC_LOGIN_URL = "https://api.minecraftservices.com/authentication/login_with_xbox"
MC_PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile"

DEVICE_GRANT = "urn:ietf:params:oauth:grant-type:device_code"
TIMEOUT = 25


class AuthError(Exception):
    pass


# --------------------------------------------------------------------------------------
# Microsoft -> Minecraft token chain
# --------------------------------------------------------------------------------------
def _xbox_to_minecraft(ms_access_token):
    xbl = requests.post(XBL_URL, json={
        "Properties": {
            "AuthMethod": "RPS",
            "SiteName": "user.auth.xboxlive.com",
            "RpsTicket": "t=" + ms_access_token,
        },
        "RelyingParty": "http://auth.xboxlive.com",
        "TokenType": "JWT",
    }, timeout=TIMEOUT)
    xbl.raise_for_status()
    xbl_json = xbl.json()
    xbl_token = xbl_json["Token"]
    uhs = xbl_json["DisplayClaims"]["xui"][0]["uhs"]

    xsts = requests.post(XSTS_URL, json={
        "Properties": {"SandboxId": "RETAIL", "UserTokens": [xbl_token]},
        "RelyingParty": "rp://api.minecraftservices.com/",
        "TokenType": "JWT",
    }, timeout=TIMEOUT)
    if xsts.status_code == 401:
        xerr = str(xsts.json().get("XErr"))
        reasons = {
            "2148916233": "This account has no Xbox profile. Sign up for Xbox first.",
            "2148916235": "Xbox Live is not available in this account's region.",
            "2148916236": "Adult verification required (South Korea).",
            "2148916237": "Adult verification required (South Korea).",
            "2148916238": "This is a child account and must be added to a family.",
        }
        raise AuthError(reasons.get(xerr, "Xbox authorization failed (XErr %s)." % xerr))
    xsts.raise_for_status()
    xsts_json = xsts.json()
    xsts_token = xsts_json["Token"]

    mc = requests.post(MC_LOGIN_URL, json={
        "identityToken": "XBL3.0 x=%s;%s" % (uhs, xsts_token),
    }, timeout=TIMEOUT)
    mc.raise_for_status()
    mc_token = mc.json()["access_token"]

    profile = profile_from_token(mc_token)
    profile["mc_token"] = mc_token
    return profile


def profile_from_token(mc_token):
    resp = requests.get(MC_PROFILE_URL, headers={"Authorization": "Bearer " + mc_token}, timeout=TIMEOUT)
    if resp.status_code == 404:
        raise AuthError("This account does not own Minecraft: Java Edition.")
    if resp.status_code == 401:
        raise AuthError("The access token is invalid or expired.")
    resp.raise_for_status()
    data = resp.json()
    return {"username": data.get("name"), "uuid": data.get("id")}


def refresh_microsoft(refresh_token):
    resp = requests.post(TOKEN_URL, data={
        "grant_type": "refresh_token",
        "client_id": MS_CLIENT_ID,
        "refresh_token": refresh_token,
        "scope": SCOPE,
    }, timeout=TIMEOUT)
    resp.raise_for_status()
    tok = resp.json()
    result = _xbox_to_minecraft(tok["access_token"])
    result["ms_refresh_token"] = tok.get("refresh_token", refresh_token)
    return result


# --------------------------------------------------------------------------------------
# Device-code flow (in-memory pending flows)
# --------------------------------------------------------------------------------------
_flows = {}
_flows_lock = threading.Lock()


def begin_device_flow():
    resp = requests.post(DEVICE_CODE_URL, data={
        "client_id": MS_CLIENT_ID,
        "scope": SCOPE,
        "response_type": "device_code",
    }, timeout=TIMEOUT)
    resp.raise_for_status()
    data = resp.json()
    flow_id = os.urandom(12).hex()
    with _flows_lock:
        _flows[flow_id] = {
            "device_code": data["device_code"],
            "interval": int(data.get("interval", 5)),
            "expires_at": time.time() + int(data.get("expires_in", 900)),
            "last_poll": 0.0,
        }
    return {
        "flow_id": flow_id,
        "user_code": data["user_code"],
        "verification_uri": data.get("verification_uri", "https://www.microsoft.com/link"),
        "message": data.get("message", ""),
        "interval": int(data.get("interval", 5)),
        "expires_in": int(data.get("expires_in", 900)),
    }


def poll_device_flow(flow_id):
    """Return (state, payload). state in {pending, ok, error}."""
    with _flows_lock:
        flow = _flows.get(flow_id)
    if not flow:
        return "error", {"message": "Unknown or expired login attempt."}
    if time.time() > flow["expires_at"]:
        with _flows_lock:
            _flows.pop(flow_id, None)
        return "error", {"message": "Login timed out. Please try again."}
    # respect the interval to avoid slow_down
    if time.time() - flow["last_poll"] < flow["interval"]:
        return "pending", {}
    flow["last_poll"] = time.time()

    resp = requests.post(TOKEN_URL, data={
        "grant_type": DEVICE_GRANT,
        "client_id": MS_CLIENT_ID,
        "device_code": flow["device_code"],
    }, timeout=TIMEOUT)
    data = resp.json()
    if resp.status_code == 200:
        with _flows_lock:
            _flows.pop(flow_id, None)
        try:
            result = _xbox_to_minecraft(data["access_token"])
            result["ms_refresh_token"] = data.get("refresh_token")
            return "ok", result
        except (AuthError, requests.RequestException, KeyError) as exc:
            return "error", {"message": str(exc)}

    err = data.get("error")
    if err in ("authorization_pending", "slow_down"):
        if err == "slow_down":
            flow["interval"] += 5
        return "pending", {}
    with _flows_lock:
        _flows.pop(flow_id, None)
    messages = {
        "authorization_declined": "Login was declined.",
        "expired_token": "Login expired. Please try again.",
        "bad_verification_code": "Verification failed. Please try again.",
    }
    return "error", {"message": messages.get(err, data.get("error_description", "Login failed."))}
