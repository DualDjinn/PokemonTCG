package ar.edu.utn.frc.tup.piii.models;

// Representa los tipos de energía que pueden tener las cartas Pokémon.
public enum EnergyType {
    GRASS("Grass"),
    FIRE("Fire"),
    WATER("Water"),
    LIGHTNING("Lightning"),
    PSYCHIC("Psychic"),
    FIGHTING("Fighting"),
    DARKNESS("Darkness"),
    METAL("Metal"),
    FAIRY("Fairy"),
    DRAGON("Dragon"),
    COLORLESS("Colorless");
    
    private final String jsonValue;
    
    EnergyType(String jsonValue) {
        this.jsonValue = jsonValue;
    }
    
    // Permite convertir un string del JSON a un EnergyType.
    public static EnergyType fromJson(String value) {
        for (EnergyType type : values()) {
            if (type.jsonValue.equals(value)) return type;
        }
        throw new IllegalArgumentException("Tipo desconocido: " + value);
    }
}