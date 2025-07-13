// src/main/java/com/radioip/backend/config/IceWatchConfig.java
package com.radioip.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "icewatch")
public class IceWatchConfig {
    private String masterToken;
    private String allowedDomain;
    private String owncastUrl;
    private String icecastStreamUrl;
    private String adminPassword;
    private String enseignantPassword;
    // Getters and setters
    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public String getEnseignantPassword() {
        return enseignantPassword;
    }

    public void setEnseignantPassword(String enseignantPassword) {
        this.enseignantPassword = enseignantPassword;
    }
    public String getMasterToken() {
        return masterToken;
    }

    public void setMasterToken(String masterToken) {
        this.masterToken = masterToken;
    }

    public String getAllowedDomain() {
        return allowedDomain;
    }

    public void setAllowedDomain(String allowedDomain) {
        this.allowedDomain = allowedDomain;
    }

    public String getOwncastUrl() {
        return owncastUrl;
    }

    public void setOwncastUrl(String owncastUrl) {
        this.owncastUrl = owncastUrl;
    }

    public String getIcecastStreamUrl() {
        return icecastStreamUrl;
    }

    public void setIcecastStreamUrl(String icecastStreamUrl) {
        this.icecastStreamUrl = icecastStreamUrl;
    }
}
