package org.adam.lotterysystem.common.config;

import org.adam.lotterysystem.common.interceptor.LoginInterceptor;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AppConfigTest {

    @Test
    void shouldExcludeUppercaseImageSuffixes() throws Exception {
        AppConfig appConfig = new AppConfig();
        Field loginInterceptorField = AppConfig.class.getDeclaredField("loginInterceptor");
        loginInterceptorField.setAccessible(true);
        loginInterceptorField.set(appConfig, Mockito.mock(LoginInterceptor.class));

        InterceptorRegistry registry = new InterceptorRegistry();
        appConfig.addInterceptors(registry);

        Field registrationsField = InterceptorRegistry.class.getDeclaredField("registrations");
        registrationsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<InterceptorRegistration> registrations =
                (List<InterceptorRegistration>) registrationsField.get(registry);

        Field excludePatternsField = InterceptorRegistration.class.getDeclaredField("excludePatterns");
        excludePatternsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> excludePatterns =
                (List<String>) excludePatternsField.get(registrations.get(0));

        assertTrue(excludePatterns.contains("/*.JPG"));
        assertTrue(excludePatterns.contains("/*.PNG"));
    }
}
