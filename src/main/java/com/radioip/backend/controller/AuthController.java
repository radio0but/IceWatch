package com.radioip.backend.controller;


import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

import com.radioip.backend.config.IceWatchConfig;
import com.radioip.backend.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class AuthController {

    private final TokenService tokenService;
    private final IceWatchConfig config;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Autowired
    public AuthController(TokenService tokenService, IceWatchConfig config) {
        this.tokenService = tokenService;
        this.config = config;
    }

    private Bucket resolveBucket(String ip) {
        return buckets.computeIfAbsent(ip, k ->
            Bucket.builder()
                .addLimit(Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1))))
                .build());
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
        return Map.of("apiBase", config.getAllowedDomain());
    }
    @RateLimiter(name = "tokenLimiter")
    @GetMapping("/auth/token")
    public ResponseEntity<?> getToken(HttpServletRequest req) {
        String ip = req.getRemoteAddr();
        Bucket bucket = resolveBucket(ip);

        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("error", "Trop de requêtes. Réessaie plus tard."));
        }

        String referer = req.getHeader("Referer");
        if (referer == null || !referer.startsWith(config.getAllowedDomain())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Accès interdit : Referer non autorisé."));
        }

        return ResponseEntity.ok(Map.of("token", tokenService.generateToken()));
    }

}
