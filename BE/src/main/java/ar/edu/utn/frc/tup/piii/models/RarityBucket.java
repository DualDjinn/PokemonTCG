package ar.edu.utn.frc.tup.piii.models;

/**
 * Bucket de rareza usado por la tienda de sobres para definir probabilidades de aparición.
 *
 * <p>Mapea las múltiples rarezas del set xy1 de pokemontcg.io a tres categorías agregadas:
 * <ul>
 *   <li>{@link #HIGH} — cartas frecuentes (Common)</li>
 *   <li>{@link #MEDIUM} — cartas intermedias (Uncommon)</li>
 *   <li>{@link #LOW} — cartas raras (Rare, Rare Holo, Rare Holo EX, Rare Ultra, Ace Spec)</li>
 * </ul>
 *
 * <p>Nota terminológica: HIGH = "probabilidad alta de salir", o sea, las más comunes.
 */
public enum RarityBucket {
    HIGH,
    MEDIUM,
    LOW;

    /**
     * Convierte una {@link CardRarity} al bucket correspondiente.
     *
     * @param rarity rareza original de la carta (puede ser null)
     * @return bucket asociado; {@link #HIGH} por defecto si la rareza es null
     */
    public static RarityBucket fromRarity(CardRarity rarity) {
        if (rarity == null) {
            return HIGH;
        }
        switch (rarity) {
            case COMMON:
                return HIGH;
            case UNCOMMON:
                return MEDIUM;
            case RARE:
            case RARE_HOLO:
            case RARE_HOLO_EX:
            case ACE_SPEC_RARE:
                return LOW;
            default:
                return HIGH;
        }
    }
}
