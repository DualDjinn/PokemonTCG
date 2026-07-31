package ar.edu.utn.frc.tup.piii.models.converters;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonConverterTest {

    private JsonConverter<List<String>> converter;

    @BeforeEach
    void setUp() {
        converter = new JsonConverter<>(new TypeReference<List<String>>() {}, "[]") {};
    }

    @Test
    void convertToDatabaseColumn_Null_ReturnsEmptyJson() {
        assertThat(converter.convertToDatabaseColumn(null)).isEqualTo("[]");
    }

    @Test
    void convertToDatabaseColumn_ValidList_ReturnsJsonArray() {
        assertThat(converter.convertToDatabaseColumn(List.of("a", "b"))).isEqualTo("[\"a\",\"b\"]");
    }

    @Test
    void convertToEntityAttribute_Null_ReturnsDeserializedEmptyJson() {
        assertThat(converter.convertToEntityAttribute(null)).isEmpty();
    }

    @Test
    void convertToEntityAttribute_Blank_ReturnsDeserializedEmptyJson() {
        assertThat(converter.convertToEntityAttribute("   ")).isEmpty();
    }

    @Test
    void convertToEntityAttribute_ValidJson_ReturnsDeserializedList() {
        assertThat(converter.convertToEntityAttribute("[\"x\",\"y\"]")).containsExactly("x", "y");
    }

    @Test
    void convertToEntityAttribute_InvalidJson_ThrowsIllegalArgument() {
        assertThatThrownBy(() -> converter.convertToEntityAttribute("not json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Error deserializing JSON");
    }

    @Test
    void convertToDatabaseColumn_WhenSerializationFails_ThrowsIllegalArgument() {
        // Un Object "vacío" dispara FAIL_ON_EMPTY_BEANS -> JsonProcessingException,
        // cubriendo el catch de convertToDatabaseColumn.
        JsonConverter<Object> objectConverter =
                new JsonConverter<Object>(new TypeReference<Object>() {}, "{}") {};
        assertThatThrownBy(() -> objectConverter.convertToDatabaseColumn(new Object()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Error serializing to JSON");
    }
}
