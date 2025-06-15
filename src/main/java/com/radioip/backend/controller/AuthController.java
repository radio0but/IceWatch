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

    @GetMapping("/auth/token")
    public Map<String,String> getToken(HttpServletRequest req) {
        String ref = req.getHeader("Referer");
        if (ref == null || !ref.contains(config.getAllowedDomain())) {
            return Map.of("error","Access denied");
        }
        return Map.of("token", tokenService.generateToken());
    }
}
