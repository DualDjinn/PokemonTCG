package ar.edu.utn.frc.tup.piii.dtos;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CardApiDTO {
    private String id;                              // "xy1-1"
    private String name;                            // "Venusaur-EX"
    private String supertype;                       // "Pokémon" | "Trainer" | "Energy"
    private List<String> subtypes;                  // ["Basic", "EX"]
    private String hp;                              // "180" (puede ser null)
    private List<String> types;                     // ["Grass"] (solo Pokémon)
    private List<String> evolvesTo;                 // ["M Venusaur-EX"]
    private String evolvesFrom;                     // null en Básicos
    private List<String> rules;                     // textos descriptivos
    private List<AttackDTO> attacks;
    private List<AbilityDTO> abilities;
    private List<WeaknessDTO> weaknesses;
    private List<ResistanceDTO> resistances;
    private List<String> retreatCost;
    private Integer convertedRetreatCost;
    private SetDTO set;
    private String number;
    private String artist;
    private String rarity;
    private LegalitiesDTO legalities;
    private ImagesDTO images;
}