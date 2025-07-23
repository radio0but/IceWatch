package com.radioip.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/settings")
public class AdminSettingsController {

    private static final Path PROPERTIES_PATH = Path.of("/etc/icewatch/application.properties");

    // Retourne uniquement les lignes "appearance.*"
    @GetMapping
    public ResponseEntity<String> getAppearanceSettings() {
        try {
            List<String> lines = Files.readAllLines(PROPERTIES_PATH, StandardCharsets.UTF_8);
            String appearanceOnly = lines.stream()
                    .filter(line -> line.trim().startsWith("appearance."))
                    .collect(Collectors.joining("\n"));
            return ResponseEntity.ok(appearanceOnly);
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body("Erreur de lecture : " + e.getMessage());
        }
    }

    // Remplace uniquement les lignes "appearance.*"
    @PostMapping
    public ResponseEntity<String> updateAppearanceSettings(@RequestBody String newAppearanceBlock) {
        try {
            List<String> existingLines = Files.readAllLines(PROPERTIES_PATH, StandardCharsets.UTF_8);

            List<String> updatedLines = existingLines.stream()
                    .filter(line -> !line.trim().startsWith("appearance."))
                    .collect(Collectors.toList());

            updatedLines.add("");
           // updatedLines.add("# === Apparence (modifié via dashboard) ===");
            updatedLines.addAll(List.of(newAppearanceBlock.split("\n")));

            Files.write(PROPERTIES_PATH, updatedLines, StandardCharsets.UTF_8);

            return ResponseEntity.ok("Configuration d'apparence mise à jour.");
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body("Erreur d’écriture : " + e.getMessage());
        }
    }
    @PostMapping("/reset")
    public ResponseEntity<String> resetAppearance() {
        try {
            List<String> lines = Files.readAllLines(PROPERTIES_PATH, StandardCharsets.UTF_8);

            // Supprimer toutes les lignes appearance.*
            List<String> updated = lines.stream()
                .filter(line -> !line.trim().startsWith("appearance."))
                .collect(Collectors.toList());

            Files.write(PROPERTIES_PATH, updated, StandardCharsets.UTF_8);

            return ResponseEntity.ok("Les paramètres d'apparence ont été réinitialisés.");
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body("Erreur lors de la suppression : " + e.getMessage());
        }
    }
    @PostMapping("/restart")
    public ResponseEntity<String> restartIceWatch() {
        try {
            new ProcessBuilder("sudo", "systemctl", "restart", "icewatch").start();
            return ResponseEntity.ok("IceWatch redémarré.");
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body("Erreur de redémarrage : " + e.getMessage());
        }
    }
}
