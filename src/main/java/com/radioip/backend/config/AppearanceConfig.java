package com.radioip.backend.config;

import com.radioip.backend.model.AppearanceSettings;
import com.radioip.backend.service.AppearanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AppearanceConfig {

    private final AppearanceService service;

    @Autowired
    public AppearanceConfig(AppearanceService service) {
        this.service = service;
    }

    public AppearanceSettings getSettings() {
        return service.get();
    }

    public String getNotes() {
    return getSettings().getNotes();
    }


    // Facilité d’accès directe si besoin
    public String getRadioTitle() {
        return getSettings().getRadioTitle();
    }

    public String getWelcomeMessage() {
        return getSettings().getWelcomeMessage();
    }

    public String getLoginTitle() {
        return getSettings().getLoginTitle();
    }

    public String getCustomCss() {
        return getSettings().getCustomCss();
    }

    public String getCustomHtml() {
        return getSettings().getCustomHtml();
    }

    public String getRadioPlainTitle() {
        return getSettings().getRadioPlainTitle();
    }

    public String getFavicon() {
        return getSettings().getFavicon();
    }
}
