package pl.edu.agh.to.gtfs.statics;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pl.edu.agh.to.exceptions.StaticGtfsExtractException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StaticGtfsService {

    private final StaticGtfsClient staticGtfsClient;
    private final GtfsZipExtractor zipExtractor;

    @Value("#{'${ztp.gtfs.static.files}'.split(',')}")
    private List<String> staticFiles;

    @Value("${ztp.gtfs.data-dir:data}")
    private String dataDir;

    public Map<String, Path> downloadAndExtractAll() {
        Map<String, Path> downloaded = staticGtfsClient.downloadAllZipsToDisk();

        Map<String, Path> extractedRoots = new LinkedHashMap<>();
        Path extractedBase = Path.of(dataDir, "extracted");

        for (String rawName : staticFiles) {
            String fileName = rawName.trim();

            Path zipPath = downloaded.getOrDefault(fileName, Path.of(dataDir, fileName));
            if (!Files.exists(zipPath)) {
                throw new StaticGtfsExtractException("Zip file not found: " + zipPath);
            }

            String datasetKey = datasetKeyFromFileName(fileName);
            Path targetDir = extractedBase.resolve(datasetKey);

            try {
                zipExtractor.extractZipToDirectory(zipPath, targetDir);
            } catch (RuntimeException e) {
                throw new StaticGtfsExtractException(
                        "Failed to extract dataset " + datasetKey + " from " + zipPath,
                        e
                );
            }

            extractedRoots.put(datasetKey, targetDir);
        }

        return extractedRoots;
    }

    private static String datasetKeyFromFileName(String fileName) {
        int underscore = fileName.lastIndexOf('_');
        int dot = fileName.lastIndexOf('.');
        if (underscore < 0 || dot < 0 || underscore + 1 >= dot) {
            throw new StaticGtfsExtractException("Unexpected static GTFS file name: " + fileName);
        }
        return fileName.substring(underscore + 1, dot);
    }
}