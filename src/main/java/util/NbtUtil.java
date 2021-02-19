package util;

import org.apache.commons.io.IOUtils;
import proxy.CompressionManager;
import se.llbit.nbt.NamedTag;
import se.llbit.nbt.Tag;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class NbtUtil {

    public static Tag read(File f) throws IOException {
        return read(new FileInputStream(f));
    }

    public static Tag read(InputStream input) throws IOException {
        byte[] fileContent = IOUtils.toByteArray(input);
        return NamedTag.read(
                new DataInputStream(new ByteArrayInputStream(CompressionManager.gzipDecompress(fileContent)))
        );
    }

   public static void write(Tag nbt, Path destination) throws IOException {
       ByteArrayOutputStream output = new ByteArrayOutputStream();
       nbt.write(new DataOutputStream(output));

       byte[] compressed = CompressionManager.gzipCompress(output.toByteArray());
       Files.write(destination, compressed);
   }
}
