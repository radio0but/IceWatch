package com.radioip.backend.controller;

import com.radioip.backend.config.IceWatchConfig;
import com.radioip.backend.service.TokenService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@RestController
public class StreamController {

    private final TokenService tokenService;
    private final IceWatchConfig config;

    @Autowired
    public StreamController(TokenService tokenService, IceWatchConfig config) {
        this.tokenService = tokenService;
        this.config = config;
    }

    @GetMapping(value = "/radio", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void streamRadio(
        @RequestParam(required = false) String token,
        HttpServletResponse response
    ) {
        if (token == null || !tokenService.isTokenValid(token)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        try {
            URL url = new URL(config.getIcecastStreamUrl());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            String contentType = conn.getContentType();
            response.setContentType(contentType != null ? contentType : "audio/mpeg");

            try (InputStream in = conn.getInputStream()) {
                in.transferTo(response.getOutputStream());
            }
            conn.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
        }
    }
}
