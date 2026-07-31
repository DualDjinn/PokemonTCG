// Paquete donde se agrupan las clases de configuración de Spring
package ar.edu.utn.frc.tup.piii.configs;

import java.util.Properties; // Clase de Java para manejar pares clave-valor de configuración
import org.springframework.beans.factory.annotation.Value; // Inyecta valores desde application.properties
import org.springframework.context.annotation.Bean; // Marca un método como productor de un bean de Spring
import org.springframework.context.annotation.Configuration; // Indica que la clase contiene configuración de Spring
import org.springframework.context.annotation.Primary; // Prioriza este bean si hay varios del mismo tipo
import org.springframework.mail.javamail.JavaMailSender; // Interfaz de Spring para enviar emails
import org.springframework.mail.javamail.JavaMailSenderImpl; // Implementación concreta de JavaMailSender

// Clase de configuración que construye y registra el bean de envío de emails
@Configuration
public class MailSenderConfig {

    @Value("${MAIL_HOST:smtp.gmail.com}")
    private String host;

    @Value("${MAIL_PORT:587}")
    private int port;

    @Value("${MAIL_USERNAME:pokeprogra@gmail.com}")
    private String username;

    @Value("${MAIL_PASSWORD:zwzo nxpe quek twca}")
    private String password;

    // Registra el JavaMailSender como bean disponible en todo el contexto de Spring
    @Bean
    // Este bean tiene preferencia si existe más de un JavaMailSender registrado
    @Primary
    public JavaMailSender javaMailSender() {
        // Crea la implementación del enviador de emails de Spring
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        // Establece el servidor SMTP al que se conectará para enviar emails
        mailSender.setHost(host);
        // Establece el puerto del servidor SMTP
        mailSender.setPort(port);
        // Establece la cuenta de email desde la cual se enviarán los correos
        mailSender.setUsername(username);
        // Establece la contraseña de autenticación para el servidor SMTP
        mailSender.setPassword(password);
        // UTF-8 permite caracteres especiales (tildes, ñ, etc.) en el cuerpo del email
        mailSender.setDefaultEncoding("UTF-8");

        // Obtiene el mapa de propiedades extendidas de JavaMail para configurar SMTP
        Properties props = mailSender.getJavaMailProperties();
        // Define SMTP como protocolo de transporte para el envío de correos
        props.put("mail.transport.protocol", "smtp");
        // Habilita autenticación SMTP con usuario y contraseña
        props.put("mail.smtp.auth", "true");
        // Habilita STARTTLS para cifrar la comunicación con el servidor
        props.put("mail.smtp.starttls.enable", "true");
        // Fuerza el uso de STARTTLS; si el servidor no lo soporta, la conexión falla
        props.put("mail.smtp.starttls.required", "true");
        // Tiempo máximo de espera para establecer la conexión inicial: 5 segundos
        props.put("mail.smtp.connectiontimeout", "5000");
        // Tiempo máximo de espera para recibir respuesta del servidor: 5 segundos
        props.put("mail.smtp.timeout", "5000");
        // Tiempo máximo de espera para escribir datos al servidor: 5 segundos
        props.put("mail.smtp.writetimeout", "5000");
        // Confia en el certificado SSL de Gmail para evitar fallos de verificación en ciertos entornos JVM
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        // Devuelve el bean completamente configurado para ser usado por EmailService
        return mailSender;
    }
}
