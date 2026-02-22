package org.adam.lotterysystem.common.config;

import org.adam.lotterysystem.common.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;
@Configuration
public class AppConfig implements WebMvcConfigurer {
    @Autowired
    private LoginInterceptor loginInterceptor;

    private final List<String> excludePaths = Arrays.asList(
            "/**/*.html",
            "/css/**",
            "/js/**",
            "/images/**",
            "/pic/**",
            "/favicon.ico",
            "/**/login",
            "/register",
            "/verification-code/send",
            "/winning-records/show"
    );
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(excludePaths);
    }
}
