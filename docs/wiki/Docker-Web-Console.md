# Docker & Web Console

Run the downloader headless in a container, managed from a small web console in your browser. The
console **mirrors every command-line option** and adds account login, live logs, and world export.

## Quick start
```bash
git clone https://github.com/cafepromenade/minecraft-world-downloader
cd minecraft-world-downloader
docker compose up -d --build
```
Then open **<http://localhost:8080>**.

| Port | Purpose |
| ---- | ------- |
| 8080 | Web management console |
| 25565 | Minecraft proxy — connect your client here |

## Using the console
1. **Minecraft account** — sign in (see [Authentication](Authentication)): Microsoft, access token, or
   offline username. Required for online-mode servers.
2. **Configuration** — set the server address and any options (these are the same as the
   [command-line flags](Command-Line-Options)).
3. Press **Start**. Then connect your Minecraft client to **`localhost:25565`**.
4. Watch **live logs** and status; **Stop** / **Restart** as needed.
5. **Save settings** to persist them, and **export** the finished world.

## Exporting the world
- **Download .zip** or **Download .tar.gz** — get an archive of the world directly from the browser.
- **Export directory** — copies a timestamped snapshot into `./data/exports`.
- The live world is also always on the host at **`./data/<output-dir>`** (the mounted volume).

## Persistence
Everything is stored in the **`./data`** folder (mounted at `/data` in the container):
- downloaded worlds and `./data/exports` snapshots,
- the Minecraft registry cache (downloaded server jars used to read newer versions),
- your account session (`auth.json`) and saved console settings (`manager-config.json`).

## Console login (optional)
The console has **no login by default** — anyone who can reach port 8080 can use it. If you expose it
beyond `localhost`, gate it behind a username/password by setting **both** `WEB_USERNAME` and
`WEB_PASSWORD`.

## Environment variables
| Variable | Default | Purpose |
| -------- | ------- | ------- |
| `WEB_PORT` | `8080` | Port for the web console |
| `WEB_USERNAME` | *(unset)* | Console login user (set with `WEB_PASSWORD` to enable login) |
| `WEB_PASSWORD` | *(unset)* | Console login password — enables the login gate when set |
| `SECRET_KEY` | auto-generated | Flask session secret (only used when login is enabled) |
| `MS_CLIENT_ID` | public launcher id | Microsoft OAuth client id for account login |
| `JAR_PATH` | `/app/world-downloader.jar` | Path to the downloader jar |
| `DATA_DIR` | `/data` | Working directory for worlds/cache/settings |

## Changing ports
If host port **25565** is already in use (e.g. you run another server), remap it in
`docker-compose.yml`, e.g. `"25566:25565"`, then connect Minecraft to `localhost:25566`. The container's
internal proxy port can also be changed with the **Local proxy port** option, but keep the published
mapping in sync.

## Running without compose
```bash
docker build -t minecraft-world-downloader .
docker run -d --name mwd \
  -p 8080:8080 -p 25565:25565 \
  -v "$PWD/data:/data" \
  minecraft-world-downloader
```

## Updating
```bash
git pull
docker compose up -d --build
```

## Architecture
The container's main process is a small Python (Flask) web app. When you press **Start** it launches the
downloader jar headless (`java -jar world-downloader.jar --no-gui -s <server> …`) with your chosen flags
and any account credentials, captures its output for the log view, and supervises the process. Stopping
the console stops the downloader.
