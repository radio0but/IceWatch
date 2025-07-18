package com.radioip.backend.service;

import org.springframework.stereotype.Service;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
public class ArtistImageService {

    // URL de l'API MediaWiki (Wikipedia)
    private static final String WIKIPEDIA_API_URL = "https://en.wikipedia.org/w/api.php";

    public Optional<String> getArtistImage(String artistName) {
        try {
            // Formater l'URL pour interroger l'API de Wikipedia
            String queryUrl = WIKIPEDIA_API_URL + "?action=query&titles=" + artistName + "&prop=images&format=json";

            HttpURLConnection conn = (HttpURLConnection) new URL(queryUrl).openConnection();
            conn.setRequestProperty("User-Agent", "IceWatchRadio/1.0 (contact@boogiepit.com)");

            // Obtenir la réponse JSON
            String jsonResponse;
            try (InputStream is = conn.getInputStream()) {
                jsonResponse = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
            conn.disconnect();

            // Log pour débogage
            System.out.println("Réponse JSON de Wikipedia : " + jsonResponse);

            // Extraire le nom de l'image depuis la réponse JSON
            String imageUrl = extractImageUrl(jsonResponse);

            if (imageUrl != null) {
                return Optional.of(imageUrl); // Retourner l'URL complète de l'image
            }

            // Si l'image n'est pas trouvée, retourner une image par défaut
            return Optional.of("/static/default-artist-image.png");

        } catch (Exception e) {
            e.printStackTrace();
            return Optional.of("/static/default-artist-image.png"); // Retourner une image par défaut en cas d'erreur
        }
    }

    // Extraire l'URL de l'image depuis la réponse JSON de Wikipedia
    private String extractImageUrl(String jsonResponse) {
        // Vérifier la présence de la clé "images" dans la réponse JSON
        int imageStart = jsonResponse.indexOf("imageinfo") + 10;
        int imageEnd = jsonResponse.indexOf("\"", imageStart);
        if (imageStart != -1 && imageEnd != -1) {
            // Retourner l'URL complète de l'image
            String imageName = jsonResponse.substring(imageStart, imageEnd);
            return "https://en.wikipedia.org/wiki/File:" + imageName; // Construire l'URL complète de l'image
        }
        return null;
    }
}
