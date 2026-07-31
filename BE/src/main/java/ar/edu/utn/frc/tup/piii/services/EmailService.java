// Paquete donde se agrupan los servicios de la aplicación
package ar.edu.utn.frc.tup.piii.services;

import jakarta.mail.MessagingException; // Excepción que lanza JavaMail si falla la construcción o el envío
import jakarta.mail.internet.MimeMessage; // Representa un mensaje de email con soporte HTML y adjuntos
import org.slf4j.Logger; // Interfaz del logger para registrar eventos en la aplicación
import org.slf4j.LoggerFactory; // Fábrica estática que crea instancias de Logger
import org.springframework.beans.factory.annotation.Value; // Inyecta valores desde application.properties
import org.springframework.mail.javamail.JavaMailSender; // Interfaz de Spring para enviar emails
import org.springframework.mail.javamail.MimeMessageHelper; // Simplifica la configuración de un MimeMessage
import org.springframework.stereotype.Service; // Marca la clase como bean de servicio de Spring

// Servicio encargado de construir y enviar emails de recuperación de contraseña
@Service
public class EmailService {

    // Logger para registrar intentos de envío, éxitos y errores
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    // Dependencia inyectada por constructor para enviar los correos electrónicos
    private final JavaMailSender mailSender;

    // Dirección de email desde la cual se envían los correos, leída de application.properties
    @Value("${MAIL_USERNAME:pokeprogra@gmail.com}")
    private String fromAddress;

    // URL base del frontend para armar el enlace de restablecimiento de contraseña
    @Value("${app.frontend-url}")
    private String frontendUrl;

    // Constructor con inyección del JavaMailSender configurado en MailSenderConfig
    public EmailService(JavaMailSender mailSender) {
        // Asigna el enviador de emails al campo del servicio
        this.mailSender = mailSender;
    }

    // Construye y envía el email con el enlace de recuperación al destinatario indicado
    public void sendPasswordResetEmail(String to, String token) {
        // Arma el enlace completo con el token como parámetro de query string
        String resetLink = frontendUrl + "/reset-password?token=" + token;
        // Registra en el log que se va a intentar enviar el email
        logger.info("Sending password reset email to {} via {}", to, fromAddress);

        // Crea un mensaje MIME vacío a partir del mailSender configurado
        MimeMessage message = mailSender.createMimeMessage();
        try {
            // MimeMessageHelper facilita configurar remitente, destinatario, asunto y cuerpo HTML
            // true = multipart (necesario para adjuntos), "UTF-8" = codificación de caracteres
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            // Establece la dirección del remitente del email
            helper.setFrom(fromAddress);
            // Establece la dirección del destinatario del email
            helper.setTo(to);
            // Establece el asunto del email que verá el jugador en su bandeja de entrada
            helper.setSubject("Recuperación de contraseña - Pokémon TCG");
            // Establece el cuerpo HTML del email; el segundo parámetro true indica que es HTML
            helper.setText(buildEmailBody(resetLink), true);
            // Envía el email a través del servidor SMTP configurado en MailSenderConfig
            mailSender.send(message);
            // Registra en el log que el envío fue exitoso
            logger.info("Password reset email sent successfully to {}", to);
        } catch (MessagingException e) {
            // Registra el error completo con el mensaje de la excepción para facilitar el diagnóstico
            logger.error("Failed to send password reset email to {}: {}", to, e.getMessage(), e);
            // Propaga el error como RuntimeException para que el servicio superior lo maneje
            throw new RuntimeException("Error al enviar el email de recuperación", e);
        }
    }

    // Genera el HTML del cuerpo del email con estilo oscuro temático de Pokémon
    private String buildEmailBody(String resetLink) {
        return "<div style='font-family:Arial,sans-serif;max-width:480px;margin:auto;padding:32px;" // Contenedor principal: fuente, ancho máximo, centrado y padding
            + "background:#0a0a1a;color:#e0e0e0;border-radius:12px;'>" // Fondo oscuro, texto claro y esquinas redondeadas
            + "<h2 style='color:#00aaff;letter-spacing:2px;'>POKÉMON TCG</h2>" // Título del juego en azul brillante
            + "<p>Recibimos una solicitud para restablecer tu contraseña.</p>" // Párrafo informando el motivo del email
            + "<p>Hacé clic en el botón para crear una nueva:</p>" // Instrucción para el usuario
            + "<a href='" + resetLink + "' " // Enlace que apunta al endpoint de restablecimiento con el token
            + "style='display:inline-block;margin:20px 0;padding:14px 28px;" // Botón: bloque en línea con margen y relleno interno
            + "background:linear-gradient(135deg,#00aaff,#ff3333);color:#fff;" // Degradado azul→rojo como fondo y texto blanco
            + "text-decoration:none;border-radius:6px;font-weight:bold;" // Sin subrayado, bordes redondeados y texto en negrita
            + "letter-spacing:1px;'>RESTABLECER CONTRASEÑA</a>" // Espaciado entre letras y texto visible del botón
            + "<p style='font-size:12px;color:#888;'>Este enlace expira en <strong>1 hora</strong>.</p>" // Aviso de expiración del token en 1 hora
            + "<p style='font-size:12px;color:#888;'>Si no solicitaste este cambio, ignorá este mensaje.</p>" // Mensaje de seguridad para evitar alarmas innecesarias
            + "</div>"; // Cierre del contenedor principal del email
    }
}
