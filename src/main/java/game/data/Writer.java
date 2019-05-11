package game.data;

import com.flowpowered.nbt.NBTUtils;
import com.flowpowered.nbt.Tag;
import com.flowpowered.nbt.stream.NBTInputStream;
import com.flowpowered.nbt.stream.NBTOutputStream;
import com.flowpowered.nbt.util.NBTMapper;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;

public class Writer {
    public static void write(Chunk c) throws IOException {
        NBTOutputStream out = new NBTOutputStream(new FileOutputStream(Paths.get("nbt-test").toFile()), false);
        out.writeTag(c.toNbt());
        out.flush();
        System.out.println(c.toNbt());
        System.out.println("Wrote chunk to file!");


    }

    public static void main(String[] args) throws IOException {
        NBTInputStream in = new NBTInputStream(new FileInputStream(Paths.get("level.dat").toFile()), true);
        Tag t = in.readTag();
        System.out.println(t);
    }
}
