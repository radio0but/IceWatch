package com.radioip.backend.controller;

import com.radioip.backend.config.IceWatchConfig;
import com.radioip.backend.model.CustomPage;
import com.radioip.backend.repository.CustomPageRepository;
import com.radioip.backend.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/pages")
public class PublicPageController {

    @Autowired
    private CustomPageRepository repository;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private IceWatchConfig config;

    @GetMapping("/{slug}")
public ResponseEntity<String> viewPage(
        @PathVariable String slug,
        @RequestParam(required = false) String token
) {
    if (!tokenService.isTokenValid(token)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("⛔ Accès refusé");
    }

    Optional<CustomPage> pageOpt = repository.findBySlug(slug);
    if (pageOpt.isEmpty() || !pageOpt.get().isEnabled()) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("❌ Page non trouvée");
    }

    String html = pageOpt.get().getHtmlContent();
    return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(html != null ? html : "");
}

}
