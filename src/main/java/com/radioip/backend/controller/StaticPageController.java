package com.radioip.backend.controller;

import com.radioip.backend.config.IceWatchConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
public class StaticPageController {

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

    private void sendHtmlWithSubstitutions(String path, HttpServletResponse response) throws IOException {
        String html = StreamUtils.copyToString(new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8);
        String logoutButton = "";
        String logoutTab = "";

        if (!config.isDisableLogin()) {
            logoutButton = "<button class=\"tab-button\" data-tab=\"logout\">🚪 Déconnexion</button>";
            logoutTab =
                "<div id=\"tab-logout\" class=\"tab-content\" style=\"text-align:center;\">\n" +
                "  <p style=\"margin: 2rem;\">Cliquez ci-dessous pour vous déconnecter.</p>\n" +
                "  <a href=\"/logout\" class=\"button-logout\">🔒 Se déconnecter</a>\n" +
                "</div>";
        }
        html = html
            .replace("${radio.title}", config.getRadioTitle())
            .replace("${radio.plainTitle}", config.getRadioPlainTitle())
            .replace("${welcome.message}", config.getWelcomeMessage())
            .replace("${login.title}", config.getLoginTitle())
            .replace("${favicon}", config.getFavicon())
            .replace("${custom.css}", config.getCustomCss())
            .replace("${custom.html}", config.getCustomHtml())
            .replace("${logout.button}", logoutButton)
            .replace("${logout.tab}", logoutTab);

        response.setContentType("text/html; charset=UTF-8");
        response.getWriter().write(html);
    }
}
