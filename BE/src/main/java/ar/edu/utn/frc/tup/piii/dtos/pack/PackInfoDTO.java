package ar.edu.utn.frc.tup.piii.dtos.pack;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Info pública de un tipo de sobre, expuesta a la UI para construir la tienda.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PackInfoDTO {
    private String type;       // "BRONZE" | "SILVER" | "GOLD"
    private int price;
    private int cardsPerPack;
    /** Probabilidades por bucket: claves "HIGH", "MEDIUM", "LOW". */
    private Map<String, Double> probabilities;
}
