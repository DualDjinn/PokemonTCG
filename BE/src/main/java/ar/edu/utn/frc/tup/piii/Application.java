package ar.edu.utn.frc.tup.piii;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main class.
 */
@SpringBootApplication
@EnableScheduling
@EnableRetry
@ConfigurationPropertiesScan
public class Application {

    /**
     * Main program.
     * @param args application args
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
