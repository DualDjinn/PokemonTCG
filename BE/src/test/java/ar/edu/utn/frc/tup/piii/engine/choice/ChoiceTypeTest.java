package ar.edu.utn.frc.tup.piii.engine.choice;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChoiceTypeTest {

    @Test
    void enum_ShouldHaveExpectedValues() {
        assertThat(ChoiceType.values())
                .containsExactly(
                        ChoiceType.PICK_FROM_DECK_TOP_N,
                        ChoiceType.PICK_BASIC_ENERGIES_FROM_DECK,
                        ChoiceType.PICK_BENCHED_POKEMON,
                        ChoiceType.REORDER_DECK_TOP_N,
                        ChoiceType.COIN_FLIP_INFO,
                        ChoiceType.CHOOSE_COIN_SIDE
                );
    }

    @Test
    void enum_ShouldHaveSixValues() {
        assertThat(ChoiceType.values()).hasSize(6);
    }

    @Test
    void valueOf_ShouldReturnCorrectEnum() {
        assertThat(ChoiceType.valueOf("PICK_FROM_DECK_TOP_N")).isEqualTo(ChoiceType.PICK_FROM_DECK_TOP_N);
        assertThat(ChoiceType.valueOf("PICK_BASIC_ENERGIES_FROM_DECK")).isEqualTo(ChoiceType.PICK_BASIC_ENERGIES_FROM_DECK);
        assertThat(ChoiceType.valueOf("PICK_BENCHED_POKEMON")).isEqualTo(ChoiceType.PICK_BENCHED_POKEMON);
        assertThat(ChoiceType.valueOf("REORDER_DECK_TOP_N")).isEqualTo(ChoiceType.REORDER_DECK_TOP_N);
        assertThat(ChoiceType.valueOf("COIN_FLIP_INFO")).isEqualTo(ChoiceType.COIN_FLIP_INFO);
        assertThat(ChoiceType.valueOf("CHOOSE_COIN_SIDE")).isEqualTo(ChoiceType.CHOOSE_COIN_SIDE);
    }

    @Test
    void valueOf_WithInvalidName_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> ChoiceType.valueOf("INVALID"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void enum_ValuesAreDistinct() {
        ChoiceType[] values = ChoiceType.values();
        for (int i = 0; i < values.length; i++) {
            for (int j = i + 1; j < values.length; j++) {
                assertThat(values[i]).isNotEqualTo(values[j]);
            }
        }
    }

    @Test
    void enum_OrdinalsAreSequential() {
        ChoiceType[] values = ChoiceType.values();
        for (int i = 0; i < values.length; i++) {
            assertThat(values[i].ordinal()).isEqualTo(i);
        }
    }

    @Test
    void enum_NamesMatchExpected() {
        assertThat(ChoiceType.PICK_FROM_DECK_TOP_N.name()).isEqualTo("PICK_FROM_DECK_TOP_N");
        assertThat(ChoiceType.PICK_BASIC_ENERGIES_FROM_DECK.name()).isEqualTo("PICK_BASIC_ENERGIES_FROM_DECK");
        assertThat(ChoiceType.PICK_BENCHED_POKEMON.name()).isEqualTo("PICK_BENCHED_POKEMON");
        assertThat(ChoiceType.REORDER_DECK_TOP_N.name()).isEqualTo("REORDER_DECK_TOP_N");
        assertThat(ChoiceType.COIN_FLIP_INFO.name()).isEqualTo("COIN_FLIP_INFO");
        assertThat(ChoiceType.CHOOSE_COIN_SIDE.name()).isEqualTo("CHOOSE_COIN_SIDE");
    }
}
