# minecraft-world-downloader
A Minecraft world downloader that works by intercepting & decrypting network traffic between the client and the server to read & save chunk data. Chunks can be sent back to the client to extend the render distance.


### Downloads  <a href="https://github.com/mircokroon/minecraft-world-downloader/releases/latest"><img align="right" src="https://img.shields.io/github/downloads/mircokroon/minecraft-world-downloader/total.svg"></a>
Latest Windows release (GUI): [world-downloader.exe](https://github.com/mircokroon/minecraft-world-downloader/releases/latest/download/world-downloader.exe)

Cross-platform jar (GUI & commandline): [world-downloader.jar](https://github.com/mircokroon/minecraft-world-downloader/releases/latest/download/world-downloader.jar)

### [Features](https://github.com/mircokroon/minecraft-world-downloader/wiki/Features)
- Requires no client modifications and as such works with every game client, vanilla or not
- Automatically merge into previous downloads or existing worlds
- Save chests and other inventories by opening them
- Extend the client's render distance by sending chunks downloaded previously back to the client
- Overview map of chunks that have been saved:

<img src="https://i.imgur.com/nSM6mLw.png" width="50%" title="Example of the GUI showing previously downloaded chunks as white squares, chunks sent from the downloader to the client to extend the render distance in normal colours, and chunks sent by server directly to the client in red.">

### Requirements
- Java 17 or higher
- Minecraft version 1.12.2+ // 1.13.2+ // 1.14.1+ // 1.15.2+ // 1.16.2+ // 1.17+ // 1.18+

### Basic usage
[Download](https://github.com/mircokroon/minecraft-world-downloader/releases/latest/download/world-downloader.exe) the latest release and run it. Enter the server address in the address field and press start. Instead of connecting to the server itself, connect to `localhost` in Minecraft to start downloading the world.

<img src="https://i.imgur.com/lpjEdPB.png">

Additional settings can be changed in the other tabs of the settings window.

If you run into any problems, check the [FAQ](https://github.com/mircokroon/minecraft-world-downloader/wiki/FAQ) page for some common issues. 


### Commandline
[Download](https://github.com/mircokroon/minecraft-world-downloader/releases/latest/download/world-downloader.jar) the cross-platform `world-downloader.jar` and run it using the commandline:

```
java -jar world-downloader.jar -s address.to.server.com
```

Then connect to ```localhost``` in Minecraft to start downloading the world. The world will be saved to the ```world/``` by default.

Other arguments can be specified to change the behaviour of the downloader. For example, render distance extending can be enabled by setting the render distance with `-r [distance]`:
```
java -jar world-downloader.jar -s address.to.server.com -r 16
```

#### Options
|  **Parameter** | **Default** | **Description** |
| --- | --- | --- |
|  --server | *required* | Server address |
|  --port | 25565 | Server port |
|  --local-port | 25565 | Port on which the downloader will run. |
|  --output | world | World output director |
|  --no-gui | | Disable the GUI, useful for running in environments that don't support GUIs. |
| --render-distance | 0 | When larger than the server's render distance, send known chunks back to the client |
|  --mark-unsaved-chunks | true | When enabled, marks unsaved chunks red in the GUI. |
|  --minecraft-dir | %appdata%/.minecraft | Path to your Minecraft installation, used for Mojang authentication |
| --username | *none* | Set your Minecraft username, used instead of the Minecraft path for authentication |
| --token | *none* | Set the Minecraft [access token](https://github.com/mircokroon/minecraft-world-downloader/wiki/Authentication), used instead of the Minecraft path for authentication |

Additional options are available by running `java -jar world-downloader.jar --help`.

### Running on Linux
To easily download the latest release using the terminal, the following commands can be used:
```
wget https://github.com/mircokroon/minecraft-world-downloader/releases/latest/download/world-downloader.jar
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
  git clone https://github.com/mircokroon/minecraft-world-downloader
  cd minecraft-world-downloader
  mvn package
  java -jar ./target/world-downloader.jar -s address.to.server.com
  ```

</details>



