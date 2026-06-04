# Building From Source

## Requirements
- **JDK 21+**
- **Maven**

### Install dependencies (Linux)
**Debian / Ubuntu**
```bash
sudo apt-get install default-jdk maven
```
**Arch / Manjaro**
```bash
sudo pacman -S --needed jdk-openjdk maven
```

## Build the jar
```bash
git clone https://github.com/cafepromenade/minecraft-world-downloader
cd minecraft-world-downloader
mvn package
java -jar ./target/world-downloader.jar -s address.to.server.com
```
The shaded, runnable jar is written to `target/world-downloader.jar`.

## Run the tests
```bash
mvn test
```

## Build the Docker image
The repository includes a multi-stage `Dockerfile` (Maven build → JRE + Python web console) and a
`docker-compose.yml`:
```bash
docker compose build      # or: docker build -t minecraft-world-downloader .
docker compose up -d
```
See [Docker & Web Console](Docker-Web-Console) for usage.

## Project layout
| Path | Purpose |
| ---- | ------- |
| `src/main/java` | The downloader (proxy, chunk parsing, GUI, auth). |
| `src/main/resources/protocol-versions.json` | Packet-ID maps per protocol version. |
| `src/main/java/config/Version.java` | Supported-version anchors (protocol + data version). |
| `src/main/java/game/data/chunk/version/` | Per-version chunk format classes. |
| `web/` | The Docker web console (Flask app, templates, static assets). |
| `Dockerfile`, `docker-compose.yml` | Container build & run. |

## Adding a new Minecraft version
1. Add a `Version` enum anchor (protocol + data version).
2. Add a `protocol-versions.json` block with that protocol's packet IDs.
3. If the chunk format changed, add a `Chunk_<ver>` class and wire it into `ChunkFactory`; otherwise it
   reuses the nearest existing one.
