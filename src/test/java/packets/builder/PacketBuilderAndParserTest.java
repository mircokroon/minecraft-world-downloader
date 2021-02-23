package packets.builder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import packets.DataTypeProvider;
import packets.UUID;
import packets.lib.ByteQueue;
import se.llbit.nbt.*;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class PacketBuilderAndParserTest {

    protected PacketBuilder builder;
    protected DataTypeProvider parser;

    @BeforeEach
    public void beforeEach() {
        builder = new PacketBuilder(0x00);
    }

    @AfterEach
    public void afterEach() {
        assertThat(parser.hasNext()).isFalse();
    }

    /**
     * Get a DataTypeProvider built from the exiting packet builder.
     */
    protected DataTypeProvider getParser() {
        ByteQueue built = builder.build();
        byte[] arr = new byte[built.size()];
        built.copyTo(arr);

        parser = new DataTypeProvider(arr);
        int length = parser.readVarInt();
        assertThat(length).isGreaterThan(0);

        int packetId = parser.readVarInt();
        return parser;
    }

    @Test
    void varInt() {
        int before = 10;
        builder.writeVarInt(before);

        int after = getParser().readVarInt();

        assertThat(after).isEqualTo(before);
    }

    @Test
    void varIntBig() {
        int before = 2 << 26 - 1;
        builder.writeVarInt(before);

        int after = getParser().readVarInt();

        assertThat(after).isEqualTo(before);
    }

    @Test
    void varIntNegative() {
        int before = - 1337;
        builder.writeVarInt(before);

        int after = getParser().readVarInt();

        assertThat(after).isEqualTo(before);
    }

    @Test
    void intTest() {
        int before = 10;
        builder.writeInt(before);

        int after = getParser().readInt();

        assertThat(after).isEqualTo(before);
    }

    @Test
    void stringTest() {
        String before = "This is a string with very many words in it.";
        builder.writeString(before);

        String after = getParser().readString();

        assertThat(after).isEqualTo(before);
    }

    @Test
    void shortTest() {
        int before = 42;
        builder.writeShort(before);

        int after = getParser().readShort();

        assertThat(after).isEqualTo(before);
    }

    @Test
    void byteArray() {
        byte[] before = {1, 2, 3, 2, 1};
        builder.writeByteArray(before);

        byte[] after = getParser().readByteArray(before.length);

        assertThat(after).isEqualTo(before);
    }

    @Test
    void boolTrue() {
        builder.writeBoolean(true);

        assertThat(getParser().readBoolean()).isTrue();
    }

    @Test
    void boolFalse() {
        builder.writeBoolean(false);

        assertThat(getParser().readBoolean()).isFalse();
    }

    @Test
    void nbtCompoundTest() {
        CompoundTag before = new CompoundTag();
        before.add("someKey", new IntTag(30));

        builder.writeNbt(before);

        assertThat(getParser().readNbtTag()).isEqualTo(before);
    }

    @Test
    void nbtListTest() {
        ListTag before = new ListTag(Tag.TAG_STRING, Arrays.asList(
                new StringTag("a"),
                new StringTag("b"),
                new StringTag("c")
        ));

        builder.writeNbt(before);

        assertThat(getParser().readNbtTag()).isEqualTo(before);
    }

    @Test
    void longTest() {
        long before = 2L^48 - 2L^12;
        builder.writeLong(before);

        long after = getParser().readLong();

        assertThat(after).isEqualTo(before);
    }

    @Test
    void floatTest() {
        float before = 0.12345f;
        builder.writeFloat(before);

        float after = getParser().readFloat();

        assertThat(after).isEqualTo(before);
    }

    @Test
    void uuidTest() {
        UUID before = new UUID(2L^42, 2L^12);
        builder.writeUUID(before);

        UUID after = getParser().readUUID();

        assertThat(after).isEqualTo(before);
    }
}