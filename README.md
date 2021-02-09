# minecraft-world-downloader
A Minecraft world downloader that works by intercepting & decrypting network traffic between the client and the server to read & save chunk data. Chunks can be sent back to the client to extend the render distance.

### Features
- Requires no client modifications and as such works with every game client, vanilla or not
- Automatically merge into previous downloads or existing worlds
- Save chests and other inventories by opening them
- Extend the client's render distance by sending chunks downloaded previously back to the client
- Overview map of chunks that have been saved:

<img src="https://i.imgur.com/nSM6mLw.png" width="50%" title="Example of the GUI showing previously downloaded chunks as white squares, chunks sent from the downloader to the client to extend the render distance in normal colours, and chunks sent by server directly to the client in red.">

### Requirements
- Java 8 or higher
- Minecraft version 1.12.2+ // 1.13.2+ // 1.14.1+ // 1.15.2+ // 1.16.2+

### Basic usage
[Download](https://github.com/mircokroon/minecraft-world-downloader/releases/latest/download/world-downloader.jar) the  latest release and execute the jar file using the commandline by running:

```
java -jar world-downloader.jar -s address.to.server.com
```

Then connect to ```localhost``` in Minecraft to start downloading the world. The world will be saved to the ```world/``` by default.

### Extending render distance
The downloader can be used to extend the render distance by sending chunks that were downloaded previously back to the client. For this, simply include the `-r [distance]` argument when running the program. For example:
```
java -jar world-downloader.jar -s address.to.server.com -r 16
```


### Options
|  **Parameter** | **Default** | **Description** |
| --- | --- | --- |
|  --server | *required* | Server address |
|  --port | 25565 | Server port |
|  --local-port | 25565 | Port on which the downloader will run. |
|  --output | world | World output director |
|  --gui | true | Enable or disable the GUI, which shows an overview of all chunks known to the downloader |
| --render-distance | 0 | When larger than the server's render distance, send known chunks back to the client |
|  --minecraft-dir | %appdata%/.minecraft | Path to your Minecraft installation, used for Mojang authentication |
|  --mark-unsaved-chunks | true | When enabled, marks unsaved chunks red in the GUI. |

Additional options are available by running `java -jar world-downloader.jar --help`.

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



