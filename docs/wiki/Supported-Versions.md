# Supported Versions

The downloader supports **every Minecraft: Java Edition version from 1.8 through 26.1**. Connecting with
an in-between version automatically maps to the nearest supported protocol.

| Era | Versions | Notes |
| --- | -------- | ----- |
| Classic | **1.8, 1.9, 1.10, 1.11** | Pre-palette "direct" chunk format (1.8) and the 1.9–1.11 paletted format. Registries reuse the bundled 1.12.2 data. |
| Flattening era | **1.12.2, 1.13.2, 1.14.x, 1.15.x, 1.16.x** | |
| Modern | **1.17, 1.18, 1.19.x, 1.20, 1.20.2, 1.20.4, 1.20.6** | 1.20.2 introduced nameless network NBT (see below). |
| 1.21 line | **1.21, 1.21.2–1.21.4, 1.21.5–1.21.11** | 1.21.5 changed chunk heightmaps from an NBT compound to a packed array. |
| Year-based | **26.1** ("Tiny Takeover") | First year-based release (protocol 775). |

## What this fork added
- **1.8 – 1.11 support** — including the 1.8 direct block-array chunk format and the Map Chunk Bulk
  packet. 1.9–1.11 share the 1.12 chunk format.
- **1.21.2 – 1.21.11 point releases** — correct per-release packet IDs and the 1.21.5 heightmap-format
  change, so the whole 1.21.x line downloads correctly.
- **Minecraft 26.1** — protocol 775 / data version 4786, with the new array-format heightmaps.
- **Fix for the 1.20.2+ "Connection Lost — Loading NBT data" error** — see below.

## The "Loading NBT data" fix
On 1.20.2 and newer, network NBT lost its root tag name. When the render-distance extender sent
downloaded chunks back to the client, the heightmap NBT was written in the old *named* format, so the
client rejected the chunk with:

```
Internal Exception: io.netty.handler.codec.DecoderException:
net.minecraft.nbt.ReportedNbtException: Loading NBT data
```

The downloader now writes the nameless form for 1.20.2+, matching what the client expects. If you saw
this error with extended render distance enabled on 1.20.2 – 1.20.6, it is fixed.

## Notes
- For 1.13+ the downloader downloads the matching server jar once to generate block/registry data; this
  is cached (in `./data/cache` under Docker). 1.8–1.12 use bundled data and need no download.
- Requires **Java 21+** to run (the Docker image bundles a JRE).
- Brand-new versions (e.g. 26.1) and very old ones (1.8–1.11) are best-effort: packet data is sourced
  from public protocol references. If you hit an issue on a specific version, please report it.
