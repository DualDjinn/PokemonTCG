package ar.edu.utn.frc.tup.piii.seeders;

import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.SetBucketPolicyArgs;
import io.minio.StatObjectArgs;

/**
 * Sube a MinIO todas las imágenes incluidas bajo {@code resources/pokesecret/}.
 *
 * <p>Cada subcarpeta de primer nivel se convierte automáticamente en un bucket
 * con su mismo nombre (creado con política public-read si no existe) y los
 * archivos se suben preservando la ruta relativa dentro de la carpeta. Para
 * publicar un set nuevo de imágenes alcanza con agregar la carpeta — este
 * seeder no necesita cambios.
 *
 * <p>Los nombres de carpeta deben cumplir las reglas de bucket de S3:
 * minúsculas, dígitos, puntos y guiones, entre 3 y 63 caracteres. Las
 * carpetas que no cumplan se saltean con un warning.
 */
@Component
public class MinioCardImageSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MinioCardImageSeeder.class);
    private static final String ROOT_PATTERN = "classpath:pokesecret/**/*";
    private static final String ROOT_PREFIX = "pokesecret/";
    private static final Pattern VALID_BUCKET_NAME = Pattern.compile("[a-z0-9][a-z0-9.-]{2,62}");

    private final MinioClient minioClient;

    public MinioCardImageSeeder(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    @Override
    public void run(ApplicationArguments args) {
        Map<String, List<ImageEntry>> imagesByFolder;
        try {
            imagesByFolder = scanImagesByFolder();
        } catch (Exception e) {
            log.warn("Failed to scan pokesecret resources for MinIO seeding: {}", e.getMessage());
            return;
        }

        for (Map.Entry<String, List<ImageEntry>> entry : imagesByFolder.entrySet()) {
            String bucket = entry.getKey();
            if (!VALID_BUCKET_NAME.matcher(bucket).matches()) {
                log.warn("Skipping pokesecret folder '{}': not a valid S3 bucket name "
                        + "(use lowercase letters, digits and hyphens).", bucket);
                continue;
            }
            try {
                ensureBucketExists(bucket);
                uploadImages(bucket, entry.getValue());
            } catch (Exception e) {
                log.warn("Failed to seed MinIO bucket '{}': {}", bucket, e.getMessage());
            }
        }
    }

    /** Recurso a subir junto con su object key relativo a la carpeta/bucket. */
    private record ImageEntry(Resource resource, String objectKey) { }

    /**
     * Agrupa todas las imágenes bajo pokesecret/ por su subcarpeta de primer
     * nivel (= bucket destino). Archivos sueltos en la raíz se ignoran.
     */
    private Map<String, List<ImageEntry>> scanImagesByFolder() throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources(ROOT_PATTERN);
        Map<String, List<ImageEntry>> byFolder = new LinkedHashMap<>();
        for (Resource resource : resources) {
            String filename = resource.getFilename();
            if (filename == null || !isImage(filename)) {
                continue;
            }
            String relativePath = extractRelativePath(resource);
            int firstSlash = relativePath.indexOf('/');
            if (firstSlash <= 0) {
                continue;
            }
            String folder = relativePath.substring(0, firstSlash);
            String objectKey = relativePath.substring(firstSlash + 1);
            byFolder.computeIfAbsent(folder, k -> new ArrayList<>())
                    .add(new ImageEntry(resource, objectKey));
        }
        return byFolder;
    }

    /** Ruta del recurso relativa a {@code pokesecret/}, ej. "back-card/backCardBio.png". */
    private String extractRelativePath(Resource resource) throws Exception {
        String uri = resource.getURI().toString();
        int idx = uri.indexOf(ROOT_PREFIX);
        if (idx < 0) {
            return resource.getFilename();
        }
        return uri.substring(idx + ROOT_PREFIX.length());
    }

    private void ensureBucketExists(String targetBucket) throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(targetBucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(targetBucket).build());
            log.info("Created MinIO bucket '{}'.", targetBucket);
        }
        applyPublicReadPolicy(targetBucket);
    }

    private void applyPublicReadPolicy(String targetBucket) {
        String policy = "{"
                + "\"Version\":\"2012-10-17\","
                + "\"Statement\":[{"
                + "\"Effect\":\"Allow\","
                + "\"Principal\":{\"AWS\":[\"*\"]},"
                + "\"Action\":[\"s3:GetObject\"],"
                + "\"Resource\":[\"arn:aws:s3:::" + targetBucket + "/*\"]"
                + "}]"
                + "}";
        try {
            minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                    .bucket(targetBucket)
                    .config(policy)
                    .build());
            log.info("Applied public-read policy to MinIO bucket '{}'.", targetBucket);
        } catch (Exception e) {
            log.warn("Failed to apply public-read policy to bucket '{}': {}",
                    targetBucket, e.getMessage());
        }
    }

    private void uploadImages(String targetBucket, List<ImageEntry> images) {
        int uploaded = 0;
        int skipped = 0;
        for (ImageEntry entry : images) {
            if (objectExists(targetBucket, entry.objectKey())) {
                skipped++;
                continue;
            }
            try (InputStream is = entry.resource().getInputStream()) {
                long size = entry.resource().contentLength();
                String contentType = URLConnection.guessContentTypeFromName(entry.objectKey());
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(targetBucket)
                        .object(entry.objectKey())
                        .stream(is, size, -1)
                        .contentType(contentType)
                        .build());
                uploaded++;
            } catch (Exception e) {
                log.warn("Failed to upload {} to MinIO bucket '{}': {}",
                        entry.objectKey(), targetBucket, e.getMessage());
            }
        }
        log.info("MinIO seeding to '{}' done. Uploaded: {}, already present: {}.",
                targetBucket, uploaded, skipped);
    }

    private boolean isImage(String filename) {
        String lower = filename.toLowerCase();
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg");
    }

    private boolean objectExists(String targetBucket, String objectName) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(targetBucket).object(objectName).build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
