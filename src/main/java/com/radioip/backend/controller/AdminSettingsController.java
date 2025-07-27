package com.radioip.backend.controller;

import com.radioip.backend.model.AppearanceSettings;
import com.radioip.backend.service.AppearanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/settings")
public class AdminSettingsController {

    @Autowired
    private AppearanceService appearanceService;

    private static final Path PROPERTIES_PATH = Path.of("/etc/icewatch/application.properties");

    // === Apparence (via base de données) ===
    @GetMapping
    public ResponseEntity<String> exportAsProperties() {
        AppearanceSettings s = appearanceService.get();
        String content = """
            appearance.radio-title=%s
            appearance.welcome-message=%s
            appearance.login-title=%s
            appearance.custom-css=%s
            appearance.custom-html=%s
            appearance.radio-plain-title=%s
            appearance.favicon=%s
            """.formatted(
                escape(s.getRadioTitle()),
                escape(s.getWelcomeMessage()),
                escape(s.getLoginTitle()),
                escape(s.getCustomCss()),
                escape(s.getCustomHtml()),
                escape(s.getRadioPlainTitle()),
                escape(s.getFavicon())
        );
        return ResponseEntity.ok(content);
    }

    @PostMapping
    public ResponseEntity<String> importFromProperties(@RequestBody String body) {
        AppearanceSettings s = appearanceService.get();
        Arrays.stream(body.split("\n")).forEach(line -> {
            if (!line.contains("=")) return;
            String[] parts = line.split("=", 2);
            String key = parts[0].trim();
            String val = parts[1].trim();

            switch (key) {
                case "appearance.radio-title" -> s.setRadioTitle(val);
                case "appearance.welcome-message" -> s.setWelcomeMessage(val);
                case "appearance.login-title" -> s.setLoginTitle(val);
                case "appearance.custom-css" -> s.setCustomCss(val);
                case "appearance.custom-html" -> s.setCustomHtml(val);
                case "appearance.radio-plain-title" -> s.setRadioPlainTitle(val);
                case "appearance.favicon" -> s.setFavicon(val);
            }
        });
        appearanceService.save(s);
        return ResponseEntity.ok("Apparence mise à jour.");
    }

    @PostMapping("/reset")
    public ResponseEntity<String> resetAppearance() {
        appearanceService.reset(); // <-- appelle la méthode dédiée qui conserve les notes
        return ResponseEntity.ok("Apparence réinitialisée.");
    }


    private String escape(String input) {
        return input == null ? "" : input.replace("\\", "\\\\").replace("\n", "\\n");
    }

    // === Redémarrage des services ===

    @PostMapping("/restart")
    public ResponseEntity<String> restartIceWatch() {
        return restartService("icewatch", "IceWatch");
    }

    @PostMapping("/restart-video-scheduler")
    public ResponseEntity<String> restartVideoScheduler() {
        return restartService("video-scheduler.service", "Vidéo Scheduler");
    }

    @PostMapping("/restart-radio-scheduler")
    public ResponseEntity<String> restartRadioScheduler() {
        return restartService("radio-scheduler.service", "Radio Scheduler");
    }

    private ResponseEntity<String> restartService(String service, String label) {
        try {
            new ProcessBuilder("sudo", "systemctl", "restart", service).start().waitFor();
            return ResponseEntity.ok(label + " redémarré !");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erreur redémarrage " + label + " : " + e.getMessage());
        }
    }

    // === Statut des services ===

    @GetMapping("/scheduler-status")
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
            return new String(process.getInputStream().readAllBytes()).trim(); // active / inactive / failed
        } catch (Exception e) {
            return "unknown";
        }
    }

    // === Logs des services ===

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
            ProcessBuilder pb = new ProcessBuilder("sudo", "journalctl", "-u", serviceName, "--no-pager", "-n", "50");
            Process p = pb.start();
            p.waitFor();
            return ResponseEntity.ok(new String(p.getInputStream().readAllBytes()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erreur journalctl : " + e.getMessage());
        }
    }
}
