package pl.edu.agh.to.gtfs.statics;

import org.springframework.stereotype.Component;
import pl.edu.agh.to.exceptions.StaticGtfsExtractException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class GtfsZipExtractor {

    public void extractZipToDirectory(Path zipFile, Path targetDir) {
        try {
            Files.createDirectories(targetDir);
        } catch (IOException e) {
            throw new StaticGtfsExtractException("Failed to create target directory: " + targetDir, e);
        }

        try (InputStream in = Files.newInputStream(zipFile);
             ZipInputStream zis = new ZipInputStream(in)) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                Path entryPath = targetDir.resolve(entry.getName()).normalize();

                if (!entryPath.startsWith(targetDir)) {
                    throw new StaticGtfsExtractException("Zip entry outside target dir: " + entry.getName());
                }

                Files.createDirectories(entryPath.getParent());
                Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
            }

        } catch (IOException e) {
            throw new StaticGtfsExtractException("Failed to extract zip: " + zipFile, e);
        } catch (RuntimeException e) {
            throw new StaticGtfsExtractException("Invalid zip structure: " + zipFile, e);
        }
    }
}