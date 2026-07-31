package ar.edu.utn.frc.tup.piii.models.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;

public abstract class JsonConverter<T> implements AttributeConverter<T, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final TypeReference<T> typeReference;
    private final String emptyJson;

    protected JsonConverter(TypeReference<T> typeReference, String emptyJson) {
        this.typeReference = typeReference;
        this.emptyJson = emptyJson;
    }

    @Override
    public String convertToDatabaseColumn(T attribute) {
        if (attribute == null) {
            return emptyJson;
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error serializing to JSON", e);
        }
    }

    @Override
    public T convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return deserialize(emptyJson);
        }
        return deserialize(dbData);
    }

    private T deserialize(String json) {
        try {
            return MAPPER.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error deserializing JSON", e);
        }
    }
}
