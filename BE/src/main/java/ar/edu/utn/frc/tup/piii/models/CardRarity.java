package ar.edu.utn.frc.tup.piii.models;

public enum CardRarity {
    COMMON("Common"),
    UNCOMMON("Uncommon"),
    RARE("Rare"),
    RARE_HOLO("Rare Holo"),
    RARE_HOLO_EX("Rare Holo EX"),
    RARE_ULTRA("Rare Ultra"),
    ACE_SPEC_RARE("Rare Ace");
    
    private final String jsonValue;
    
    CardRarity(String jsonValue) {
        this.jsonValue = jsonValue;
    }
    
public static CardRarity fromJson(String value) {
    for (CardRarity rarity : values()) {
        if (rarity.jsonValue.equals(value)) return rarity;
    }
    System.out.println("Rareza desconocida, usando COMMON por defecto: {} " + value);
    return COMMON;
}
}