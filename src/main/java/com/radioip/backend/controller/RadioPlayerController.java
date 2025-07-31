package com.radioip.backend.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@RestController
public class RadioPlayerController {

    @GetMapping("/radioPlayer")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> getRadioPlayerPage() {
        try (InputStream in = new ClassPathResource("static/radioPlayer.html").getInputStream()) {
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return ResponseEntity.ok().body(content);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erreur de chargement du lecteur.");
        }
    }
}
