package ar.edu.utn.frc.tup.piii.seeders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

@ExtendWith(MockitoExtension.class)
class MinioCardImageSeederTest {

    @Mock
    private MinioClient minioClient;

    private MinioCardImageSeeder seeder;

    @BeforeEach
    void setUp() {
        seeder = new MinioCardImageSeeder(minioClient);
    }

    @Test
    void run_DoesNotCrash() throws Exception {
        // scanImagesByFolder may find pokesecret resources or not;
        // the seeder must not crash in either case.
        seeder.run(new DefaultApplicationArguments());
    }
}
