package pl.edu.agh.to.gtfs.statics;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
        // 1) download ZIPs to disk
        Map<String, Path> downloaded = staticGtfsClient.downloadAllZipsToDisk();

        // 2) extract each ZIP into data/extracted/<suffix>/
        Map<String, Path> extractedRoots = new LinkedHashMap<>();
        Path extractedBase = Path.of(dataDir, "extracted");

        for (String rawName : staticFiles) {
            String fileName = rawName.trim();

            Path zipPath = downloaded.get(fileName);
            if (zipPath == null) {
                zipPath = Path.of(dataDir, fileName);
            }

            String datasetKey = datasetKeyFromFileName(fileName); // A/M/T
            Path targetDir = extractedBase.resolve(datasetKey);

            zipExtractor.extractZipToDirectory(zipPath, targetDir);
            extractedRoots.put(datasetKey, targetDir);
        }

        return extractedRoots;
    }

    private static String datasetKeyFromFileName(String fileName) {
        int underscore = fileName.lastIndexOf('_');
        int dot = fileName.lastIndexOf('.');
        if (underscore < 0 || dot < 0 || underscore + 1 >= dot) {
            throw new IllegalStateException("Unexpected static GTFS file name: " + fileName);
        }
        return fileName.substring(underscore + 1, dot);
    }
}