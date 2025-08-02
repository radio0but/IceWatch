
package com.radioip.backend.controller;

import com.radioip.backend.service.EmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.*;


@RestController
@RequestMapping("/admin/schedule")
public class ScheduleStatusController {

    private static final String AUDIO_BASE = "/srv/radioemissions";
    private static final String VIDEO_BASE = "/srv/owncast-schedule";

    private static final String[] JOURS = {
        "dimanche", "lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi"
    };

    @Autowired
    private EmissionService emissionService;

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

    @GetMapping("/current")
    public Map<String, String> getCurrentStatus() {
        Calendar cal = Calendar.getInstance();
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK); // 1 = dimanche ... 7 = samedi
        int hour = cal.get(Calendar.HOUR_OF_DAY);

        String jour = JOURS[(dayOfWeek - 1 + 7) % 7]; // Assure compatibilité
        String heure = String.format("%02d", hour);

        String audioStatus = getStatus(new File(AUDIO_BASE + "/" + jour + "/" + heure), "radio.liq");
        String videoStatus = getStatus(new File(VIDEO_BASE + "/" + formatVideoPath(jour, heure)), "play");

        Map<String, String> result = new HashMap<>();
        result.put("audio", audioStatus);
        result.put("video", videoStatus);
        return result;
    }

    @GetMapping("/contents")
    public ResponseEntity<Map<String, Object>> getSlotContents(
        @RequestParam String day,
        @RequestParam String hour
    ) {
        Map<String, Object> result = new HashMap<>();

        String audioPath = AUDIO_BASE + "/" + day + "/" + hour;
        String videoPath = VIDEO_BASE + "/" + formatVideoPath(day, hour) + "/video";

        result.put("audio", listDirectoryRecursive(new File(audioPath)));
        result.put("video", listDirectoryRecursive(new File(videoPath)));

        return ResponseEntity.ok(result);
    }

    @PostMapping("/batch")
    public ResponseEntity<String> batchSaveEmission(@RequestBody Map<String, Object> payload) {
        String startDay = (String) payload.get("startDay");
        String startHour = (String) payload.get("startHour");
        String endAudioDay = (String) payload.get("endAudioDay");
        String endAudioHour = (String) payload.get("endAudioHour");
        String endVideoDay = (String) payload.get("endVideoDay");
        String endVideoHour = (String) payload.get("endVideoHour");
        String audio = (String) payload.get("audio");
        String video = (String) payload.get("video");

        List<String> days = List.of("lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche");

        int startIndex = days.indexOf(startDay) * 24 + Integer.parseInt(startHour);
        int endAudioIndex = days.indexOf(endAudioDay) * 24 + Integer.parseInt(endAudioHour);
        int endVideoIndex = days.indexOf(endVideoDay) * 24 + Integer.parseInt(endVideoHour);

        for (int i = startIndex; i <= endAudioIndex; i++) {
            String day = days.get((i / 24) % 7);
            String hour = String.format("%02d", i % 24);
            emissionService.setEmission(day, hour, audio, null); // audio seulement
        }

        for (int i = startIndex; i <= endVideoIndex; i++) {
            String day = days.get((i / 24) % 7);
            String hour = String.format("%02d", i % 24);
            emissionService.setEmission(day, hour, null, video); // vidéo seulement
        }

        return ResponseEntity.ok("OK");
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

        folders.sort(Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        regularFiles.sort(Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));

        for (File f : folders) {
            result.put(f.getName() + "/", listDirectoryRecursive(f));
        }

        for (File f : regularFiles) {
            result.put(f.getName(), null);
        }

        return result;
    }

    private String formatVideoPath(String day, String hour) {
        List<String> days = List.of("dimanche", "lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi");
        int index = days.indexOf(day.toLowerCase());
        if (index == -1) return day + "/" + hour;
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
