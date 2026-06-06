# minecraft-world-downloader
A Minecraft world downloader that works as a proxy server between the client and the server to read & save chunk data. Download multiplayer worlds by connecting to them and walking around. Chunks can be sent back to the client to extend the render distance.

> **This fork adds:** support for **every version from 1.8 through 26.1**, a fix for the 1.20.2+
> *"Connection Lost — Loading NBT data"* error, and a **Dockerized web console** for running and managing
> the downloader from your browser (Microsoft / access-token / offline account login, live logs, and
> world export).

### 📖 Documentation
Full guides are in [**`docs/wiki/`**](docs/wiki) (also published to the project
[wiki](https://github.com/cafepromenade/minecraft-world-downloader/wiki)):
[Installation](docs/wiki/Installation.md) ·
[Docker & web console](docs/wiki/Docker-Web-Console.md) ·
[Authentication](docs/wiki/Authentication.md) ·
[Supported versions](docs/wiki/Supported-Versions.md) ·
[Command-line options](docs/wiki/Command-Line-Options.md) ·
[Building from source](docs/wiki/Building-From-Source.md) ·
[FAQ](docs/wiki/FAQ.md)

### Downloads  <a href="https://github.com/cafepromenade/minecraft-world-downloader/releases/latest"><img align="right" src="https://img.shields.io/github/downloads/cafepromenade/minecraft-world-downloader/total.svg"></a>
Windows launcher: [world-downloader-launcher.exe](https://github.com/cafepromenade/minecraft-world-downloader-launcher/releases/latest/download/world-downloader-launcher.exe)

Latest cross-platform jar (command-line support): [world-downloader.jar](https://github.com/cafepromenade/minecraft-world-downloader/releases/latest/download/world-downloader.jar)

### Basic usage
[Download](https://github.com/cafepromenade/minecraft-world-downloader-launcher/releases/latest/download/world-downloader-launcher.exe) the latest release and run it. Enter the server address in the address field and press start.

<img src="https://i.imgur.com/yH8SH5C.png">

Instead of connecting to the server itself, connect to `localhost` in Minecraft to start downloading the world.
<img src="https://i.imgur.com/wKMnXfq.png">

If you run into any problems, check the [FAQ](https://github.com/cafepromenade/minecraft-world-downloader/wiki/FAQ) page for some common issues. 

### [Features](https://github.com/cafepromenade/minecraft-world-downloader/wiki/Features)
- Requires no client modifications and as such works with every game client, vanilla or not
- Automatically merge into previous downloads or existing worlds
- Save chests and other inventories by opening them
- Extend the client's render distance by sending chunks downloaded previously back to the client
- Overview map of chunks that have been saved:

<img src="https://i.imgur.com/7FIJ6fZ.png" width="80%" title="Example of the GUI showing all the downloaded chunks as white squares, which ones from a previous download greyed out.">

### Requirements
- Java 21 or higher
- Minecraft version 1.8+ // 1.9+ // 1.10+ // 1.11+ // 1.12.2+ // 1.13.2+ // 1.14.1+ // 1.15.2+ // 1.16.2+ // 1.17+ // 1.18+ // 1.19.3+ // 1.20+ // 1.21+ // 26.1

### Command-line
[Download](https://github.com/cafepromenade/minecraft-world-downloader/releases/latest/download/world-downloader.jar) the cross-platform `world-downloader.jar` and run it using the command-line:

```
java -jar world-downloader.jar
```

Arguments can be specified to change the behaviour of the downloader. Running with `--help` shows all the available commands.
```
java -jar world-downloader.jar --help
```

The GUI can be disabled by including the `--no-gui` option, and specifying the server address:
```
java -jar world-downloader.jar --no-gui -s address.to.server.com
```

### Running on Linux
To easily download the latest release using the terminal, the following commands can be used:
```
wget https://github.com/cafepromenade/minecraft-world-downloader/releases/latest/download/world-downloader.jar
java -jar world-downloader.jar -s address.to.server.com
```

When running headless Java, the GUI should be disabled by including the GUI option:
```
java -jar world-downloader.jar -s address.to.server.com --no-gui
```

Some linux distributions may require `-Djdk.gtk.version=2` for the GUI to work:
```
java -Djdk.gtk.version=2 -jar world-downloader.jar
```

### Docker + web console
The project ships a `Dockerfile` and `docker-compose.yml` that run the downloader headless behind a
small **web management console** which mirrors every command-line option. See the
[Docker & web console](https://github.com/cafepromenade/minecraft-world-downloader/wiki/Docker-Web-Console)
wiki page for the full guide.

```
docker compose up -d --build
```

Then open **http://localhost:8080** — the console has **no login by default** (set `WEB_PASSWORD`
to gate it behind a username/password if you expose it beyond localhost). From the console you can:
- **sign in to your Minecraft account** three ways — **Microsoft** (device-code login: open the link,
  enter the code), **access token** (paste an existing token), or **offline** username,
- set every option (server address, ports, render distance, world output, center offset, and all the
  map/behaviour toggles) — the same flags as the command line,
- **start / stop / restart** the downloader,
- watch live logs and status,
- **save** all settings (persisted to the volume),
- **export the world** as `.zip` or `.tar.gz`, or snapshot the directory into `./data/exports`.

Point your Minecraft client at `localhost:25565` (the proxy port) to download a world. Worlds, the
registry cache, your account session and saved settings persist in the `./data` folder (mounted at `/data`).

| Port | Purpose |
| ---- | ------- |
| 8080 | Web management console |
| 25565 | Minecraft proxy — connect your client here |

Environment variables: `WEB_PORT`; `WEB_USERNAME` + `WEB_PASSWORD` (optional — set both to require a
console login; off by default); `SECRET_KEY` (auto-generated if unset, only used when login is enabled);
and `MS_CLIENT_ID` (Azure/Microsoft OAuth client id for Microsoft login; defaults to the public
Minecraft launcher client id).

### Desktop manager (Windows)
A small **WinUI 3** desktop app (`desktop/`) sets up and runs the Dockerized console for you: pick the
folder where worlds and data are stored, choose the ports and (optional) login, then **Start** — it
launches the container and opens the console in your browser. Download the **NSIS installer** from the
[releases](https://github.com/cafepromenade/minecraft-world-downloader/releases) page (built by the
`Desktop manager release` GitHub Actions workflow on each `v*` tag). It uses the image published to
`ghcr.io/cafepromenade/minecraft-world-downloader`. Requires [Docker Desktop](https://www.docker.com/products/docker-desktop/).

### Building from source
<details>
  <summary>Dependencies on linux</summary>
  
  ### debian/ubuntu
  
  ```
  sudo apt-get install default-jdk maven
  ```

  ### arch/manjaro
  
  ```
  sudo pacman -S --needed jdk-openjdk maven
  ```
</details>

<details>
  <summary>Build project to executable jar file</summary>
  
 Building the project manually can be done using Maven:
  ```
  git clone https://github.com/cafepromenade/minecraft-world-downloader
  cd minecraft-world-downloader
  mvn package
  java -jar ./target/world-downloader.jar -s address.to.server.com
  ```

</details>

### Contact
<details>
  <summary>Contact information</summary>

  For problems, bugs, feature requests and questions about how to use the application, please [open an issue](https://github.com/cafepromenade/minecraft-world-downloader/issues/new/choose) or discussion on GitHub. 

  For other inquiries, email: cafepromenade.github@gmail.com
  
  If you want to support this project, you can [donate through GitHub](https://github.com/sponsors/cafepromenade?frequency=one-time&amount=5)
</details>

