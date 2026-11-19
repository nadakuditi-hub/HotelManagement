package com.hotel.config;

import com.hotel.service.FileStorageService;
import jakarta.servlet.MultipartConfigElement;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final FileStorageService storage;

    public WebConfig(FileStorageService storage) {
        this.storage = storage;
    }

    /** Serve uploaded room photos at http://localhost:8080/uploads/<file>. */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(storage.getDir().toUri().toString());
    }

    /** Limit uploads to 5 MB. */
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory f = new MultipartConfigFactory();
        f.setMaxFileSize(DataSize.ofMegabytes(5));
        f.setMaxRequestSize(DataSize.ofMegabytes(6));
        return f.createMultipartConfig();
    }
}
