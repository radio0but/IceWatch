package com.radioip.backend.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCacheNames(List.of("metadata", "icecastMeta"));

        manager.setCacheSpecification("maximumSize=500,expireAfterWrite=2h");
        manager.registerCustomCache("icecastMeta",
            Caffeine.newBuilder()
                    .expireAfterWrite(5, TimeUnit.SECONDS)
                    .maximumSize(50)
                    .build()
        );

        return manager;
    }
}
