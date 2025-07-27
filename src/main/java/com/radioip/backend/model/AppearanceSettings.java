package com.radioip.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "appearance_settings")
public class AppearanceSettings {

    @Id
    private Long id = 1L; // Singleton

    @Column(length = 4096)
    private String radioTitle;

    @Column(length = 8192)
    private String notes;  // Nouveau champ pour le calepin

    @Column(length = 4096)
    private String welcomeMessage;

    @Column(length = 1024)
    private String loginTitle;

    @Column(length = 8192)
    private String customCss;

    @Column(length = 8192)
    private String customHtml;

    @Column(length = 1024)
    private String radioPlainTitle;

    @Column(length = 1024)
    private String favicon;

    // === Constructeurs ===
    public AppearanceSettings() {}

    public AppearanceSettings(Long id, String radioTitle, String welcomeMessage, String loginTitle,
                            String customCss, String customHtml, String radioPlainTitle,
                            String favicon, String notes) {
        this.id = id;
        this.radioTitle = radioTitle;
        this.welcomeMessage = welcomeMessage;
        this.loginTitle = loginTitle;
        this.customCss = customCss;
        this.customHtml = customHtml;
        this.radioPlainTitle = radioPlainTitle;
        this.favicon = favicon;
        this.notes = notes;
    }
    // === Getters / Setters ===

    public String getNotes() {
        return notes;
    }
    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRadioTitle() {
        return radioTitle;
    }

    public void setRadioTitle(String radioTitle) {
        this.radioTitle = radioTitle;
    }

    public String getWelcomeMessage() {
        return welcomeMessage;
    }

    public void setWelcomeMessage(String welcomeMessage) {
        this.welcomeMessage = welcomeMessage;
    }

    public String getLoginTitle() {
        return loginTitle;
    }

    public void setLoginTitle(String loginTitle) {
        this.loginTitle = loginTitle;
    }

    public String getCustomCss() {
        return customCss;
    }

    public void setCustomCss(String customCss) {
        this.customCss = customCss;
    }

    public String getCustomHtml() {
        return customHtml;
    }

    public void setCustomHtml(String customHtml) {
        this.customHtml = customHtml;
    }

    public String getRadioPlainTitle() {
        return radioPlainTitle;
    }

    public void setRadioPlainTitle(String radioPlainTitle) {
        this.radioPlainTitle = radioPlainTitle;
    }

    public String getFavicon() {
        return favicon;
    }

    public void setFavicon(String favicon) {
        this.favicon = favicon;
    }
}
