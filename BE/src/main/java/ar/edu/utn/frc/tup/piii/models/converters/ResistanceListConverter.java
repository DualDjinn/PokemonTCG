package ar.edu.utn.frc.tup.piii.models.converters;

import ar.edu.utn.frc.tup.piii.models.Resistance;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.Converter;
import java.util.List;

@Converter
public class ResistanceListConverter extends JsonConverter<List<Resistance>> {

    public ResistanceListConverter() {
        super(new TypeReference<>() { }, "[]");
    }
}
