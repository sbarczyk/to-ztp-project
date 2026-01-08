package pl.edu.agh.to.gtfs.statics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.edu.agh.to.exceptions.StaticGtfsExtractException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GtfsZipExtractorTest {

    private final GtfsZipExtractor extractor = new GtfsZipExtractor();

    @TempDir
    Path tempDir;

    @Test
    void shouldExtractZipSuccessfully() throws IOException {
        // Given
        Path zipFile = tempDir.resolve("test.zip");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile.toFile()))) {
            ZipEntry entry = new ZipEntry("stops.txt");
            zos.putNextEntry(entry);
            zos.write("content".getBytes());
            zos.closeEntry();
        }
        Path targetDir = tempDir.resolve("extracted");

        // When
        extractor.extractZipToDirectory(zipFile, targetDir);

        // Then
        Path extractedFile = targetDir.resolve("stops.txt");
        assertThat(extractedFile).exists();
        assertThat(Files.readString(extractedFile)).isEqualTo("content");
    }

    @Test
    void shouldThrowExceptionOnZipSlipAttack() throws IOException {
        // Given
        Path zipFile = tempDir.resolve("evil.zip");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile.toFile()))) {
            ZipEntry entry = new ZipEntry("../../evil.txt");
            zos.putNextEntry(entry);
            zos.write("hacked".getBytes());
            zos.closeEntry();
        }
        Path targetDir = tempDir.resolve("safe_dir");

        // When & Then
        assertThatThrownBy(() -> extractor.extractZipToDirectory(zipFile, targetDir))
                .isInstanceOf(StaticGtfsExtractException.class)
                .hasMessageContaining("Zip entry outside target dir");
    }
}