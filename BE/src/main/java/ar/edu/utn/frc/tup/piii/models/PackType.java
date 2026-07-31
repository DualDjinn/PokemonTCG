package ar.edu.utn.frc.tup.piii.models;

import java.util.EnumMap;
import java.util.Map;

/**
 * Tipos de sobres comprables en la tienda de Blanca.
 *
 * <p>Cada sobre entrega 3 cartas (constante {@link #CARDS_PER_PACK}) y tiene
 * asociado un precio en monedas y una distribución de probabilidad por bucket
 * de rareza.
 *
 * <p>Las probabilidades de cada sobre suman 1.0 (100%) y siguen la lógica:
 * <ul>
 *   <li><b>BRONZE</b>: muy sesgado a cartas comunes.</li>
 *   <li><b>SILVER</b>: balance medio.</li>
 *   <li><b>GOLD</b>: mayor chance de medias y raras (sigue habiendo comunes).</li>
 *   <li><b>DIAMOND</b>: premium; no sortea por rareza, entrega una única
 *       skin full-art garantizada (ver {@link #isFullArtPack()}).</li>
 * </ul>
 */
public enum PackType {

    BRONZE(100, 0.80, 0.17, 0.03, 0.03, 0.02),
    SILVER(250, 0.60, 0.30, 0.10, 0.08, 0.04),
    GOLD(500,  0.45, 0.35, 0.20, 0.15, 0.06),
    DIAMOND(2000, 0.0, 0.0, 0.0, 0.0, 0.0);

    public static final int CARDS_PER_PACK = 3;

    private final int price;
    private final Map<RarityBucket, Double> probabilities;
    private final double skinDropChance;
    private final double cardBackDropChance;

    PackType(int price, double pHigh, double pMedium, double pLow,
             double skinDropChance, double cardBackDropChance) {
        this.price = price;
        this.probabilities = new EnumMap<>(RarityBucket.class);
        this.probabilities.put(RarityBucket.HIGH, pHigh);
        this.probabilities.put(RarityBucket.MEDIUM, pMedium);
        this.probabilities.put(RarityBucket.LOW, pLow);
        this.skinDropChance = skinDropChance;
        this.cardBackDropChance = cardBackDropChance;
    }

    public int getPrice() {
        return price;
    }

    /**
     * Sobre premium que en lugar de sortear cartas por rareza entrega una
     * única skin full-art garantizada.
     */
    public boolean isFullArtPack() {
        return this == DIAMOND;
    }

    /** Cantidad de cartas que entrega este sobre. */
    public int getCardsPerPack() {
        return isFullArtPack() ? 1 : CARDS_PER_PACK;
    }

    /**
     * Probabilidad de obtener una skin bonus al abrir este sobre.
     */
    public double getSkinDropChance() {
        return skinDropChance;
    }

    /**
     * Probabilidad de obtener un dorso de carta bonus al abrir este sobre.
     */
    public double getCardBackDropChance() {
        return cardBackDropChance;
    }

    /**
     * Devuelve la probabilidad asociada al bucket de rareza dado.
     */
    public double probabilityOf(RarityBucket bucket) {
        return probabilities.getOrDefault(bucket, 0.0);
    }

    public Map<RarityBucket, Double> getProbabilities() {
        return new EnumMap<>(probabilities);
    }
}
