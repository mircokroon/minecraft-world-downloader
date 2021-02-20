package game.data;

import config.Config;
import game.data.WorldManager;
import game.data.coordinates.Coordinate3D;
import game.data.coordinates.CoordinateDim2D;
import game.data.coordinates.CoordinateDim3D;
import game.data.coordinates.CoordinateDouble3D;
import game.data.dimension.Dimension;
import game.data.dimension.DimensionCodec;
import org.apache.commons.io.IOUtils;
import proxy.CompressionManager;
import se.llbit.nbt.*;
import util.NbtUtil;
import util.PathUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
        load(false);
    }
    private void load(boolean forceInternal) throws IOException {
        Files.createDirectories(outputDir);

        InputStream fileInput;
        if (!file.exists() || !file.isFile() || forceInternal) {
            fileInput = WorldManager.class.getClassLoader().getResourceAsStream("level.dat");
        } else {
            fileInput = new FileInputStream(file);
        }

        try {
            // get default level.dat
            this.root = NbtUtil.read(fileInput);
        } catch (Exception ex) {
            ex.printStackTrace();
            if (!forceInternal) {
                load(true);
            }
        }

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
            data.add("DataVersion", new IntTag(Config.getDataVersion()));
        }

        if (!Config.isWorldGenEnabled()) {
            disableWorldGeneration(data);
        } else {
            enableWorldGeneration(data);
        }

        // write the file
        NbtUtil.write(root, file.toPath());
    }

    private void enableWorldGeneration(CompoundTag data) {
        if (Config.getDataVersion() < 2504) {
            data.add("generatorVersion", new IntTag(1));
            data.add("generatorName", new StringTag("default"));
            // this is the 1.12.2 superflat format, but it still works in later versions.
            data.add("generatorOptions", new StringTag(""));
        }
    }


    /**
     * Set world type to a superflat void world.
     */
    private void disableWorldGeneration(CompoundTag data) {
        if (Config.getDataVersion() < 2504) {
            data.add("generatorVersion", new IntTag(1));
            data.add("generatorName", new StringTag("flat"));
            // this is the 1.12.2 superflat format, but it still works in later versions.
            data.add("generatorOptions", new StringTag("3;minecraft:air;127"));
        } else {
            CompoundTag generator = new CompoundTag();
            generator.add("type", new StringTag("minecraft:flat"));
            generator.add("settings", new CompoundTag(Arrays.asList(
                    new NamedTag("layers", new ListTag(Tag.TAG_COMPOUND, Collections.singletonList(
                            new CompoundTag(Arrays.asList(
                                    new NamedTag("block", new StringTag("minecraft:air")),
                                    new NamedTag("height", new IntTag(1))
                            ))
                    ))),
                    new NamedTag("structures", new CompoundTag(Arrays.asList(new NamedTag("structures", new CompoundTag())))),
                    new NamedTag("biome", new StringTag("minecraft:the_void"))
            )));

            CompoundTag dimensions = new CompoundTag(worldManager.getDimensionCodec().getDimensions().stream().map(dimension -> {
                CompoundTag dim = new CompoundTag();
                dim.add("type", new StringTag(dimension.getType()));
                dim.add("generator", generator);

                return new NamedTag(dimension.getName(), dim);
            }).collect(Collectors.toList()));


            data.add("WorldGenSettings", new CompoundTag(Arrays.asList(
                    new NamedTag("bonus_chest", new ByteTag(0)),
                    new NamedTag("generate_features", new ByteTag(0)),
                    new NamedTag("seed", new LongTag(Config.getLevelSeed())),
                    new NamedTag("dimensions", dimensions)
            )));
        }
    }

}
