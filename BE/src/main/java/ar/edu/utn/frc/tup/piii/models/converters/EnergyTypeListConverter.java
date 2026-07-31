package ar.edu.utn.frc.tup.piii.models.converters;

import ar.edu.utn.frc.tup.piii.models.EnergyType;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.Converter;
import java.util.List;

@Converter
public class EnergyTypeListConverter extends JsonConverter<List<EnergyType>> {

    public EnergyTypeListConverter() {
        super(new TypeReference<>() { }, "[]");
    }
}
