// MetadataController.java
package com.radioip.backend.controller;

import com.radioip.backend.config.IceWatchConfig;
import com.radioip.backend.service.MetadataService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.radioip.backend.service.ArtistImageService;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Optional;

@RestController
public class MetadataController {

    private final IceWatchConfig config;
    private final MetadataService metadataService;

    @Autowired
    public MetadataController(IceWatchConfig config, MetadataService metadataService) {
        this.config = config;
        this.metadataService = metadataService;
    }

    @GetMapping("/radio/metadata")
    public Map<String, String> getMetadata(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", config.getAllowedDomain());

        try {
            String base = config.getIcecastStreamUrl().replace("/radio", "");
            URL url = new URL(base + "/status-json.xsl");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            String json;
            try (InputStream inputStream = conn.getInputStream()) {
                json = new String(inputStream.readAllBytes());
            }
            conn.disconnect();

            String title = "Inconnu";
            int index = json.indexOf("\"title\":\"");
            if (index != -1) {
                int start = index + 9;
                int end = json.indexOf("\"", start);
                if (end > start) {
                    title = json.substring(start, end);
                }
            }

            return Map.of("title", title);
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", "Impossible de lire les métadonnées");
        }
    }

    @GetMapping("/radio/metadata/enriched")
    public Optional<Map<String, Object>> getEnrichedMetadata(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", config.getAllowedDomain());

        Map<String, String> metadata = getMetadata(response);
        String title = metadata.getOrDefault("title", "Inconnu");

        return metadataService.enrich(title)
                .map(enriched -> {
                    // Assurer que enrich renvoie une Map de type Object
                    enriched.put("cover", enriched.get("cover"));
                    return enriched;
                });
    }
    @Autowired
    private ArtistImageService artistImageService;

    // Endpoint pour récupérer l'image de l'artiste
    @GetMapping("/artist/image")
    public String getArtistImage(@RequestParam String artistName) {
        Optional<String> imageUrl = artistImageService.getArtistImage(artistName);
        return imageUrl.orElse("/static/default-artist-image.png");  // URL de fallback si aucune image n'est trouvée
    }

}
