# Command-Line Options

Run `java -jar world-downloader.jar --help` to list these. In the
[Docker web console](Docker-Web-Console), the same options appear as form fields.

```bash
java -jar world-downloader.jar --no-gui -s mc.example.com -o my_world -r 10
```

## Connection
| Flag | Alias | Default | Description |
| ---- | ----- | ------- | ----------- |
| `--server` | `-s` | — | Remote server address (hostname or IP, **no port**). Required for `--no-gui`. |
| `--local-port` | `-l` | `25565` | Port the downloader's proxy listens on. Connect Minecraft here. |
| `--username` | `-u` | — | Your Minecraft username. |
| `--token` | `-t` | — | Minecraft access token (see [Authentication](Authentication)). |
| `--disable-srv-lookup` | | off | Disable resolving the true address via DNS SRV records. |

## World output
| Flag | Alias | Default | Description |
| ---- | ----- | ------- | ----------- |
| `--output` | `-o` | `world` | Output directory. An existing world is updated/merged. |
| `--seed` | | `0` | Numeric level seed for the output world. |
| `--center-x` | | `0` | Offsets the world: this X is placed at origin (0,0). Rounded to 512. Requires `--center-z`. |
| `--center-z` | | `0` | Offsets the world: this Z is placed at origin (0,0). Rounded to 512. Requires `--center-x`. |
| `--disable-world-gen` | | off | Set world type to a superflat void to stop new chunks generating. |
| `--disable-chunk-saving` | | off | Don't write chunks to disk (debugging). |
| `--ignore-block-changes` | | off | Ignore changes to chunks after they are loaded. |

## Render distance & map
| Flag | Alias | Default | Description |
| ---- | ----- | ------- | ----------- |
| `--extended-render-distance` | `-r` | `0` | Re-send downloaded chunks to the client to extend render distance. |
| `--render-players` | | off | Show other players on the overview map. |
| `--mark-new-chunks` | | off | Outline newly downloaded chunks in orange. |
| `--mark-old-chunks` | | on | Grey out old chunks on the map. |
| `--disable-mark-unsaved` | | off | Stop marking unsaved chunks in red. |
| `--draw-extended-chunks` | | off | Draw re-sent (extended) chunks on the map. |
| `--enable-cave-mode` | | off | Auto-switch to cave render mode when underground. |

## Interface & misc
| Flag | Alias | Description |
| ---- | ----- | ----------- |
| `--no-gui` | | Run headless (no GUI). Requires `--server`. |
| `--force-console` | | Never redirect console output to the GUI. |
| `--disable-messages` | | Disable various info messages (e.g. chest saving). |
| `--dev-mode` | | Enable developer mode. |
| `--clear-settings` | | Delete the saved `config.json` and exit. |
| `--help` | `-h` | Show the help message. |

## Examples
Headless download to a custom folder with extended render distance:
```bash
java -jar world-downloader.jar --no-gui -s mc.example.com -o survival -r 12
```
Offset the saved world so spawn-area coordinates `(1500, -200)` become `(0, 0)`:
```bash
java -jar world-downloader.jar --no-gui -s mc.example.com --center-x 1500 --center-z -200
```
Offline-mode server:
```bash
java -jar world-downloader.jar --no-gui -s 192.168.1.50 -u Steve
```
