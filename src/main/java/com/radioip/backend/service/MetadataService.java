package com.radioip.backend.service;

import org.springframework.stereotype.Service;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class MetadataService {

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

            // Affichage du JSON pour le débogage
            System.out.println("Réponse JSON brute : " + json);

            Map<String, Object> enriched = new HashMap<>();
            enriched.put("title", title);
            enriched.put("artist", artist);

            List<Map<String, String>> albums = new ArrayList<>();
            int pos = 0;
            while ((pos = json.indexOf("{\"id\":\"", pos)) != -1) {
                Map<String, String> album = new HashMap<>();

                // Extraction de l'ID de l'album
                int idStart = json.indexOf("\"id\":\"", pos) + 6;
                int idEnd = json.indexOf("\"", idStart);
                if (idStart == -1 || idEnd == -1) break;
                album.put("id", json.substring(idStart, idEnd));

                // Extraction du titre de l'album
                int titleStart = json.indexOf("\"title\":\"", idEnd) + 10;
                int titleEnd = json.indexOf("\"", titleStart);
                if (titleStart == -1 || titleEnd == -1) {
                    System.out.println("Erreur dans l'extraction du titre");
                    break;
                }

                // Extrait le titre complet sans modification
                String albumTitle = json.substring(titleStart - 1, titleEnd);

                // Log du titre extrait pour le débogage
                System.out.println("Titre de l'album extrait : " + albumTitle);

                // Vérification si la première lettre du titre est manquante
                if (albumTitle.length() > 1 && !Character.isLetterOrDigit(albumTitle.charAt(0))) {
                    albumTitle = albumTitle.substring(1); // Ajouter la première lettre manquante
                }

                // Vérifier que le titre est valide
                if (albumTitle != null && !albumTitle.isEmpty()) {
                    album.put("album", albumTitle);
                } else {
                    System.out.println("Titre vide ou invalide pour l'album");
                }

                // Extraction de la première date de sortie
                int firstReleaseDateStart = json.indexOf("\"first-release-date\":\"", titleEnd);
                if (firstReleaseDateStart != -1) {
                    firstReleaseDateStart += 21;  // Length of the string "\"first-release-date\":\""
                    int firstReleaseDateEnd = json.indexOf("\"", firstReleaseDateStart);
                    if (firstReleaseDateEnd > firstReleaseDateStart) {
                        album.put("firstReleaseDate", json.substring(firstReleaseDateStart, firstReleaseDateEnd));
                    }
                }

                // Extraction de la date de sortie
                int dateStart = json.indexOf("\"date\":\"", titleEnd);
                if (dateStart != -1) {
                    dateStart += 8;
                    int dateEnd = json.indexOf("\"", dateStart);
                    if (dateEnd > dateStart) {
                        album.put("date", json.substring(dateStart, dateEnd));
                    }
                }

                // URL de la couverture de l'album
                String coverUrl = "https://coverartarchive.org/release/" + album.get("id") + "/front-250";
                
                // Vérification de l'URL de couverture et ajout de l'image de fallback si nécessaire
                if (coverUrl == null || coverUrl.isEmpty()) {
                    coverUrl = "/static/album.png";  // URL de fallback pour l'image
                }

                album.put("cover", coverUrl);

                albums.add(album);
                pos = titleEnd;
            }

            // Si des albums ont été trouvés, les trier et ajouter les infos enrichies
            if (!albums.isEmpty()) {
                // Trier les albums par date
                albums.sort(Comparator.comparing(a -> a.getOrDefault("date", "9999")));

                // Inverser la liste pour afficher le dernier album en premier
                //Collections.reverse(albums);

                enriched.put("album", albums.get(0).get("album"));
                enriched.put("date", albums.get(0).get("date"));
                enriched.put("cover", albums.get(0).get("cover"));
                enriched.put("firstReleaseDate", albums.get(0).get("firstReleaseDate"));
                enriched.put("allAlbums", albums);
            }

            return Optional.of(enriched);
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
