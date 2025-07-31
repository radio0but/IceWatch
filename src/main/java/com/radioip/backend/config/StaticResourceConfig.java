package com.radioip.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.http.CacheControl;

import java.util.concurrent.TimeUnit;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Pour que Spring serve correctement les .js comme application/javascript
        registry
        .addResourceHandler("/js/**")
        .addResourceLocations("classpath:/static/js/")
        .setCacheControl(CacheControl.noStore())
        .resourceChain(false);

    }
}
