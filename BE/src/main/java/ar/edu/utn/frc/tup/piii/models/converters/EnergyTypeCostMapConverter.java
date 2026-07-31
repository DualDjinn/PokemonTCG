package ar.edu.utn.frc.tup.piii.models.converters;

import ar.edu.utn.frc.tup.piii.models.EnergyType;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.Converter;
import java.util.Map;

@Converter
public class EnergyTypeCostMapConverter extends JsonConverter<Map<EnergyType, Integer>> {

    public EnergyTypeCostMapConverter() {
        super(new TypeReference<>() { }, "{}");
    }
}
