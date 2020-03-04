# minecraft-world-downloader
A Minecraft world downloader that works by intercepting & decrypting network traffic between the client and the server to read & save chunk data. 

### Features
- Requires no client modifications and as such works with every game client, vanilla or not
- Automatically merge into previous downloads or existing worlds
- Offset world coordinates of the saved location
- Optional GUI to show overview of which chunks have been saved:

<img src="https://i.imgur.com/AwwPw42.png" width="50%">

### Requirements
- Java 8 or higher
- Minecraft version 1.12.2+ // 1.13.2+ // 1.14.1+ // 1.15.2+

### Basic usage
[Download](https://github.com/mircokroon/minecraft-world-downloader/releases) the latest release and execute the jar file using the commandline by running:

```java -jar world-downloader.jar -s address.to.server.com```

Then connect to ```localhost``` in Minecraft to start downloading the world. The world will be saved to the ```world/``` by default.


### Options
|  **Parameter** | **Default** | **Description** |
| --- | --- | --- |
|  --server | *required* | Server address |
|  --port | 25565 | Server port |
|  --local-port | 25565 | Local server port |
|  --output | world | Output directory (world root) |
|  --mask-bedrock | false | If true, replace bedrock with stone. Does not currently work on 1.13+. |
|  --center-x | 0 | Offset the world so that the given coordinate is at 0 |
|  --center-y | 0 | Offset the world so that the given coordinate is at 0 |
|  --gui | true | If false, hides the saved chunks GUI |
|  --minecraft | %appdata%/.minecraft | Path to your Minecraft installation, used for Mojang authentication |
| --render-distance | 75 | Render distance of (in chunks) of the overview map |
|  --seed | 0 | World seed, useful when generating chunks after downloading |
