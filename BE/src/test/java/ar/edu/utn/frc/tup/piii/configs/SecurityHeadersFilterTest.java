package ar.edu.utn.frc.tup.piii.configs;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import jakarta.servlet.http.HttpServletResponse;
import org.mockito.InOrder;

class SecurityHeadersFilterTest {

    private SecurityHeadersFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new SecurityHeadersFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = mock(FilterChain.class);
    }

    @Test
    void doFilter_SetsCrossOriginOpenerPolicy() throws IOException, ServletException {
        filter.doFilter(request, response, chain);

        assertThat(response.getHeader("Cross-Origin-Opener-Policy"))
                .isEqualTo("same-origin-allow-popups");
    }

    @Test
    void doFilter_SetsXContentTypeOptionsNosniff() throws IOException, ServletException {
        filter.doFilter(request, response, chain);

        assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
    }

    @Test
    void doFilter_SetsContentSecurityPolicy() throws IOException, ServletException {
        filter.doFilter(request, response, chain);

        String csp = response.getHeader("Content-Security-Policy");
        assertThat(csp).isNotNull();
        assertThat(csp).startsWith("default-src 'self';");
        assertThat(csp).contains("script-src 'self'");
        assertThat(csp).contains("style-src 'self'");
        assertThat(csp).contains("font-src 'self'");
        assertThat(csp).contains("img-src 'self'");
        assertThat(csp).contains("connect-src 'self'");
        assertThat(csp).contains("frame-src 'self'");
    }

    @Test
    void doFilter_CspAllowsRequiredThirdPartyOrigins() throws IOException, ServletException {
        filter.doFilter(request, response, chain);

        String csp = response.getHeader("Content-Security-Policy");
        assertThat(csp)
                .as("CSP must whitelist providers the app integrates with")
                .contains("https://accounts.google.com")
                .contains("https://sdk.mercadopago.com")
                .contains("https://fonts.googleapis.com")
                .contains("https://fonts.gstatic.com");
    }

    @Test
    void doFilter_CallsChainExactlyOnce() throws IOException, ServletException {
        filter.doFilter(request, response, chain);

        verify(chain).doFilter(any(), any());
    }

    @Test
    void doFilter_SetsAllThreeSecurityHeaders() throws IOException, ServletException {
        filter.doFilter(request, response, chain);

        assertThat(response.getHeaderNames())
                .as("regression guard: do not silently drop a security header")
                .contains("Cross-Origin-Opener-Policy",
                          "X-Content-Type-Options",
                          "Content-Security-Policy");
    }

    @Test
    void doFilter_SetsHeadersBeforeInvokingChain() throws IOException, ServletException {
        HttpServletResponse responseSpy = spy(new MockHttpServletResponse());

        filter.doFilter(request, responseSpy, chain);

        InOrder ordered = inOrder(responseSpy, chain);
        ordered.verify(responseSpy).setHeader("Cross-Origin-Opener-Policy", "same-origin-allow-popups");
        ordered.verify(responseSpy).setHeader("X-Content-Type-Options", "nosniff");
        ordered.verify(responseSpy).setHeader(anyString(), any());
        ordered.verify(chain).doFilter(any(), any());
    }
}
