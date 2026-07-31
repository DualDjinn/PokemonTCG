package ar.edu.utn.frc.tup.piii.configs;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SecurityHeadersFilter implements Filter {

    private static final String CSP_VALUE =
            "default-src 'self'; "
            + "script-src 'self' 'unsafe-inline' 'unsafe-eval' "
            +     "https://accounts.google.com https://apis.google.com "
            +     "https://ssl.gstatic.com https://www.gstatic.com "
            +     "https://secure.mlstatic.com https://sdk.mercadopago.com; "
            + "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com "
            +     "https://accounts.google.com; "
            + "font-src 'self' https://fonts.gstatic.com; "
            + "img-src 'self' data: https: blob:; "
            + "connect-src 'self' ws: wss: "
            +     "https://accounts.google.com https://www.googleapis.com "
            +     "https://secure.mlstatic.com https://sdk.mercadopago.com; "
            + "frame-src 'self' "
            +     "https://accounts.google.com "
            +     "https://sdk.mercadopago.com "
            +     "https://www.mercadopago.com.ar "
            +     "https://www.mercadopago.com;";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        // "same-origin-allow-popups" lets popups (Google Sign-In) keep their opener so
        // postMessage works. Chrome may still log a benign COOP warning that the Google /
        // MercadoPago SDKs trigger internally; it does not block the app (MercadoPago
        // Checkout Pro uses a full redirect via window.location.href, not a popup).
        httpResponse.setHeader("Cross-Origin-Opener-Policy", "same-origin-allow-popups");
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        httpResponse.setHeader("Content-Security-Policy", CSP_VALUE);
        chain.doFilter(request, response);
    }
}
