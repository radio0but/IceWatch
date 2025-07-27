package com.radioip.backend.controller;

import com.radioip.backend.config.IceWatchConfig;
import com.radioip.backend.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class AuthController {

    private final TokenService tokenService;
    private final IceWatchConfig config;

    @Autowired
    public AuthController(TokenService tokenService, IceWatchConfig config) {
        this.tokenService = tokenService;
        this.config = config;
    }
    @GetMapping("/auth/me")
    public Map<String, Object> me(HttpServletRequest req) {
    var user = req.getUserPrincipal();
    if (user == null) {
        return Map.of("authenticated", false);
    }

    boolean isAdmin = req.isUserInRole("ADMIN");
    return Map.of(
        "authenticated", true,
        "username", user.getName(),
        "role", isAdmin ? "ADMIN" : "USER"
    );
}
    @GetMapping("/config/frontend")
    public Map<String, String> getFrontendConfig(HttpServletRequest request) {
        // On peut ajouter le header CORS dynamiquement
        String allowed = config.getAllowedDomain();
        return Map.of("apiBase", allowed);
}

    @GetMapping("/auth/token")
    public Map<String,String> getToken(HttpServletRequest req) {
        String ref = req.getHeader("Referer");
        if (ref == null || !ref.startsWith(config.getAllowedDomain())) {
            return Map.of("error", "Access denied: " + ref);
        }
        return Map.of("token", tokenService.generateToken());
    }
}
