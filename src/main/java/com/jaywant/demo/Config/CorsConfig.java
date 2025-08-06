package com.jaywant.demo.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    // 1) Credentialed API endpoints
    registry.addMapping("/api/**")
        .allowedOriginPatterns(
            "http://localhost:3012",
            "http://localhost:3000",
            "https://admin.managifyhr.com",
            "https://*.managifyhr.com")
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD")
        .allowedHeaders("*")
        .allowCredentials(true)
        .maxAge(3600);

    // 2) Master-admin endpoints w/ credentials
    registry.addMapping("/masteradmin/**")
        .allowedOriginPatterns(
            "http://localhost:3012",
            "http://localhost:3000",
            "https://admin.managifyhr.com",
            "https://*.managifyhr.com")
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD")
        .allowedHeaders("*")
        .allowCredentials(true)
        .maxAge(3600);

    // 3) Public, non-credentialed
    for (String path : new String[] { "/api/fcm/**", "/images/**", "/uploads/**" }) {
      registry.addMapping(path)
          .allowedOriginPatterns("*")
          .allowedMethods("GET", "POST", "OPTIONS", "HEAD")
          .allowedHeaders("*")
          .allowCredentials(false)
          .maxAge(3600);
    }
  }
}
