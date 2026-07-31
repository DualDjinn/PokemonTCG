package ar.edu.utn.frc.tup.piii.services;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.stereotype.Component;

import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.PackType;
import ar.edu.utn.frc.tup.piii.models.RarityBucket;

/**
 * Algoritmo de sorteo de cartas para un sobre.
 *
 * <p>Aislado de Spring HTTP / persistencia para poder testearse en isolation
 * con cobertura ≥90% (en línea con el estilo del proyecto: {@code RuleValidator},
 * {@code DamageCalculator}, etc).
 *
 * <p>Determinismo: acepta una instancia de {@link Random} inyectable por constructor,
 * facilitando tests reproducibles mockeando el RNG.
 */
@Component
public class PackRandomizer {

    private final Random random;

    /** Constructor productivo: usa un {@link Random} no determinístico. */
    public PackRandomizer() {
        this(new Random());
    }

    /** Constructor para tests: permite inyectar un RNG controlado. */
    public PackRandomizer(Random random) {
        this.random = random;
    }

    /**
     * Sortea {@link PackType#CARDS_PER_PACK} cartas del pool dado, aplicando las
     * probabilidades de bucket del {@code packType}.
     *
     * @param packType  define la distribución de buckets
     * @param cardPool  catálogo completo del cual sortear
     * @return lista inmutable de cartas (puede contener repetidas)
     * @throws IllegalArgumentException si el pool está vacío o si algún bucket
     *                                  con probabilidad &gt; 0 no tiene cartas
     */
    public List<Card> drawCards(PackType packType, List<Card> cardPool) {
        if (cardPool == null || cardPool.isEmpty()) {
            throw new IllegalArgumentException("Card pool cannot be empty");
        }
        Map<RarityBucket, List<Card>> byBucket = groupByBucket(cardPool);

        List<Card> result = new ArrayList<>(PackType.CARDS_PER_PACK);
        for (int i = 0; i < PackType.CARDS_PER_PACK; i++) {
            RarityBucket bucket = pickBucket(packType, byBucket);
            List<Card> candidates = byBucket.get(bucket);
            result.add(candidates.get(random.nextInt(candidates.size())));
        }
        return result;
    }

    /**
     * Determina si un sobre otorga una skin bonus.
     */
    public boolean shouldDropSkin(PackType packType) {
        return random.nextDouble() < packType.getSkinDropChance();
    }

    /**
     * Determina si un sobre otorga un dorso de carta bonus.
     */
    public boolean shouldDropCardBack(PackType packType) {
        return random.nextDouble() < packType.getCardBackDropChance();
    }

    /**
     * Agrupa las cartas del pool por bucket de rareza, descartando buckets vacíos.
     */
    private Map<RarityBucket, List<Card>> groupByBucket(List<Card> cardPool) {
        Map<RarityBucket, List<Card>> byBucket = new EnumMap<>(RarityBucket.class);
        for (Card card : cardPool) {
            RarityBucket bucket = RarityBucket.fromRarity(card.getRarity());
            byBucket.computeIfAbsent(bucket, b -> new ArrayList<>()).add(card);
        }
        return byBucket;
    }

    /**
     * Elige un bucket según las probabilidades del {@code packType}, con fallback
     * a cualquier bucket disponible si el sorteado no tiene cartas.
     */
    private RarityBucket pickBucket(PackType packType, Map<RarityBucket, List<Card>> byBucket) {
        double roll = random.nextDouble();
        double cumulative = 0.0;
        for (RarityBucket bucket : RarityBucket.values()) {
            cumulative += packType.probabilityOf(bucket);
            if (roll < cumulative && byBucket.containsKey(bucket)) {
                return bucket;
            }
        }
        // Fallback: si el bucket sorteado está vacío o el roll cae justo en el borde,
        // devolver el primer bucket con cartas disponibles.
        for (RarityBucket bucket : RarityBucket.values()) {
            if (byBucket.containsKey(bucket)) {
                return bucket;
            }
        }
        throw new IllegalStateException("No buckets available in card pool");
    }
}
