package ar.edu.utn.frc.tup.piii.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;

class EmailServiceTest {

    private JavaMailSender mailSender;
    private EmailService service;
    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() throws Exception {
        mailSender = mock(JavaMailSender.class);
        mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        service = new EmailService(mailSender);

        setField(service, "fromAddress", "test@pokemon.com");
        setField(service, "frontendUrl", "http://localhost:4200");
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void sendPasswordResetEmail_SendsEmail() {
        service.sendPasswordResetEmail("player@test.com", "reset-token-123");

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendPasswordResetEmail_CalledMultipleTimes_SendsEachTime() {
        service.sendPasswordResetEmail("a@test.com", "token1");
        service.sendPasswordResetEmail("b@test.com", "token2");

        verify(mailSender, org.mockito.Mockito.times(2)).send(any(MimeMessage.class));
    }

    @Test
    void sendPasswordResetEmail_MessagingException_ThrowsRuntime() throws Exception {
        doThrow(new RuntimeException("Error al enviar el email de recuperación"))
                .when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> service.sendPasswordResetEmail("fail@test.com", "token"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error al enviar");
    }

    @Test
    void sendPasswordResetEmail_HelperThrowsMessagingException_WrapsInRuntime() throws Exception {
        // Force the MimeMessageHelper.setFrom(...) call to fail with a checked MessagingException
        // so the real catch block is exercised (not just a RuntimeException propagating through).
        doThrow(new MessagingException("smtp config broken"))
                .when(mimeMessage).setFrom(any(Address.class));

        assertThatThrownBy(() -> service.sendPasswordResetEmail("fail@test.com", "token"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error al enviar el email de recuperación")
                .hasCauseInstanceOf(MessagingException.class);

        verify(mailSender, org.mockito.Mockito.never()).send(any(MimeMessage.class));
    }
}
