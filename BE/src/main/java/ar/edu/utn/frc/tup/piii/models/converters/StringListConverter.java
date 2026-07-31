package ar.edu.utn.frc.tup.piii.models.converters;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.Converter;
import java.util.List;

@Converter
public class StringListConverter extends JsonConverter<List<String>> {

    public StringListConverter() {
        super(new TypeReference<>() { }, "[]");
    }
}
