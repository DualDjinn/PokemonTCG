package ar.edu.utn.frc.tup.piii.models;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PokemonStageTest {

    @Test
    void values_ContainAllStages() {
        assertThat(PokemonStage.values()).containsExactly(
                PokemonStage.BASIC,
                PokemonStage.STAGE_1,
                PokemonStage.STAGE_2,
                PokemonStage.MEGA
        );
    }

    @Test
    void fromJson_WithValidValue_ReturnsCorrectStage() {
        assertThat(PokemonStage.fromJson("Basic")).isEqualTo(PokemonStage.BASIC);
        assertThat(PokemonStage.fromJson("Stage 1")).isEqualTo(PokemonStage.STAGE_1);
        assertThat(PokemonStage.fromJson("Stage 2")).isEqualTo(PokemonStage.STAGE_2);
        assertThat(PokemonStage.fromJson("Mega")).isEqualTo(PokemonStage.MEGA);
    }

    @Test
    void fromJson_WithInvalidValue_ThrowsException() {
        assertThatThrownBy(() -> PokemonStage.fromJson("InvalidStage"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stage desconocido");
    }

    @Test
    void fromJson_CaseSensitive() {
        assertThatThrownBy(() -> PokemonStage.fromJson("basic"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
