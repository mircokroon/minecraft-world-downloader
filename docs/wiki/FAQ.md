# FAQ

## Connecting & basics

**How do I start downloading?**
Start the downloader pointed at the server, then in Minecraft connect to **`localhost`** (or
`localhost:25565`) instead of the real server address. Walk/fly around to load chunks.

**Where is my world saved?**
In the output directory — `world` by default (`--output` to change). Under Docker it's in
`./data/<output-dir>`. Open it in singleplayer, or export it from the web console.

**Nothing gets saved / the map stays empty.**
Make sure you connected Minecraft to `localhost`, not the real server. Only chunks you actually load
(by being near them) are saved — explore the area you want.

## Authentication

**"Connection refused" / login fails on an online-mode server.**
You must sign in to your Minecraft account — see [Authentication](Authentication). With the jar, use the
Authentication tab or `--username`/`--token`; with Docker, use the **Minecraft account** panel.

**Microsoft login says my account has no Xbox profile / wrong region / is a child account.**
Finish Xbox setup for the account, or the account is restricted. These come straight from Microsoft's
servers — see [Authentication](Authentication).

**Does it steal my password/token?**
No. The downloader uses your token only to join the server on your behalf — the same request the vanilla
client makes. The token stays on your machine (in the mounted `./data` volume under Docker).

## Errors

**`Connection Lost — Internal Exception: … ReportedNbtException: Loading NBT data`**
This was a bug on 1.20.2+ when extended render distance re-sent chunks to the client. It is **fixed** in
this fork (the heightmap NBT is now written in the nameless format the client expects). Update to the
latest build. See [Supported Versions](Supported-Versions).

**It downloads a server jar / "Generating reports…" on first connect.**
For 1.13+ the downloader generates block/registry data from the matching server jar once, then caches
it. This needs internet and a little time on the first run of each version. 1.8–1.12 use bundled data.

## Docker

**Port 25565 is already allocated.**
Another server/container is using it. Remap the host port in `docker-compose.yml`
(e.g. `"25566:25565"`) and connect Minecraft to that port. See [Docker & Web Console](Docker-Web-Console).

**The web console has no login — is that safe?**
By default it's meant for `localhost`. If you expose it to a network, set `WEB_USERNAME` + `WEB_PASSWORD`
to require a login. The Minecraft **account** login is always required for online servers regardless.

**How do I get my world out of the container?**
Use **Download .zip** / **.tar.gz** in the console, or **Export directory** (snapshots into
`./data/exports`). The live world is also directly at `./data/<output-dir>` on the host.

## Versions

**Which versions are supported?**
1.8 through 26.1 — see [Supported Versions](Supported-Versions). In-between versions map to the nearest
supported protocol.

## Still stuck?
Open an [issue or discussion](https://github.com/cafepromenade/minecraft-world-downloader/issues) on
GitHub with your version, OS, and the relevant log output.
