package com.radioip.backend.controller;

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

    private String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
