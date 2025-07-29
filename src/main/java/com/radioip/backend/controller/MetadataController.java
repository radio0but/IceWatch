// MetadataController.java
package com.radioip.backend.controller;

import com.radioip.backend.config.IceWatchConfig;
import com.radioip.backend.service.ArtistImageService;
import com.radioip.backend.service.MetadataService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.Locale;
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

    @Autowired
    private ArtistImageService artistImageService;

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
                json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
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
                    enriched.put("cover", enriched.get("cover")); // placeholder
                    return enriched;
                });
    }

    @GetMapping("/artist/image")
    public String getArtistImage(@RequestParam String artistName) {
        Optional<String> imageUrl = artistImageService.getArtistImage(artistName);
        return imageUrl.orElse("/static/default-artist-image.png");
    }

    @GetMapping("/radio/metadata/info")
    public ResponseEntity<String> getEmissionInfo() {
        try {
            LocalDateTime now = LocalDateTime.now();
            DayOfWeek day = now.getDayOfWeek();
            String jour = day.getDisplayName(TextStyle.FULL, Locale.FRENCH).toLowerCase();
            String jourNum = String.valueOf(day.getValue());
            String heure = String.format("%02d", now.getHour());

            Path pathVideo = Path.of("/srv/owncast-schedule", jourNum + capitalize(jour), heure);
            Path pathAudio = Path.of("/srv/radioemissions", jour, heure);

            System.out.println("📺 pathVideo: " + pathVideo);
            System.out.println("📻 pathAudio: " + pathAudio);

            // Vidéo
            if (Files.exists(pathVideo.resolve("play")) || Files.exists(pathVideo.resolve("live"))) {
                Path info = pathVideo.resolve("info.md");
                if (Files.exists(info)) return readMarkdown(info);
            }

            // Audio
            Path info = pathAudio.resolve("info.md");
            if (Files.exists(info)) return readMarkdown(info);


            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erreur interne");
        }
    }

    private ResponseEntity<String> readMarkdown(Path infoPath) {
        try {
            String md = Files.readString(infoPath, StandardCharsets.UTF_8);
            return ResponseEntity.ok(md);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Erreur lecture info.md");
        }
    }

    private String capitalize(String str) {
        return str == null || str.isEmpty()
                ? str
                : str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
