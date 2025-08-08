package com.radioip.backend.model;

import jakarta.persistence.*;

@Entity
public class CustomPage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String slug; // utilisé dans l'URL (/pages/slug)

    @Column(nullable = false)
    private String title; // Titre affiché dans l'onglet

    @Lob
    @Column(nullable = false)
    private String htmlContent; // Contenu HTML

    private boolean enabled = true; // Page visible ou non


    @Column(nullable = true)
    private String icon; // Emoji ou code d'icône choisi pour le bouton

    // === Getters & Setters ===
    public String getIcon() { 
        return icon; 
    }
    public void setIcon(String icon) { 
        this.icon = icon;
     }
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getHtmlContent() {
        return htmlContent;
    }

    public void setHtmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
