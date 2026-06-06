# Minecraft World Downloader

A proxy that sits between your Minecraft client and a multiplayer server and saves the chunks,
block entities and inventories you encounter into a normal singleplayer world — **no client mods
required**, works with vanilla or any client.

## Contents
- **[Installation](Installation)** — download the jar or run it with Docker
- **[Docker & Web Console](Docker-Web-Console)** — run headless with a browser UI
- **[Authentication](Authentication)** — sign in to your Minecraft account
- **[Supported Versions](Supported-Versions)** — every version from 1.8 to 26.1
- **[Command-Line Options](Command-Line-Options)** — every flag, explained
- **[Building From Source](Building-From-Source)**
- **[FAQ](FAQ)**

## How it works
1. Start the downloader — it opens a local proxy server (default port **25565**).
2. In Minecraft, connect to **`localhost`** instead of the real server address.
3. The downloader transparently forwards your connection to the real server and records every chunk,
   block entity and inventory you load.
4. Walk or fly around to capture the area; the world is written to the output directory as a normal
   Minecraft save you can open in singleplayer.

## Highlights
- Requires no client modifications — works with every client, vanilla or modded.
- Automatically merges into previous downloads or an existing world.
- Saves chests and other inventories when you open them.
- Can extend the client's render distance by re-sending previously downloaded chunks.
- Overview map of saved chunks.
- **Supports Minecraft 1.8 → 26.1** (see [Supported Versions](Supported-Versions)).
- Optional **[Docker web console](Docker-Web-Console)** to run and manage it from a browser.

## Quick start
**Jar (GUI):** download `world-downloader.jar`, run `java -jar world-downloader.jar`, enter the server
address and press start, then connect to `localhost` in Minecraft.

**Docker (web console):** `docker compose up -d --build`, open <http://localhost:8080>, sign in to your
Minecraft account, enter the server, press Start, then connect Minecraft to `localhost:25565`.

> For problems, see the [FAQ](FAQ) or open an issue on GitHub.
