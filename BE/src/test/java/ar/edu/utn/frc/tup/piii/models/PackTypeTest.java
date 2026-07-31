package ar.edu.utn.frc.tup.piii.models;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit tests for {@link PackType}.
 *
 * <p>Validan:
 * <ul>
 *   <li>Que cada tipo tenga precio &gt; 0 e incremental (Bronce &lt; Plata &lt; Oro).</li>
 *   <li>Que las probabilidades de cada sobre sumen exactamente 1.0.</li>
 *   <li>Que la distribución respete la jerarquía esperada por bucket.</li>
 *   <li>Que {@link PackType#getProbabilities()} devuelva una copia defensiva.</li>
 * </ul>
 */
class PackTypeTest {

    private static final double EPSILON = 0.0001;

    @Test
    void allPackTypesHavePositivePrice() {
        for (PackType type : PackType.values()) {
            assertThat(type.getPrice()).isPositive();
        }
    }

    @Test
    void pricesAreIncrementalBronzeSilverGold() {
        assertThat(PackType.BRONZE.getPrice()).isLessThan(PackType.SILVER.getPrice());
        assertThat(PackType.SILVER.getPrice()).isLessThan(PackType.GOLD.getPrice());
    }

    @Test
    void cardsPerPackIsThree() {
        assertThat(PackType.CARDS_PER_PACK).isEqualTo(3);
    }

    @Test
    void allProbabilitiesSumToOne() {
        for (PackType type : PackType.values()) {
            // DIAMOND es premium full-art: no sortea por rareza, sus probabilidades son 0.
            if (type.isFullArtPack()) {
                continue;
            }
            double total = 0.0;
            for (RarityBucket bucket : RarityBucket.values()) {
                total += type.probabilityOf(bucket);
            }
            assertThat(total)
                    .as("Probabilities of %s must sum to 1.0", type)
                    .isCloseTo(1.0, within(EPSILON));
        }
    }

    @Test
    void bronzePrioritizesHighBucket() {
        assertThat(PackType.BRONZE.probabilityOf(RarityBucket.HIGH))
                .isGreaterThan(PackType.BRONZE.probabilityOf(RarityBucket.MEDIUM));
        assertThat(PackType.BRONZE.probabilityOf(RarityBucket.MEDIUM))
                .isGreaterThan(PackType.BRONZE.probabilityOf(RarityBucket.LOW));
    }

    @Test
    void goldHasMoreLowBucketProbabilityThanBronze() {
        // Spec: oro debe dar más chance a cartas raras (LOW bucket) que bronce.
        assertThat(PackType.GOLD.probabilityOf(RarityBucket.LOW))
                .isGreaterThan(PackType.SILVER.probabilityOf(RarityBucket.LOW))
                .isGreaterThan(PackType.BRONZE.probabilityOf(RarityBucket.LOW));
    }

    @Test
    void probabilityOfReturnsExpectedValuesForBronze() {
        assertThat(PackType.BRONZE.probabilityOf(RarityBucket.HIGH)).isEqualTo(0.80);
        assertThat(PackType.BRONZE.probabilityOf(RarityBucket.MEDIUM)).isEqualTo(0.17);
        assertThat(PackType.BRONZE.probabilityOf(RarityBucket.LOW)).isEqualTo(0.03);
    }

    @Test
    void probabilityOfReturnsExpectedValuesForSilver() {
        assertThat(PackType.SILVER.probabilityOf(RarityBucket.HIGH)).isEqualTo(0.60);
        assertThat(PackType.SILVER.probabilityOf(RarityBucket.MEDIUM)).isEqualTo(0.30);
        assertThat(PackType.SILVER.probabilityOf(RarityBucket.LOW)).isEqualTo(0.10);
    }

    @Test
    void probabilityOfReturnsExpectedValuesForGold() {
        assertThat(PackType.GOLD.probabilityOf(RarityBucket.HIGH)).isEqualTo(0.45);
        assertThat(PackType.GOLD.probabilityOf(RarityBucket.MEDIUM)).isEqualTo(0.35);
        assertThat(PackType.GOLD.probabilityOf(RarityBucket.LOW)).isEqualTo(0.20);
    }

    @Test
    void probabilityOfWithNullBucketReturnsZero() {
        // Edge case: getOrDefault evita NPE devolviendo 0.
        assertThat(PackType.BRONZE.probabilityOf(null)).isZero();
        assertThat(PackType.SILVER.probabilityOf(null)).isZero();
        assertThat(PackType.GOLD.probabilityOf(null)).isZero();
    }

    @Test
    void getProbabilitiesReturnsDefensiveCopy() {
        // Modificar el mapa devuelto NO debe afectar el enum.
        Map<RarityBucket, Double> probs = PackType.BRONZE.getProbabilities();
        probs.put(RarityBucket.HIGH, 0.0);
        probs.clear();

        // Volver a pedir: el original sigue intacto.
        assertThat(PackType.BRONZE.probabilityOf(RarityBucket.HIGH)).isEqualTo(0.80);
        assertThat(PackType.BRONZE.getProbabilities()).hasSize(3);
    }

    @Test
    void getProbabilitiesContainsAllBuckets() {
        for (PackType type : PackType.values()) {
            Map<RarityBucket, Double> probs = type.getProbabilities();
            assertThat(probs).containsKeys(RarityBucket.HIGH, RarityBucket.MEDIUM, RarityBucket.LOW);
        }
    }

    @Test
    void valuesContainsExactlyFourPackTypes() {
        assertThat(PackType.values()).containsExactly(
                PackType.BRONZE, PackType.SILVER, PackType.GOLD, PackType.DIAMOND);
    }
}
