# minecraft-world-downloader
A Minecraft world downloader that works by intercepting & decrypting network traffic between the client and the server to read & save chunk data. 

### Features
- Requires no client modifications and as such works with every game client, vanilla or not
- Automatically merge into previous downloads or existing worlds
- Save chests and other inventories by opening them
- Optional GUI to show overview of which chunks have been saved:

<img src="https://i.imgur.com/AwwPw42.png" width="50%">

### Requirements
- Java 9 or higher
- Minecraft version 1.12.2+ // 1.13.2+ // 1.14.1+ // 1.15.2+ // 1.16.0+

### Basic usage
[Download](https://github.com/mircokroon/minecraft-world-downloader/releases/latest/download/world-downloader.jar) the  latest release and execute the jar file using the commandline by running:

```
java -jar world-downloader.jar -s address.to.server.com
```

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
|  --enable-world-gen | true | When set to false, will prevent new terrain from being generated in-game. |

### Running on Linux
To easily download the latest release using the terminal, the following commands can be used:
```
wget https://github.com/mircokroon/minecraft-world-downloader/releases/latest/download/world-downloader.jar
java -jar world-downloader.jar -s address.to.server.com
```

When running headless Java, the GUI should be disabled by including the GUI option:
```
java -jar world-downloader.jar -s address.to.server.com --gui=false
```

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
  
 Building the project manually can be done with the Maven assembly plugin:
  ```
  git clone https://github.com/mircokroon/minecraft-world-downloader
  cd minecraft-world-downloader
  mvn assembly:assembly
  java -jar ./target/world-downloader.jar -s address.to.server.com
  ```

</details>



