package com.radioip.backend.controller;

import com.radioip.backend.model.AppearanceSettings;
import com.radioip.backend.service.AppearanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class AppearanceNotesController {

    private final AppearanceService appearanceService;

    @Autowired
    public AppearanceNotesController(AppearanceService appearanceService) {
        this.appearanceService = appearanceService;
    }

    @GetMapping("/appearance-notes")
    public Map<String, String> getNotes() {
        AppearanceSettings settings = appearanceService.get();
        String notes = settings.getNotes();
        return Map.of("notes", notes != null ? notes : "");
    }

    @PostMapping("/appearance-notes")
    public Map<String, String> updateNotes(@RequestBody Map<String, String> body) {
        String newNotes = body.getOrDefault("notes", "");
        AppearanceSettings settings = appearanceService.get();
        settings.setNotes(newNotes);
        appearanceService.save(settings);
        return Map.of("status", "ok");
    }
}
