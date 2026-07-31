package ar.edu.utn.frc.tup.piii.configs;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PaymentConfig}.
 *
 * <p>Verifica que el binding por constructor (valores {@code @Value}) quede expuesto por los
 * getters generados y que el bean {@link RestTemplate} se construya.
 */
class PaymentConfigTest {

    private PaymentConfig config() {
        return new PaymentConfig(
                "access-token-123",
                "https://app/success",
                "https://app/failure",
                "https://app/pending",
                "https://public/notify",
                true,
                "whsec_abc");
    }

    @Test
    void getters_ExposeConstructorValues() {
        PaymentConfig config = config();

        assertThat(config.getAccessToken()).isEqualTo("access-token-123");
        assertThat(config.getSuccessUrl()).isEqualTo("https://app/success");
        assertThat(config.getFailureUrl()).isEqualTo("https://app/failure");
        assertThat(config.getPendingUrl()).isEqualTo("https://app/pending");
        assertThat(config.getNotificationUrl()).isEqualTo("https://public/notify");
        assertThat(config.isSandbox()).isTrue();
        assertThat(config.getWebhookSecret()).isEqualTo("whsec_abc");
    }

    @Test
    void sandboxFalse_IsExposed() {
        PaymentConfig config = new PaymentConfig("t", "s", "f", "p", "n", false, "");
        assertThat(config.isSandbox()).isFalse();
        assertThat(config.getWebhookSecret()).isEmpty();
    }

    @Test
    void restTemplate_ReturnsNonNullBean() {
        assertThat(config().restTemplate()).isNotNull();
    }
}
