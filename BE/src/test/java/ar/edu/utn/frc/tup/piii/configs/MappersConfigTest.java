package ar.edu.utn.frc.tup.piii.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class MappersConfigTest {

    private MappersConfig config;

    @BeforeEach
    void setUp() {
        config = new MappersConfig();
    }

    @Test
    void modelMapper_ReturnsUsableInstance() {
        ModelMapper mapper = config.modelMapper();

        assertThat(mapper).isNotNull();
        Sample copy = mapper.map(new Sample("Ash", 18), Sample.class);
        assertThat(copy.name).isEqualTo("Ash");
        assertThat(copy.age).isEqualTo(18);
    }

    @Test
    void mergerMapper_SkipsNullFieldsOnUpdate() {
        ModelMapper merger = config.mergerMapper();

        Sample existing = new Sample("Ash", 18);
        Sample patch = new Sample(null, 19);
        merger.map(patch, existing);

        assertThat(existing.name)
                .as("mergerMapper must skip null source fields so partial updates don't wipe data")
                .isEqualTo("Ash");
        assertThat(existing.age).isEqualTo(19);
    }

    @Test
    void mergerMapper_AllNullsLeavesTargetUntouched() {
        ModelMapper merger = config.mergerMapper();

        Sample existing = new Sample("Ash", 18);
        merger.map(new Sample(null, null), existing);

        assertThat(existing.name).isEqualTo("Ash");
        assertThat(existing.age).isEqualTo(18);
    }

    @Test
    void mergerMapper_NoNullsBehavesLikeStandardMapping() {
        ModelMapper merger = config.mergerMapper();

        Sample existing = new Sample("Ash", 18);
        merger.map(new Sample("Misty", 17), existing);

        assertThat(existing.name).isEqualTo("Misty");
        assertThat(existing.age).isEqualTo(17);
    }

    @Test
    void objectMapper_SerializesLocalDateTimeViaJavaTimeModule() {
        ObjectMapper mapper = config.objectMapper();

        assertThatCode(() -> mapper.writeValueAsString(LocalDateTime.of(2026, 6, 11, 12, 0)))
                .as("objectMapper must register JavaTimeModule so LocalDateTime serializes")
                .doesNotThrowAnyException();
    }

    @Test
    void objectMapper_RoundTripsLocalDateTime() throws Exception {
        ObjectMapper mapper = config.objectMapper();
        LocalDateTime original = LocalDateTime.of(2026, 6, 11, 12, 0);

        String json = mapper.writeValueAsString(original);
        LocalDateTime back = mapper.readValue(json, LocalDateTime.class);

        assertThat(back).isEqualTo(original);
    }

    @Test
    void objectMapper_RoundTripsLocalDate() throws Exception {
        ObjectMapper mapper = config.objectMapper();
        LocalDate original = LocalDate.of(2026, 6, 11);

        String json = mapper.writeValueAsString(original);
        LocalDate back = mapper.readValue(json, LocalDate.class);

        assertThat(back).isEqualTo(original);
    }

    @Test
    void objectMapper_RoundTripsInstant() throws Exception {
        ObjectMapper mapper = config.objectMapper();
        Instant original = Instant.parse("2026-06-11T12:00:00Z");

        String json = mapper.writeValueAsString(original);
        Instant back = mapper.readValue(json, Instant.class);

        assertThat(back).isEqualTo(original);
    }

    @Test
    void objectMapper_RoundTripsZonedDateTime() throws Exception {
        ObjectMapper mapper = config.objectMapper();
        ZonedDateTime original = ZonedDateTime.of(2026, 6, 11, 12, 0, 0, 0, ZoneOffset.UTC);

        String json = mapper.writeValueAsString(original);
        ZonedDateTime back = mapper.readValue(json, ZonedDateTime.class);

        assertThat(back).isEqualTo(original);
    }

    @Test
    void objectMapper_SerializesNullAsJsonNull() throws Exception {
        ObjectMapper mapper = config.objectMapper();

        assertThat(mapper.writeValueAsString(null)).isEqualTo("null");
    }

    @Test
    void mergerMapper_NonNullSourceListIsMergedIntoTargetList() {
        ModelMapper merger = config.mergerMapper();

        WithList existing = new WithList(new java.util.ArrayList<>(java.util.List.of("a", "b")));
        merger.map(new WithList(java.util.List.of("c")), existing);

        assertThat(existing.items)
                .as("ModelMapper merges source list elements into the existing target list "
                  + "(append/union semantics); callers doing partial updates with lists must "
                  + "clear the target first if they want replacement")
                .contains("c");
    }

    @Test
    void mergerMapper_NullSourceListPreservesTargetList() {
        ModelMapper merger = config.mergerMapper();

        WithList existing = new WithList(java.util.List.of("a", "b"));
        merger.map(new WithList(null), existing);

        assertThat(existing.items)
                .as("null list source must be skipped by isNotNull condition")
                .containsExactly("a", "b");
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class Sample {
        private String name;
        private Integer age;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class WithList {
        private java.util.List<String> items;
    }
}
