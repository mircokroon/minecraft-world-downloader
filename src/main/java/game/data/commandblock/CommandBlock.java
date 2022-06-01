package game.data.commandblock;

import game.data.WorldManager;
import game.data.coordinates.Coordinate3D;
import game.data.coordinates.CoordinateDim3D;
import se.llbit.nbt.ByteTag;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.IntTag;
import se.llbit.nbt.StringTag;

public class CommandBlock {

    private CoordinateDim3D location;
    private String command;
    private CommandMode mode; // Not stored in NBT, but useful to know

    /**
     * Whether this command block tracks output or not. If true, opening this
     * command block displays the "O" character (with no previous output text box
     * visible in the menu). If false, this command block displays the "X" character
     * (with a previous output text box visible in the menu)
     */
    private boolean trackOutput;

    /**
     * Whether this command block is Conditional (true) or Unconditional (false)
     */
    private boolean isConditional; // Not stored in NBT

    /**
     * Whether this command block is Always Active (true) or Needs Redstone (false)
     */
    private boolean isAlwaysActive;

    public CommandBlock(Coordinate3D coords, String command, int mode, byte flags) {
        this.location = coords.addDimension3D(WorldManager.getInstance().getDimension());
        this.command = command;
        this.mode = CommandMode.fromMode(mode);
        this.trackOutput = (flags & 0x01) > 0;
        this.isConditional = (flags & 0x02) > 0;
        this.isAlwaysActive = (flags & 0x04) > 0;
    }

    public void addNbt(CompoundTag root) {
        root.add("auto", new ByteTag(isAlwaysActive ? 1 : 0));
        root.add("Command", new StringTag(command));
        root.add("conditionMet", new ByteTag(1)); // Not entirely sure why, but this is always 1
        root.add("powered", new ByteTag(0)); // Unless it's actually powered, assume false
        root.add("TrackOutput", new ByteTag(trackOutput ? 1 : 0));
        root.add("SuccessCount", new IntTag(0));
    }
    
    public CoordinateDim3D getLocation() {
        return this.location;
    }

    public String getBlockStateName() {
        return this.mode.getResourceLocation();
    }

    @Override
    public String toString() {
        return "CommandBlock{" + "location=" + location + ", command='" + command + '\'' + ", mode=" + mode
                + ", alwaysActive=" + isAlwaysActive + ", isConditional=" + isConditional + ", trackOutput="
                + trackOutput + '}';
    }

    private enum CommandMode {
        /**
         * Chain command blocks (also known as "sequence" in wiki.vg)
         */
        CHAIN("minecraft:chain_command_block"),

        /**
         * Repeating command blocks (also known as "auto" in wiki.vg)
         */
        REPEAT("minecraft:repeating_command_block"),

        /**
         * Impulse (normal) command blocks (also known as "redstone" in wiki.vg)
         */
        IMPULSE("minecraft:command_block");

        private final String resourceLocation;

        CommandMode(String resourceLocation) {
            this.resourceLocation = resourceLocation;
        }

        static CommandMode fromMode(int mode) {
            return switch (mode) {
                case 0 -> CHAIN;
                case 1 -> REPEAT;
                case 2 -> IMPULSE;
                default -> throw new IllegalArgumentException("Unexpected value: " + mode);
            };
        }

        public String getResourceLocation() {
            return this.resourceLocation;
        }

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }
}
