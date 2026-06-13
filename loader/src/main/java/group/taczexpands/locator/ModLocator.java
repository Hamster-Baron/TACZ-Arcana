package group.taczexpands.locator;

import net.minecraftforge.fml.loading.moddiscovery.AbstractJarFileModLocator;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

public class ModLocator extends AbstractJarFileModLocator {
    @Override
    public Stream<Path> scanCandidates() {
        try {
            File tmpFile = File.createTempFile("tmp_", ".tmp");

            try (var inputStream = ModLocator.class.getResourceAsStream("/META-INF/mod.jar");
                 var fos = new FileOutputStream(tmpFile)) {
                byte[] buf = new byte[1024];
                int read;

                while ((read = inputStream.read(buf)) != -1) {
                    fos.write(buf, 0, read);
                }
            }

            tmpFile.deleteOnExit();
            return Stream.of(tmpFile.toPath());
        } catch (Exception e) {
            var newException = new RuntimeException("Failed to load TACZExpands. ");
            newException.setStackTrace(new StackTraceElement[]{});
            throw newException;
        }
    }

    @Override
    public String name() {
        return "TACZExpands Mod Locator";
    }

    @Override
    public void initArguments(Map<String, ?> arguments) {

    }
}

