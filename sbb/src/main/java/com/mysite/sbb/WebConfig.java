package com.mysite.sbb;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // /uploads/** -> file:///C:/upload/** 매핑
        String location = "file:///" + uploadDir.replace("\\", "/") + "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}
