package com.radioip.backend.controller;

import com.radioip.backend.config.IceWatchConfig;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

@RestController
public class MetadataController {

    private final IceWatchConfig config;

    @Autowired
    public MetadataController(IceWatchConfig config) {
        this.config = config;
    }

    @GetMapping("/radio/metadata")
    public Map<String, String> getMetadata(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", config.getAllowedDomain());

        try {
            // Construction dynamique de l'URL vers /status-json.xsl à partir de l’URL Icecast
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
}
