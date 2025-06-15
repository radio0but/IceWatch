package com.radioip.backend.controller;

import com.radioip.backend.config.IceWatchConfig;
import com.radioip.backend.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@RestController
public class OwncastProxyController {

    private final TokenService tokenService;
    private final IceWatchConfig config;

    @Autowired
    public OwncastProxyController(TokenService tokenService, IceWatchConfig config) {
        this.tokenService = tokenService;
        this.config       = config;
    }

    @RequestMapping({
      "/owncast/**",
      "/_next/**",
      "/api/**","/hls/**",
      "/thumbnail.jpg","/logo","/manifest.json","/favicon.ico","/customjavascript.js"
    })
    public void proxyAll(
        HttpServletRequest request,
        HttpServletResponse response,
        @RequestParam(required = false) String token
    ) {
      try {
        String uri = request.getRequestURI();           // ex /owncast/embed/video
        // 1) protéger page & embed
        if (uri.startsWith("/owncast")) {
          String sub = uri.substring("/owncast".length());
          if ("".equals(sub) || "/".equals(sub) || sub.startsWith("/embed")) {
            if (token==null || !tokenService.isTokenValid(token)) {
              response.setStatus(HttpServletResponse.SC_FORBIDDEN);
              return;
            }
          }
        }
        // 2) construire URL cible
        String path  = uri.startsWith("/owncast")
                     ? uri.substring("/owncast".length())
                     : uri;
        String qs    = request.getQueryString();
        String target= config.getOwncastUrl()+path+(qs!=null?"?"+qs:"");

        // 3) appeler Owncast
        HttpURLConnection conn = (HttpURLConnection)new URL(target).openConnection();
        conn.setRequestMethod(request.getMethod());
        conn.setRequestProperty("User-Agent","IceWatch-Proxy");

        // 4) propager Content-Type
        String ct = conn.getContentType();
        if (ct!=null) response.setContentType(ct);

        // 5) CORS pour front
        String origin = request.getHeader("Origin");
        if (origin!=null) {
          response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,origin);
          response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS,"true");
        }

        // 6) streamer la réponse
        try (
          InputStream  in  = conn.getInputStream();
          OutputStream out = response.getOutputStream()
        ) { in.transferTo(out); }
        conn.disconnect();
      }
      catch(Exception e){
        e.printStackTrace();
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      }
    }
}
