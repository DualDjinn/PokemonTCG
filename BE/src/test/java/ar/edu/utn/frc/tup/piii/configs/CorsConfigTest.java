package ar.edu.utn.frc.tup.piii.configs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CorsConfigTest {

    private CorsConfig config;
    private CorsRegistry registry;
    private CorsRegistration registration;

    @BeforeEach
    void setUp() {
        config = new CorsConfig();
        // El @Value no se inyecta sin contexto Spring; lo seteamos para que
        // addCorsMappings reciba un origen real en vez de null.
        ReflectionTestUtils.setField(config, "allowedOrigins", new String[]{"http://localhost:4200"});
        registry = mock(CorsRegistry.class);
        registration = mock(CorsRegistration.class);

        when(registry.addMapping(anyString())).thenReturn(registration);
        when(registration.allowedOriginPatterns(any(String[].class))).thenReturn(registration);
        when(registration.allowedMethods(any(String[].class))).thenReturn(registration);
        when(registration.allowedHeaders(any(String[].class))).thenReturn(registration);
        when(registration.exposedHeaders(any(String[].class))).thenReturn(registration);
        when(registration.allowCredentials(anyBoolean())).thenReturn(registration);
        when(registration.maxAge(anyLong())).thenReturn(registration);
    }

    @Test
    void addCorsMappings_AppliesToAllRoutes() {
        config.addCorsMappings(registry);

        verify(registry).addMapping("/**");
    }

    @Test
    void addCorsMappings_RestrictsAllowedOriginToLocalhostFrontend() {
        config.addCorsMappings(registry);

        verify(registration).allowedOriginPatterns("http://localhost:4200");
    }

    @Test
    void addCorsMappings_AllowsExpectedHttpMethods() {
        config.addCorsMappings(registry);

        verify(registration).allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
    }

    @Test
    void addCorsMappings_AllowsAllRequestHeaders() {
        config.addCorsMappings(registry);

        verify(registration).allowedHeaders("*");
    }

    @Test
    void addCorsMappings_ExposesSetCookieHeader() {
        config.addCorsMappings(registry);

        verify(registration).exposedHeaders("Set-Cookie");
    }

    @Test
    void addCorsMappings_AllowsCredentialsForCookieBasedAuth() {
        config.addCorsMappings(registry);

        verify(registration).allowCredentials(true);
    }

    @Test
    void addCorsMappings_SetsOneHourPreflightCache() {
        config.addCorsMappings(registry);

        verify(registration).maxAge(3600L);
    }
}
