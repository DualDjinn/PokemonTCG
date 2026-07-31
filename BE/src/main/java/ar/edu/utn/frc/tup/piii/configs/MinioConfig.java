package ar.edu.utn.frc.tup.piii.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.minio.MinioClient;

@Configuration
public class MinioConfig {

    @Value("${APP_MINIO_URL:http://localhost:9000}")
    private String minioUrl;

    @Value("${APP_MINIO_ACCESS_KEY:admin}")
    private String accessKey;

    @Value("${APP_MINIO_SECRET_KEY:admin123456}")
    private String secretKey;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(minioUrl)
                .credentials(accessKey, secretKey)
                .build();
    }
}
