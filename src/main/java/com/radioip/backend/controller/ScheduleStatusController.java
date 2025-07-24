package com.radioip.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/admin/schedule")
public class ScheduleStatusController {

    private static final String AUDIO_BASE = "/srv/radioemissions";
    private static final String VIDEO_BASE = "/srv/owncast-schedule";

    private static final String[] JOURS = {
        "dimanche", "lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi"
    };

    @GetMapping
    public Map<String, Object> getScheduleStatus() {
        Map<String, Object> result = new LinkedHashMap<>();

        for (int jourIndex = 0; jourIndex < JOURS.length; jourIndex++) {
            String jourNom = JOURS[jourIndex];
            String jourCle = (jourIndex + 1) + capitalize(jourNom);

            Map<String, Map<String, String>> horaires = new LinkedHashMap<>();

            for (int h = 0; h < 24; h++) {
                String heure = String.format("%02d", h);

                String audioStatus = getStatus(new File(AUDIO_BASE + "/" + jourNom + "/" + heure), "radio.liq");
                String videoStatus = getStatus(new File(VIDEO_BASE + "/" + jourCle + "/" + heure), "play");

                Map<String, String> status = new HashMap<>();
                status.put("audio", audioStatus);
                status.put("video", videoStatus);

                horaires.put(heure, status);
            }

            result.put(jourNom, horaires);
        }

        return result;
    }

    @GetMapping("/contents")
    public ResponseEntity<Map<String, Object>> getSlotContents(
        @RequestParam String day,
        @RequestParam String hour
    ) {
        Map<String, Object> result = new HashMap<>();

        String audioPath = AUDIO_BASE + "/" + day + "/" + hour;
        String videoPath = VIDEO_BASE + "/" + formatVideoPath(day, hour) + "/video";  // ← ici, on force `/video`

        result.put("audio", listDirectoryRecursive(new File(audioPath)));
        result.put("video", listDirectoryRecursive(new File(videoPath)));

        return ResponseEntity.ok(result);
    }

    private Map<String, Object> listDirectoryRecursive(File dir) {
        if (!dir.exists() || !dir.isDirectory()) {
            return Map.of("(Dossier inexistant)", null);
        }

        Map<String, Object> result = new TreeMap<>();
        File[] files = dir.listFiles();
        if (files == null) return Map.of("(Erreur de lecture)", null);

        List<File> folders = new ArrayList<>();
        List<File> regularFiles = new ArrayList<>();

        for (File f : files) {
            if (f.isDirectory()) folders.add(f);
            else regularFiles.add(f);
        }

        // Trier les deux listes
        folders.sort(Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        regularFiles.sort(Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));

        // Ajouter d'abord les dossiers
        for (File f : folders) {
            result.put(f.getName() + "/", listDirectoryRecursive(f));
        }

        // Puis les fichiers
        for (File f : regularFiles) {
            result.put(f.getName(), null);
        }

        return result;
    }

    private String formatVideoPath(String day, String hour) {
        List<String> days = List.of("dimanche", "lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi");
        int index = days.indexOf(day.toLowerCase());
        if (index == -1) return day + "/" + hour; // fallback
        return (index + 1) + capitalize(day) + "/" + hour;
    }

    private String getStatus(File folder, String automationFileName) {
        if (!folder.exists() || !folder.isDirectory()) return "vide";

        File[] files = folder.listFiles();
        if (files == null) return "vide";

        boolean hasAutomation = Arrays.stream(files).anyMatch(f -> f.getName().equalsIgnoreCase(automationFileName));
        boolean hasLive = Arrays.stream(files).anyMatch(f -> f.getName().equalsIgnoreCase("live"));

        if (hasLive) return "live";
        if (hasAutomation) return "auto";
        return "vide";
    }

    private String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
