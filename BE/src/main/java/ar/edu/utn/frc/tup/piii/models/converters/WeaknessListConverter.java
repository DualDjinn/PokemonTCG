package ar.edu.utn.frc.tup.piii.models.converters;

import ar.edu.utn.frc.tup.piii.models.Weakness;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.Converter;
import java.util.List;

@Converter
public class WeaknessListConverter extends JsonConverter<List<Weakness>> {

    public WeaknessListConverter() {
        super(new TypeReference<>() { }, "[]");
    }
}
