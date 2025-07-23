package com.radioip.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "appearance")
public class AppearanceConfig {

    private String radioTitle = "<span style='color:#e53935; font-weight:bold;'>Radio</span>Rosemont";
    private String welcomeMessage = "<p style='text-align:center; max-width:720px; margin:0 auto 1rem auto; font-size:1.1rem; line-height:1.6;'>Bienvenue sur la radio étudiante du Cégep Rosemont !<br>Découvrez des prestations musicales, des balados, des capsules éducatives, et plus encore — le tout produit avec passion par notre communauté collégiale.</p>";
    private String loginTitle = "Connexion | Radio Rosemont";
    private String customCss = "";
    private String customHtml = "";
    private String radioPlainTitle = "Radio Rosemont";
    private String favicon = "/favicon.png"; // valeur par défaut

    // Getters et Setters
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
