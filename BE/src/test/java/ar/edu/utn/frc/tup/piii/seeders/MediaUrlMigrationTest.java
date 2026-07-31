package ar.edu.utn.frc.tup.piii.seeders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class MediaUrlMigrationTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private final String minioUrl = "http://localhost:9000";

    private MediaUrlMigration migration;

    @BeforeEach
    void setUp() {
        migration = new MediaUrlMigration(jdbcTemplate, minioUrl);
    }

    @Test
    void run_UpdatesAll5Statements() throws Exception {
        when(jdbcTemplate.update(anyString(), anyString(), anyString())).thenReturn(1);

        migration.run(new DefaultApplicationArguments());

        String prefix = "http://localhost:9000/";
        String like = "http://localhost:9000/%";
        verify(jdbcTemplate, times(5)).update(anyString(), eq(prefix), eq(like));
    }

    @Test
    void run_NoRowsUpdated_DoesNotFail() throws Exception {
        when(jdbcTemplate.update(anyString(), anyString(), anyString())).thenReturn(0);

        migration.run(new DefaultApplicationArguments());

        verify(jdbcTemplate, times(5)).update(anyString(), anyString(), anyString());
    }

    @Test
    void run_StatementFails_ContinuesToNext() throws Exception {
        when(jdbcTemplate.update(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("DB error"))
                .thenReturn(1);

        migration.run(new DefaultApplicationArguments());

        verify(jdbcTemplate, times(5)).update(anyString(), anyString(), anyString());
    }
}
