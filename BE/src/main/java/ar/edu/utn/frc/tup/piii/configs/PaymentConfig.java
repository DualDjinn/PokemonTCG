package ar.edu.utn.frc.tup.piii.configs;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuración de la pasarela de pago.
 *
 * <p>Lee las propiedades {@code app.mercadopago.*} de {@code application.properties}
 * mediante inyección de constructor con {@code @Value}. Todos los valores admiten
 * override por variable de entorno (p.ej. {@code MERCADOPAGO_ACCESS_TOKEN}) para que
 * los despliegues en producción nunca expongan credenciales en el código fuente.
 *
 * <p>También registra el bean {@link RestTemplate} compartido que usa
 * {@link ar.edu.utn.frc.tup.piii.services.adapters.MercadoPagoAdapter}
 * para las llamadas HTTP a la API REST de MercadoPago.
 */
@Configuration
@Getter
public class PaymentConfig {

    /** Token de acceso de la cuenta MercadoPago (producción o prueba). */
    private final String accessToken;

    /** URL a la que MP redirige al comprador tras un pago aprobado. */
    private final String successUrl;

    /** URL a la que MP redirige al comprador si el pago es rechazado. */
    private final String failureUrl;

    /** URL a la que MP redirige al comprador si el pago queda pendiente. */
    private final String pendingUrl;

    /**
     * URL pública donde MP enviará las notificaciones IPN (webhook).
     * Debe ser accesible por los servidores de MercadoPago; se omite en entornos locales.
     */
    private final String notificationUrl;

    /** {@code true} para usar {@code sandbox_init_point} (pruebas); {@code false} para producción. */
    private final boolean sandbox;

    /**
     * Clave secreta configurada en el panel de webhooks de MercadoPago.
     * Si está vacía, la validación de firma se omite (modo sin secret).
     */
    private final String webhookSecret;

    /**
     * @param accessToken     token OAuth de la cuenta MP
     * @param successUrl      URL de redirección tras pago aprobado
     * @param failureUrl      URL de redirección tras pago rechazado
     * @param pendingUrl      URL de redirección para pagos pendientes
     * @param notificationUrl URL pública para notificaciones IPN
     * @param sandbox         {@code true} activa el checkout sandbox de MP
     * @param webhookSecret   secret para validar la firma HMAC-SHA256 del webhook (opcional)
     */
    public PaymentConfig(
               @Value("${app.mercadopago.access-token}") String accessToken,
            @Value("${app.mercadopago.success-url}") String successUrl,
            @Value("${app.mercadopago.failure-url}") String failureUrl,
            @Value("${app.mercadopago.pending-url}") String pendingUrl,
            @Value("${app.mercadopago.notification-url}") String notificationUrl,
            @Value("${app.mercadopago.sandbox}") boolean sandbox,
            @Value("${app.mercadopago.webhook-secret:}") String webhookSecret) {
        this.accessToken = accessToken;
        this.successUrl = successUrl;
        this.failureUrl = failureUrl;
        this.pendingUrl = pendingUrl;
        this.notificationUrl = notificationUrl;
        this.sandbox = sandbox;
        this.webhookSecret = webhookSecret;
    }

    /**
     * Bean HTTP compartido para llamadas a la API REST de MercadoPago.
     *
     * @return instancia de {@link RestTemplate} sin configuración adicional
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
