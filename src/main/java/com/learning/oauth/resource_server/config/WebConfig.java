package com.learning.oauth.resource_server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**") // Apply CORS to all paths under /api
                        .allowedOrigins("*") // Add your frontend origins
                        .allowedMethods("*") // Allowed HTTP methods
                        .allowedHeaders("*") // Allow all headers
                        .maxAge(3600); // Cache pre-flight response for 1 hour
            }
        };
    }
}