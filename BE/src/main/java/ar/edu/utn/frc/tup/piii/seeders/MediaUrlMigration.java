package ar.edu.utn.frc.tup.piii.seeders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Reescribe las URLs de imagen absolutas a MinIO
 * ({@code http://localhost:9000/<bucket>/...}) por rutas relativas
 * ({@code /media/<bucket>/...}) en las filas ya persistidas.
 *
 * <p>Las URLs absolutas no cargan al correr la app por un devtunnel HTTPS
 * (mixed content + bloqueo de loopback + CORS). Las relativas se sirven desde el
 * backend (ver {@code WebConfig}) por el mismo origen. Corre después de todos los
 * seeders y es idempotente: tras la primera corrida el {@code LIKE} no matchea
 * nada y no toca las URLs externas de pokemontcg.io.
 */
@Component
@Order(10)
public class MediaUrlMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MediaUrlMigration.class);

    private static final String[] STATEMENTS = {
        "UPDATE card SET image_url = REPLACE(image_url, ?, '/media/') WHERE image_url LIKE ?",
        "UPDATE card SET image_url_large = REPLACE(image_url_large, ?, '/media/') "
            + "WHERE image_url_large LIKE ?",
        "UPDATE card_skin SET image_url = REPLACE(image_url, ?, '/media/') WHERE image_url LIKE ?",
        "UPDATE card_skin SET image_url_large = REPLACE(image_url_large, ?, '/media/') "
            + "WHERE image_url_large LIKE ?",
        "UPDATE card_back SET image_url = REPLACE(image_url, ?, '/media/') WHERE image_url LIKE ?"
    };

    private final JdbcTemplate jdbcTemplate;
    private final String minioUrl;

    public MediaUrlMigration(JdbcTemplate jdbcTemplate,
                             @Value("${app.minio.url}") String minioUrl) {
        this.jdbcTemplate = jdbcTemplate;
        this.minioUrl = minioUrl;
    }

    @Override
    public void run(ApplicationArguments args) {
        String prefix = minioUrl + "/";
        String like = prefix + "%";
        int total = 0;
        for (String sql : STATEMENTS) {
            try {
                total += jdbcTemplate.update(sql, prefix, like);
            } catch (Exception e) {
                log.warn("Failed to migrate image URLs ({}): {}", sql, e.getMessage());
            }
        }
        if (total > 0) {
            log.info("Rewrote {} MinIO image URL(s) to /media/ relative paths.", total);
        }
    }
}
