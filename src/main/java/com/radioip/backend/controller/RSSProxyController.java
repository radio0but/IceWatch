package com.radioip.backend.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@RestController
public class RSSProxyController {

    @GetMapping("/proxy/rss")
    public ResponseEntity<String> proxyRSS(@RequestParam String url) {
        try {
            URL rssUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) rssUrl.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(3000);
            conn.connect();

            InputStream is = conn.getInputStream();
            String xml = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.setContentType(MediaType.APPLICATION_XML);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(xml);

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("Erreur lors du chargement du flux RSS : " + e.getMessage());
        }
    }
}
