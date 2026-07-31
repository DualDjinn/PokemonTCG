package ar.edu.utn.frc.tup.piii.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RarityBucket}.
 *
 * <p>Valida el mapeo {@code CardRarity → RarityBucket} usado por la tienda
 * para calcular probabilidades de aparición.
 */
class RarityBucketTest {

    @Test
    void commonMapsToHigh() {
        assertThat(RarityBucket.fromRarity(CardRarity.COMMON)).isEqualTo(RarityBucket.HIGH);
    }

    @Test
    void uncommonMapsToMedium() {
        assertThat(RarityBucket.fromRarity(CardRarity.UNCOMMON)).isEqualTo(RarityBucket.MEDIUM);
    }

    @Test
    void rareMapsToLow() {
        assertThat(RarityBucket.fromRarity(CardRarity.RARE)).isEqualTo(RarityBucket.LOW);
    }

    @Test
    void rareHoloMapsToLow() {
        assertThat(RarityBucket.fromRarity(CardRarity.RARE_HOLO)).isEqualTo(RarityBucket.LOW);
    }

    @Test
    void rareHoloExMapsToLow() {
        assertThat(RarityBucket.fromRarity(CardRarity.RARE_HOLO_EX)).isEqualTo(RarityBucket.LOW);
    }

    @Test
    void rareUltraMapsToHigh() {
        assertThat(RarityBucket.fromRarity(CardRarity.RARE_ULTRA)).isEqualTo(RarityBucket.HIGH);
    }

    @Test
    void aceSpecRareMapsToLow() {
        assertThat(RarityBucket.fromRarity(CardRarity.ACE_SPEC_RARE)).isEqualTo(RarityBucket.LOW);
    }

    @Test
    void nullRarityDefaultsToHigh() {
        // Edge case: nunca queremos que falle si la carta no tiene rareza asignada.
        assertThat(RarityBucket.fromRarity(null)).isEqualTo(RarityBucket.HIGH);
    }

    @Test
    void everyCardRarityMapsToNonNullBucket() {
        for (CardRarity rarity : CardRarity.values()) {
            assertThat(RarityBucket.fromRarity(rarity))
                    .as("rarity %s must map to a non-null bucket", rarity)
                    .isNotNull();
        }
    }

    @Test
    void valuesContainsExactlyThreeBuckets() {
        assertThat(RarityBucket.values()).containsExactly(
                RarityBucket.HIGH, RarityBucket.MEDIUM, RarityBucket.LOW);
    }
}
