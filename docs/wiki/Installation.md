# Installation

## Requirements
- **Java 21 or higher** (to run the jar) — or **Docker** (no Java needed on the host).
- A supported Minecraft version — see [Supported Versions](Supported-Versions) (1.8 → 26.1).

## Option 1 — Windows launcher (easiest)
Download [`world-downloader-launcher.exe`](https://github.com/cafepromenade/minecraft-world-downloader-launcher/releases/latest/download/world-downloader-launcher.exe)
and run it. Enter the server address and press **Start**, then connect to `localhost` in Minecraft.

## Option 2 — Cross-platform jar
Download [`world-downloader.jar`](https://github.com/cafepromenade/minecraft-world-downloader/releases/latest/download/world-downloader.jar)
and run it:

```bash
java -jar world-downloader.jar
```

This opens the GUI. Enter the server address, press start, and connect to `localhost` in Minecraft.

To run **headless** (no GUI), pass the server with `--no-gui`:

```bash
java -jar world-downloader.jar --no-gui -s address.to.server.com
```

See [Command-Line Options](Command-Line-Options) for every flag.

### Linux notes
```bash
wget https://github.com/cafepromenade/minecraft-world-downloader/releases/latest/download/world-downloader.jar
java -jar world-downloader.jar -s address.to.server.com --no-gui
```
Some distributions need `-Djdk.gtk.version=2` for the GUI:
```bash
java -Djdk.gtk.version=2 -jar world-downloader.jar
```

## Option 3 — Docker + web console
Run the downloader headless behind a browser-based management console. See
**[Docker & Web Console](Docker-Web-Console)**.

```bash
docker compose up -d --build
# open http://localhost:8080
```

## First use
1. Start the downloader and point it at your server.
2. In Minecraft, **connect to `localhost`** (not the real server address).
3. Walk/fly around to load chunks — they're saved to the output directory (`world` by default).
4. Open the saved world in singleplayer.

For online-mode servers you must sign in to your Minecraft account — see [Authentication](Authentication).
