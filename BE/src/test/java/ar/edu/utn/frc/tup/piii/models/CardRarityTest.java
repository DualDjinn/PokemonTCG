package ar.edu.utn.frc.tup.piii.models;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CardRarityTest {

    @Test
    void values_ContainAllRarities() {
        assertThat(CardRarity.values()).containsExactly(
                CardRarity.COMMON,
                CardRarity.UNCOMMON,
                CardRarity.RARE,
                CardRarity.RARE_HOLO,
                CardRarity.RARE_HOLO_EX,
                CardRarity.RARE_ULTRA,
                CardRarity.ACE_SPEC_RARE
        );
    }

    @Test
    void fromJson_WithValidValue_ReturnsCorrectRarity() {
        assertThat(CardRarity.fromJson("Common")).isEqualTo(CardRarity.COMMON);
        assertThat(CardRarity.fromJson("Uncommon")).isEqualTo(CardRarity.UNCOMMON);
        assertThat(CardRarity.fromJson("Rare")).isEqualTo(CardRarity.RARE);
        assertThat(CardRarity.fromJson("Rare Holo")).isEqualTo(CardRarity.RARE_HOLO);
        assertThat(CardRarity.fromJson("Rare Holo EX")).isEqualTo(CardRarity.RARE_HOLO_EX);
        assertThat(CardRarity.fromJson("Rare Ultra")).isEqualTo(CardRarity.RARE_ULTRA);
        assertThat(CardRarity.fromJson("Rare Ace")).isEqualTo(CardRarity.ACE_SPEC_RARE);
    }

    @Test
    void fromJson_WithUnknownValue_ReturnsCommon() {
        assertThat(CardRarity.fromJson("UnknownRarity")).isEqualTo(CardRarity.COMMON);
    }

    @Test
    void fromJson_WithNullLikeValue_ReturnsCommon() {
        assertThat(CardRarity.fromJson("")).isEqualTo(CardRarity.COMMON);
    }

    @Test
    void fromJson_CaseSensitive_ReturnsCommon() {
        assertThat(CardRarity.fromJson("common")).isEqualTo(CardRarity.COMMON);
    }
}
