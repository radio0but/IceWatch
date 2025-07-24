package com.radioip.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;



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
        @PostMapping("/admin/settings/restart-video-scheduler")
    public ResponseEntity<Void> restartVideoScheduler() {
        try {
            ProcessBuilder pb = new ProcessBuilder("sudo", "systemctl", "restart", "video-scheduler.service");
            pb.start().waitFor();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/admin/settings/restart-radio-scheduler")
    public ResponseEntity<Void> restartRadioScheduler() {
        try {
            ProcessBuilder pb = new ProcessBuilder("sudo", "systemctl", "restart", "radio-scheduler.service");
            pb.start().waitFor();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
    @GetMapping("/admin/settings/scheduler-status")
public Map<String, String> getSchedulerStatus() {
    return Map.of(
        "video", checkService("video-scheduler.service"),
        "radio", checkService("radio-scheduler.service")
    );
}

private String checkService(String serviceName) {
    try {
        ProcessBuilder pb = new ProcessBuilder("systemctl", "is-active", serviceName);
        Process process = pb.start();
        process.waitFor();
        String output = new String(process.getInputStream().readAllBytes()).trim();
        return output; // active, inactive, failed, etc.
    } catch (Exception e) {
        return "unknown";
    }
}


@GetMapping("/logs/video-scheduler")
public ResponseEntity<String> getVideoSchedulerLogs() {
    return getServiceLogs("video-scheduler.service");
}

@GetMapping("/logs/radio-scheduler")
public ResponseEntity<String> getRadioSchedulerLogs() {
    return getServiceLogs("radio-scheduler.service");
}

private ResponseEntity<String> getServiceLogs(String serviceName) {
    try {
        ProcessBuilder pb = new ProcessBuilder("sudo","journalctl", "-u", serviceName, "--no-pager", "-n", "50");
        Process p = pb.start();
        p.waitFor();
        String output = new String(p.getInputStream().readAllBytes());
        return ResponseEntity.ok(output);
    } catch (Exception e) {
        return ResponseEntity.internalServerError().body("Erreur journalctl : " + e.getMessage());
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
