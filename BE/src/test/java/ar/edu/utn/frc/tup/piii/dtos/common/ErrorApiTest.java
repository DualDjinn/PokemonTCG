package ar.edu.utn.frc.tup.piii.dtos.common;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ErrorApiTest {

    @Test
    void testBuilderAndGetters() {
        ErrorApi error = ErrorApi.builder()
                .timestamp("2024-01-01T00:00:00")
                .status(400)
                .error("Bad Request")
                .message("Invalid input")
                .build();

        assertThat(error.getTimestamp()).isEqualTo("2024-01-01T00:00:00");
        assertThat(error.getStatus()).isEqualTo(400);
        assertThat(error.getError()).isEqualTo("Bad Request");
        assertThat(error.getMessage()).isEqualTo("Invalid input");
    }

    @Test
    void testEqualsAndHashCode_SameValues() {
        ErrorApi e1 = ErrorApi.builder()
                .timestamp("2024-01-01T00:00:00")
                .status(400)
                .error("Bad Request")
                .message("Invalid")
                .build();

        ErrorApi e2 = ErrorApi.builder()
                .timestamp("2024-01-01T00:00:00")
                .status(400)
                .error("Bad Request")
                .message("Invalid")
                .build();

        assertThat(e1).isEqualTo(e2);
        assertThat(e1.hashCode()).isEqualTo(e2.hashCode());
    }

    @Test
    void testEquals_DifferentValues() {
        ErrorApi e1 = ErrorApi.builder().status(400).build();
        ErrorApi e2 = ErrorApi.builder().status(500).build();

        assertThat(e1).isNotEqualTo(e2);
    }

    @Test
    void testEquals_NullObject() {
        ErrorApi e = ErrorApi.builder().build();
        assertThat(e.equals(null)).isFalse();
    }

    @Test
    void testNoArgsConstructor() {
        assertThat(new ErrorApi()).isNotNull();
    }

    @Test
    void testAllArgsConstructor() {
        ErrorApi e = new ErrorApi("ts", 200, "OK", "done");
        assertThat(e.getStatus()).isEqualTo(200);
        assertThat(e.getError()).isEqualTo("OK");
    }

    @Test
    void testBuilder_Empty() {
        ErrorApi e = ErrorApi.builder().build();
        assertThat(e).isNotNull();
    }

    @Test
    void testEquals_Reflexivity() {
        ErrorApi e = ErrorApi.builder().timestamp("ts").status(200).error("OK").message("done").build();
        assertThat(e).isEqualTo(e);
    }

    @Test
    void testEquals_DifferentType_ReturnsFalse() {
        ErrorApi e = ErrorApi.builder().build();
        assertThat(e).isNotEqualTo("some string");
    }

    @Test
    void testToString_ContainsFields() {
        ErrorApi e = new ErrorApi("ts", 200, "OK", "done");
        assertThat(e.toString()).contains("ts", "200", "OK", "done");
    }
}
