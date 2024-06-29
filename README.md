# minecraft-world-downloader
A Minecraft world downloader that works as a proxy server between the client and the server to read & save chunk data. Download multiplayer worlds by connecting to them and walking around. Chunks can be sent back to the client to extend the render distance.


### Downloads  <a href="https://github.com/mircokroon/minecraft-world-downloader/releases/latest"><img align="right" src="https://img.shields.io/github/downloads/mircokroon/minecraft-world-downloader/total.svg"></a>
Windows launcher: [world-downloader-launcher.exe](https://github.com/mircokroon/minecraft-world-downloader-launcher/releases/latest/download/world-downloader-launcher.exe)

Latest cross-platform jar (command-line support): [world-downloader.jar](https://github.com/mircokroon/minecraft-world-downloader/releases/latest/download/world-downloader.jar)

### Basic usage
[Download](https://github.com/mircokroon/minecraft-world-downloader-launcher/releases/latest/download/world-downloader-launcher.exe) the latest release and run it. Enter the server address in the address field and press start.

<img src="https://i.imgur.com/yH8SH5C.png">

Instead of connecting to the server itself, connect to `localhost` in Minecraft to start downloading the world.
<img src="https://i.imgur.com/wKMnXfq.png">

If you run into any problems, check the [FAQ](https://github.com/mircokroon/minecraft-world-downloader/wiki/FAQ) page for some common issues. 

### [Features](https://github.com/mircokroon/minecraft-world-downloader/wiki/Features)
- Requires no client modifications and as such works with every game client, vanilla or not
- Automatically merge into previous downloads or existing worlds
- Save chests and other inventories by opening them
- Extend the client's render distance by sending chunks downloaded previously back to the client
- Overview map of chunks that have been saved:

<img src="https://i.imgur.com/7FIJ6fZ.png" width="80%" title="Example of the GUI showing all the downloaded chunks as white squares, which ones from a previous download greyed out.">

### Requirements
- Java 21 or higher
- Minecraft version 1.12.2+ // 1.13.2+ // 1.14.1+ // 1.15.2+ // 1.16.2+ // 1.17+ // 1.18+ // 1.19.3+ // 1.20+ // 1.21+

### Command-line
[Download](https://github.com/mircokroon/minecraft-world-downloader/releases/latest/download/world-downloader.jar) the cross-platform `world-downloader.jar` and run it using the command-line:

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

### Contact
<details>
  <summary>Contact information</summary>

  For problems, bugs, feature requests and questions about how to use the application, please [open an issue](https://github.com/mircokroon/minecraft-world-downloader/issues/new/choose) or discussion on GitHub. 

  For other inquiries, email: mircokroon.github@gmail.com
  
  If you want to support this project, you can [donate through GitHub](https://github.com/sponsors/mircokroon?frequency=one-time&amount=5)
</details>

