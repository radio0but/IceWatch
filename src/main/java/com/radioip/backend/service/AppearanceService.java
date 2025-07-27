package com.radioip.backend.service;

import com.radioip.backend.model.AppearanceSettings;
import com.radioip.backend.repository.AppearanceSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AppearanceService {

    @Autowired
    private AppearanceSettingsRepository repo;

    public AppearanceSettings get() {
        AppearanceSettings s = repo.findById(1L).orElse(new AppearanceSettings());

        if (s.getRadioTitle() == null || s.getRadioTitle().isBlank())
            s.setRadioTitle("<span style='color:#2196f3; font-weight:bold;'>Ice</span>Watch");

        if (s.getWelcomeMessage() == null || s.getWelcomeMessage().isBlank())
            s.setWelcomeMessage("""
                <p style='text-align:center; max-width:720px; margin:0 auto 1rem auto; font-size:1.1rem; line-height:1.6;'>
                    Bienvenue sur <strong>IceWatch</strong>, le système de diffusion sécurisé de votre établissement.
                    <br>Vous pouvez personnaliser ce message dans le tableau de bord d’administration.
                </p>
            """);

        if (s.getLoginTitle() == null || s.getLoginTitle().isBlank())
            s.setLoginTitle("Connexion | IceWatch");

        if (s.getCustomCss() == null)
            s.setCustomCss("");

        if (s.getCustomHtml() == null)
            s.setCustomHtml("<div style='text-align:center;'>Bienvenue sur IceWatch.</div>");

        if (s.getRadioPlainTitle() == null || s.getRadioPlainTitle().isBlank())
            s.setRadioPlainTitle("IceWatch");

        if (s.getFavicon() == null || s.getFavicon().isBlank())
            s.setFavicon("/favicon.png");

        if (s.getNotes() == null)
            s.setNotes("");

        s.setId(1L);
        return repo.save(s);
    }

    public void save(AppearanceSettings newSettings) {
        newSettings.setId(1L);
        newSettings.setCustomHtml(sanitizeHtml(newSettings.getCustomHtml()));
        repo.save(newSettings);
    }

    public void reset() {
        AppearanceSettings existing = get(); // récupère les valeurs existantes
        String preservedNotes = existing.getNotes();

        AppearanceSettings reset = new AppearanceSettings();
        reset.setId(1L);
        reset.setRadioTitle("<span style='color:#2196f3; font-weight:bold;'>Ice</span>Watch");
        reset.setWelcomeMessage("""
            <p style='text-align:center; max-width:720px; margin:0 auto 1rem auto; font-size:1.1rem; line-height:1.6;'>
                Bienvenue sur <strong>IceWatch</strong>, le système de diffusion sécurisé de votre établissement.
                <br>Vous pouvez personnaliser ce message dans le tableau de bord d’administration.
            </p>
        """);
        reset.setLoginTitle("Connexion | IceWatch");
        reset.setCustomCss("");
        reset.setCustomHtml("<div style='text-align:center;'>Bienvenue sur IceWatch.</div>");
        reset.setRadioPlainTitle("IceWatch");
        reset.setFavicon("/favicon.png");
        reset.setNotes(preservedNotes); // ← conserve les notes existantes

        repo.save(reset);
    }

    private String sanitizeHtml(String html) {
        if (html == null) return "";
        return html
            .replaceAll("(?i)<script.*?>.*?</script>", "")
            .replaceAll("(?i)javascript:", "#")
            .replaceAll("(?i)on[a-z]+\\s*=", "");
    }
}
