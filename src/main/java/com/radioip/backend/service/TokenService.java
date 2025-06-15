package com.radioip.backend.service;

import com.radioip.backend.config.IceWatchConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenService {

    private final IceWatchConfig config;

    private static final long EXPIRATION_TIME_SECONDS = 60;
    private static final Map<String, Long> tokenCache = new ConcurrentHashMap<>();

    @Autowired
    public TokenService(IceWatchConfig config) {
        this.config = config;
    }

    public String generateToken() {
        String token = UUID.randomUUID().toString();
        tokenCache.put(token, System.currentTimeMillis() / 1000L);
        return token;
    }

    public boolean isTokenValid(String token) {
        if (config.getMasterToken().equals(token)) {
            return true;
        }

        Long timestamp = tokenCache.get(token);
        if (timestamp == null) {
            return false;
        }

        return (System.currentTimeMillis() / 1000L) - timestamp <= EXPIRATION_TIME_SECONDS;
    }
}
