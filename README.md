# minecraft-world-downloader
A Minecraft world downloader that works by intercepting network traffic between the client and the server to read & save chunk data. 

### Features
- Requires no client modifications and as such works with every non-vanilla game client
- Offset world save coordinates to obscure the original location
- Automatically merge into existing downloads
- Optional GUI to show which chunks have been saved

### Requirements
- Java 8 or higher 
- Minecraft 1.12.2 (currently untested on other versions)

### Basic usage
[Download](https://github.com/mircokroon/minecraft-world-downloader/releases) the latest release and execute the jar file using:
```java -jar world-downloader.jar -s address.to.server.com```

Then connect to ```localhost``` in Minecraft to start downloading the world. The world will be saved to the ```world/``` by default.


### Options
|  **Parameter** | **Default** | **Description** |
| --- | --- | --- |
|  --server | *required* | Server address |
|  --port | 25565 | Server port |
|  --local-port | 25565 | Local server port |
|  --output | world | Output directory (world root) |
|  --mask-bedrock | false | If true, replace bedrock with stone |
|  --center-x | 0 | Offset the world so that the given coordinate is at 0 |
|  --center-y | 0 | Offset the world so that the given coordinate is at 0 |
|  --gui | true | If false, hides the saved chunks GUI |
