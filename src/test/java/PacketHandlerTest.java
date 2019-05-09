import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PacketHandlerTest {

    @Test
    public void testbig() {
        ByteArrayReader byteArrayReader = new ByteArrayReader(new byte[]{(byte) 0xff, 0x01});
        assertEquals(PacketHandler.readVarInt(byteArrayReader), 255);
    }

    @Test
    public void testsmall() {
        ByteArrayReader byteArrayReader = new ByteArrayReader(new byte[]{(byte) 0x01});
        assertEquals(PacketHandler.readVarInt(byteArrayReader), 1);
    }

    @Test
    public void testmassive() {
        ByteArrayReader byteArrayReader = new ByteArrayReader(new byte[]{ (byte)0x80, (byte)0x80, (byte)0x80, (byte)0x80, (byte)0x08});
        assertEquals(PacketHandler.readVarInt(byteArrayReader), -2147483648);
    }

    @Test
    public void testdouble() {
        ByteArrayReader byteArrayReader = new ByteArrayReader(new byte[]{(byte) 0xff,(byte) 0xff, (byte)0xff,(byte) 0xff, (byte)0x07,
                (byte) 0x80, 0x01});
        assertEquals(2147483647, PacketHandler.readVarInt(byteArrayReader));
        assertEquals(128, PacketHandler.readVarInt(byteArrayReader));
    }
}
