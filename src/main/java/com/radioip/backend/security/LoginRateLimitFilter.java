package com.radioip.backend.security;

import io.github.bucket4j.*;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimitFilter implements Filter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private final int maxAttempts;
    private final Duration blockDuration;

    public LoginRateLimitFilter(
            @Value("${icewatch.login.max-attempts:5}") int maxAttempts,
            @Value("${icewatch.login.block-minutes:10}") int blockMinutes
    ) {
        this.maxAttempts = maxAttempts;
        this.blockDuration = Duration.ofMinutes(blockMinutes);
    }

    private Bucket resolveBucket(String ip) {
        return buckets.computeIfAbsent(ip, k ->
            Bucket.builder()
                .addLimit(Bandwidth.classic(maxAttempts, Refill.greedy(maxAttempts, blockDuration)))
                .build());
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        if ("POST".equalsIgnoreCase(req.getMethod()) && "/login".equals(req.getServletPath())) {
            String ip = req.getRemoteAddr();
            Bucket bucket = resolveBucket(ip);

            if (!bucket.tryConsume(1)) {
                res.setStatus(429);
                res.setContentType("application/json");
                res.getWriter().write("{\"error\": \"Trop de tentatives de connexion. Réessaie dans quelques minutes.\"}");
                return;
            }
        }

        chain.doFilter(request, response);
    }
}
