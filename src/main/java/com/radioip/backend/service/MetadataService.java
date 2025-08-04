package com.radioip.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class MetadataService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Cacheable("metadata")
    public Optional<Map<String, Object>> enrich(String rawTitle) {
        if (!rawTitle.contains(" - ")) return Optional.empty();

        String[] parts = rawTitle.split(" - ", 2);
        String artist = parts[0].trim();
        String title = parts[1].trim();

        try {
            String query = "recording:\"%s\" AND artist:\"%s\"".formatted(title, artist);
            String url = "https://musicbrainz.org/ws/2/recording/?query=" +
                    URLEncoder.encode(query, StandardCharsets.UTF_8) + "&fmt=json&inc=releases+tags";

            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestProperty("User-Agent", "IceWatchRadio/1.0 (contact@boogiepit.com)");

            String json;
            try (InputStream is = conn.getInputStream()) {
                json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
            conn.disconnect();

            JsonNode root = objectMapper.readTree(json);
            JsonNode recordings = root.path("recordings");
            if (!recordings.isArray()) return Optional.empty();

            Map<String, Object> enriched = new HashMap<>();
            enriched.put("title", title);
            enriched.put("artist", artist);

            List<Map<String, String>> albums = new ArrayList<>();
            Set<String> seenTitles = new HashSet<>();

            for (JsonNode rec : recordings) {
                JsonNode releases = rec.path("releases");
                for (JsonNode release : releases) {
                    String id = release.path("id").asText();
                    String albumTitle = release.path("title").asText();
                    if (id.isEmpty() || albumTitle.isEmpty() || !seenTitles.add(albumTitle)) continue;

                    Map<String, String> album = new HashMap<>();
                    album.put("id", id);
                    album.put("album", albumTitle);
                    album.put("date", release.path("date").asText(""));
                    album.put("firstReleaseDate", release.path("first-release-date").asText(""));
                    album.put("cover", "https://coverartarchive.org/release/" + id + "/front-250");
                    albums.add(album);
                }
            }

            if (!albums.isEmpty()) {
                albums.sort(Comparator.comparing(a -> a.getOrDefault("date", "9999")));
                Map<String, String> latest = albums.get(0);
                enriched.put("album", latest.get("album"));
                enriched.put("date", latest.get("date"));
                enriched.put("cover", latest.get("cover"));
                enriched.put("firstReleaseDate", latest.get("firstReleaseDate"));
                enriched.put("allAlbums", albums);
            }

            return Optional.of(enriched);
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
