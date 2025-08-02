package com.radioip.backend.controller;

import com.radioip.backend.model.SlotInfo;
import com.radioip.backend.repository.SlotInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/admin/emission")
public class SlotInfoController {

    @Autowired
    private SlotInfoRepository repo;

    @GetMapping
    public ResponseEntity<Map<String, String>> getInfo(
            @RequestParam String day,
            @RequestParam String hour) {

        Optional<SlotInfo> opt = repo.findByDayAndHour(day, hour);
        return ResponseEntity.ok(Map.of(
            "audio", opt.map(SlotInfo::getAudioInfo).orElse(""),
            "video", opt.map(SlotInfo::getVideoInfo).orElse("")
        ));
    }

    @PostMapping
    public ResponseEntity<Void> saveInfo(@RequestBody Map<String, String> payload) {
        String day = payload.get("day");
        String hour = payload.get("hour");
        String audio = payload.getOrDefault("audio", "");
        String video = payload.getOrDefault("video", "");

        SlotInfo info = repo.findByDayAndHour(day, hour).orElse(new SlotInfo());
        info.setDay(day);
        info.setHour(hour);
        info.setAudioInfo(audio);
        info.setVideoInfo(video);

        repo.save(info);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/current")
    public ResponseEntity<Map<String, String>> getCurrentSlotInfo() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/Toronto"));
        String day = now.getDayOfWeek().name().toLowerCase();
        String hour = String.format("%02d", now.getHour());

        Map<String, String> jours = Map.of(
            "monday", "lundi",
            "tuesday", "mardi",
            "wednesday", "mercredi",
            "thursday", "jeudi",
            "friday", "vendredi",
            "saturday", "samedi",
            "sunday", "dimanche"
        );

        String jourFr = jours.getOrDefault(day, day);
        Optional<SlotInfo> opt = repo.findByDayAndHour(jourFr, hour);

        return ResponseEntity.ok(Map.of(
            "audioInfo", opt.map(SlotInfo::getAudioInfo).orElse(""),
            "videoInfo", opt.map(SlotInfo::getVideoInfo).orElse("")
        ));
    }
}
