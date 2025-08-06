package com.jaywant.demo.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
                // Serve attendance images from resources/images directory
                registry.addResourceHandler("/images/**")
                                .addResourceLocations("file:src/main/resources/images/",
                                                "file:src/main/resources/static/images/");

                // Serve static files from resources/static directory
                registry.addResourceHandler("/static/**")
                                .addResourceLocations("file:src/main/resources/static/");

                // Serve uploads from uploads directory (relative to project root)
                registry.addResourceHandler("/uploads/**")
                                .addResourceLocations("file:uploads/");
        }
}
