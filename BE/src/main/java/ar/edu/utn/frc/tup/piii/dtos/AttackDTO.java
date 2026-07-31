package ar.edu.utn.frc.tup.piii.dtos;

import java.util.List;

import lombok.Getter;

@Getter
public class AttackDTO {
    private String name;
    private List<String> cost;                      // ["Grass", "Colorless", "Colorless"]
    private Integer convertedEnergyCost;
    private String damage;                          // "60" o "30+" o "20×"
    private String text;
}
