package game.data;

import config.Config;
import game.data.coordinates.Coordinate3D;
import game.data.coordinates.CoordinateDim2D;
import game.data.coordinates.CoordinateDim3D;
import game.data.coordinates.CoordinateDouble3D;
import game.data.dimension.Dimension;
import org.apache.commons.io.IOUtils;
import proxy.CompressionManager;
import se.llbit.nbt.*;
import util.PathUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class LevelData {
    WorldManager worldManager;
    Path outputDir;
    File file;
    Tag root;
    CompoundTag data;

    public LevelData(WorldManager worldManager) {
        this.worldManager = worldManager;
        this.outputDir = PathUtils.toPath(Config.getWorldOutputDir());
        this.file = Paths.get(outputDir.toString(), "level.dat").toFile();
    }

    public CoordinateDouble3D getPlayerPosition() {
        try {
            CompoundTag player = data.get("Player").asCompound();

            ListTag pos = player.get("Pos").asList();
            double x =  pos.get(0).doubleValue();
            double y =  pos.get(1).doubleValue();
            double z =  pos.get(2).doubleValue();

            return new CoordinateDouble3D(x, y, z);
        } catch (Exception ex) {
            // probably the tag is missing, doesn't matter, we'll just return the origin
            return new CoordinateDouble3D(0, 0, 0);
        }
    }

    public Dimension getPlayerDimension() {
        try {
            return Dimension.standardDimensionFromString(data.get("Player").get("Dimension").stringValue());
        } catch (Exception ex) {
            return Dimension.OVERWORLD;
        }
    }

    public void load() throws IOException {
        Files.createDirectories(outputDir);

        InputStream fileInput;
        if (!file.exists() || !file.isFile()) {
            fileInput = WorldManager.class.getClassLoader().getResourceAsStream("level.dat");
        } else {
            fileInput = new FileInputStream(file);
        }

        byte[] fileContent = IOUtils.toByteArray(fileInput);

        // get default level.dat
        this.root = NamedTag.read(
                new DataInputStream(new ByteArrayInputStream(CompressionManager.gzipDecompress(fileContent)))
        );

        this.data = (CompoundTag) root.unpack().get("Data");
    }


    /**
     * Save the level.dat file so the world can be easily opened. If one doesn't exist, use the default one from
     * the resource folder.
     */
    public void save() throws IOException {
        // add the player's position
        if (worldManager.getPlayerPosition() != null) {
            Tag playerTag = data.get("Player");
            CompoundTag player;
            if (playerTag instanceof ErrorTag) {
                player = new CompoundTag();
            } else {
                player = (CompoundTag) playerTag;
                data.add("Player", player);
            }

            Coordinate3D playerPosition = worldManager.getPlayerPosition().offsetGlobal();
            player.add("Pos", new ListTag(Tag.TAG_DOUBLE, Arrays.asList(
                    new DoubleTag(playerPosition.getX() * 1.0),
                    new DoubleTag(playerPosition.getY() * 1.0),
                    new DoubleTag(playerPosition.getZ() * 1.0)
            )));

            // set the world spawn to match the last known player location
            data.add("SpawnX", new IntTag(playerPosition.getX()));
            data.add("SpawnY", new IntTag(playerPosition.getY()));
            data.add("SpawnZ", new IntTag(playerPosition.getZ()));
        }

        // add the seed & last played time
        data.add("RandomSeed", new LongTag(Config.getLevelSeed()));
        data.add("LastPlayed", new LongTag(System.currentTimeMillis()));

        // add the version
        if (Config.getDataVersion() > 0 && Config.getGameVersion() != null) {
            CompoundTag versionTag = new CompoundTag();
            versionTag.add("Id", new IntTag(Config.getDataVersion()));
            versionTag.add("Name", new StringTag(Config.getGameVersion()));
            versionTag.add("Snapshot", new ByteTag((byte) 0));

            data.add("Version", versionTag);
        }

        if (!Config.isWorldGenEnabled()) {
            disableWorldGeneration(data);
        }

        // write the file
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        root.write(new DataOutputStream(output));

        byte[] compressed = CompressionManager.gzipCompress(output.toByteArray());
        Files.write(file.toPath(), compressed);
    }


    /**
     * Set world type to a superflat void world.
     */
    private void disableWorldGeneration(CompoundTag data) {
        data.add("generatorName", new StringTag("flat"));

        // this is the 1.12.2 superflat format, but it still works in later versions.
        data.add("generatorOptions", new StringTag("3;minecraft:air;127"));
    }

}
