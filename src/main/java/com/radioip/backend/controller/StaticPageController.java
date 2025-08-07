package com.radioip.backend.controller;

import com.radioip.backend.repository.SettingRepository;
import com.radioip.backend.model.Setting;
import java.util.Optional;
import java.net.HttpURLConnection;
import java.net.URL;

import com.radioip.backend.model.CustomPage;
import com.radioip.backend.repository.CustomPageRepository;
import com.radioip.backend.config.IceWatchConfig;
import com.radioip.backend.config.AppearanceConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;




@RestController
public class StaticPageController {


    @Autowired
    private CustomPageRepository repository;

    @Autowired
    private SettingRepository settingRepository;

    @Autowired
    private AppearanceConfig appearance;

    @Autowired
    private IceWatchConfig config;

    @GetMapping({"/", "/index"})
    public void index(HttpServletResponse response) throws IOException {
        sendHtmlWithSubstitutions("static/index.html", response);
    }

    @GetMapping("/login")
    public void login(HttpServletResponse response) throws IOException {
        sendHtmlWithSubstitutions("static/login.html", response);
    }

    @GetMapping("/dashboard")
    public void dashboard(HttpServletResponse response, HttpServletRequest request) throws IOException {
        if (request.getUserPrincipal() == null) {
            response.sendRedirect("/login");
            return;
        }

        sendHtmlWithSubstitutions("static/dashboard.html", response);
    }



private void sendHtmlWithSubstitutions(String path, HttpServletResponse response) throws IOException {
    String html = StreamUtils.copyToString(new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8);
    String logoutButton = "";
    String logoutTab = "";
    String journalButton = "";
    String journalTab = "";
    StringBuilder pageButtons = new StringBuilder();
    StringBuilder pageTabs = new StringBuilder();

    for (CustomPage page : repository.findAll()) {
        if (!page.isEnabled()) continue;

        String slug = page.getSlug();
        String title = page.getTitle();

        // 🔘 Bouton dans la barre d'onglets
    pageButtons.append("""
        <button class="tab-button" data-tab="%1$s">%2$s</button>
        """.formatted(slug, title));   
    pageTabs.append("""
        <div id="tab-%1$s" class="tab-content">
        %2$s
        </div>
        """.formatted(slug, page.getHtmlContent() != null ? page.getHtmlContent() : ""));


    }


    // Vérifie si un flux RSS est défini ET accessible (code HTTP 200)
    Optional<Setting> rss = settingRepository.findByKey("rss-url");
    if (rss.isPresent() && isValidRSS(rss.get().getValue())) {
        journalButton = "<button class=\"tab-button\" id=\"journal-tab-button\" data-tab=\"journal\">🗞️ Journal</button>";
        journalTab =
            "<div id=\"tab-journal\" class=\"tab-content\">\n" +
            "  <h2>🗞️ Journal étudiant</h2>\n" +
            "  <ul id=\"journal-articles\" class=\"rss-feed\">Chargement…</ul>\n" +
            "<button id=\"toggle-rss\" class=\"button-toggle\">📜 Afficher tout</button>" +
            "</div>";
    }
    if (!config.isDisableLogin()) {
    logoutButton = "<button id=\"logout-tab-button\" class=\"tab-button\" data-tab=\"logout\">🚪</button>";
    logoutTab =
        "<div id=\"tab-logout\" class=\"tab-content\" style=\"text-align:center;\">\n" +
        "  <p style=\"margin: 2rem;\">Cliquez ci-dessous pour vous déconnecter.</p>\n" +
        "  <a href=\"/logout\" class=\"button-logout\">🔒 Se déconnecter</a>\n" +
        "</div>";
}


    html = html
        .replace("${radio.title}", appearance.getRadioTitle())
        .replace("${radio.plainTitle}", appearance.getRadioPlainTitle())
        .replace("${welcome.message}", appearance.getWelcomeMessage())
        .replace("${login.title}", appearance.getLoginTitle())
        .replace("${favicon}", appearance.getFavicon())
        .replace("${custom.css}", appearance.getCustomCss())
        .replace("${custom.html}", appearance.getCustomHtml())
        .replace("${logout.button}", logoutButton)
        .replace("${logout.tab}", logoutTab)
        .replace("${journal.button}", journalButton)
        .replace("${journal.tab}", journalTab)
        .replace("${notes}", appearance.getNotes() != null ? appearance.getNotes() : "")
        .replace("${custompage.buttons}", pageButtons.toString())
        .replace("${custompage.tabs}", pageTabs.toString());

    response.setContentType("text/html; charset=UTF-8");
    response.getWriter().write(html);
}
private boolean isValidRSS(String urlStr) {
    try {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        conn.setConnectTimeout(3000);
        conn.connect();
        return conn.getResponseCode() == 200;
    } catch (Exception e) {
        return false;
    }
}

}
