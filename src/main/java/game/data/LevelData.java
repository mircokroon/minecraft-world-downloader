package game.data;

import config.Config;
import config.Option;
import config.Version;
import game.data.coordinates.Coordinate3D;
import game.data.coordinates.CoordinateDouble3D;
import game.data.dimension.Dimension;
import se.llbit.nbt.*;
import util.NbtUtil;
import util.PathUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

public class LevelData {
    private final WorldManager worldManager;
    private final Path outputDir;
    private final File file;
    private Tag root;
    private SpecificTag worldGenSettings;
    private CompoundTag data;
    private boolean savingBroken;

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
        loadGeneratorSettings();
    }
    private void load(boolean forceInternal) throws IOException {
        Files.createDirectories(outputDir);

        InputStream fileInput;
        if (!file.exists() || !file.isFile() || forceInternal) {
            fileInput = LevelData.class.getClassLoader().getResourceAsStream("level.dat");
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
     * Loads the default generator settings used in 1.16+. They are more complicated than before so loading them from a
     * file is easier.
     */
    private void loadGeneratorSettings() throws IOException {
        this.worldGenSettings = NbtUtil
                .read(LevelData.class.getClassLoader().getResourceAsStream(getGeneratorSettingsName()))
                .unpack()
                .get("WorldGenSettings")
                .asCompound();
    }

    private static String getGeneratorSettingsName() {
        if (Config.versionReporter().isAtLeast(Version.V1_19)) {
            return "world-gen-settings-1.19.dat";
        } else {
            return "world-gen-settings-1.16.dat";
        }
    }

    /**
     * Save the level.dat file so the world can be easily opened. If one doesn't exist, use the default one from
     * the resource folder.
     */
    public void save() throws IOException {
        if (savingBroken) { return; }
        if (data == null) {
            savingBroken = true;
            System.err.println("Unable to read in valid 'level.dat' file. Chunks will be saved, but they cannot be opened in-game without manually providing a 'level.dat' file.");
            return;
        }

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
            player.add("Dimension", new StringTag(WorldManager.getInstance().getDimension().toString()));

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
        if (Config.versionReporter().isAtLeast(Version.V1_16)) {
            LongTag seed = new LongTag(Config.getLevelSeed());

            CompoundTag dimensions = this.worldGenSettings.get("dimensions").asCompound();
            for (Dimension dim : Dimension.DEFAULTS)  {
                Tag generator = dimensions.get(dim.getName()).get("generator");
                generator.asCompound().add("seed", seed);
                generator.get("biome_source").asCompound().add("seed", seed);
            }
            this.worldGenSettings.asCompound().add("seed", seed);

            data.add("WorldGenSettings", this.worldGenSettings);
        } else {
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
        if (Config.versionReporter().isAtLeast(Version.V1_16)) {
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
        } else {
            data.add("generatorVersion", new IntTag(1));
            data.add("generatorName", new StringTag("flat"));
            // this is the 1.12.2 superflat format, but it still works in later versions.
            data.add("generatorOptions", new StringTag("3;minecraft:air;127"));
        }
    }

}
