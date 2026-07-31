package ar.edu.utn.frc.tup.piii.models;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PokemonCardTest {

    private PokemonCard createBasicPokemon() {
        return PokemonCard.builder()
                .id("test-1")
                .name("TestPokemon")
                .hp(100)
                .stage(PokemonStage.BASIC)
                .isEx(false)
                .isMega(false)
                .pokemonType(EnergyType.GRASS)
                .attacks(List.of())
                .weaknesses(List.of())
                .resistances(List.of())
                .retreatCost(1)
                .evolvesFrom(null)
                .evolvesTo(List.of())
                .rules(List.of())
                .build();
    }

    private PokemonCard createStage1Pokemon() {
        return PokemonCard.builder()
                .id("test-2")
                .name("TestEvolution")
                .hp(80)
                .stage(PokemonStage.STAGE_1)
                .isEx(false)
                .isMega(false)
                .pokemonType(EnergyType.FIRE)
                .attacks(List.of())
                .weaknesses(List.of())
                .resistances(List.of())
                .retreatCost(2)
                .evolvesFrom("TestPokemon")
                .evolvesTo(List.of())
                .rules(List.of())
                .build();
    }

    @Test
    void isBasic_WhenBasic_ReturnsTrue() {
        PokemonCard card = createBasicPokemon();
        assertThat(card.isBasic()).isTrue();
    }

    @Test
    void isBasic_WhenNotBasic_ReturnsFalse() {
        PokemonCard card = createStage1Pokemon();
        assertThat(card.isBasic()).isFalse();
    }

    @Test
    void canEvolveFrom_WhenEvolvesFromMatches_ReturnsTrue() {
        PokemonCard basic = createBasicPokemon();
        PokemonCard stage1 = createStage1Pokemon();
        assertThat(stage1.canEvolveFrom(basic)).isTrue();
    }

    @Test
    void canEvolveFrom_WhenEvolvesFromDoesNotMatch_ReturnsFalse() {
        PokemonCard other = PokemonCard.builder()
                .id("other")
                .name("OtherPokemon")
                .hp(50)
                .stage(PokemonStage.BASIC)
                .build();
        PokemonCard stage1 = createStage1Pokemon();
        assertThat(stage1.canEvolveFrom(other)).isFalse();
    }

    @Test
    void canEvolveFrom_WhenEvolvesFromNull_ReturnsFalse() {
        PokemonCard basic = createBasicPokemon();
        assertThat(basic.canEvolveFrom(basic)).isFalse();
    }

    @Test
    void getPrizeValue_WhenEx_Returns2() {
        PokemonCard card = PokemonCard.builder()
                .id("ex-1")
                .name("Venusaur-EX")
                .hp(180)
                .stage(PokemonStage.BASIC)
                .isEx(true)
                .isMega(false)
                .build();
        assertThat(card.getPrizeValue()).isEqualTo(2);
    }

    @Test
    void getPrizeValue_WhenMega_Returns2() {
        PokemonCard card = PokemonCard.builder()
                .id("mega-1")
                .name("Mega-Charizard")
                .hp(220)
                .stage(PokemonStage.MEGA)
                .isEx(false)
                .isMega(true)
                .build();
        assertThat(card.getPrizeValue()).isEqualTo(2);
    }

    @Test
    void getPrizeValue_WhenNormal_Returns1() {
        PokemonCard card = createBasicPokemon();
        assertThat(card.getPrizeValue()).isEqualTo(1);
    }

    @Test
    void getSupertype_ReturnsPokemon() {
        PokemonCard card = createBasicPokemon();
        assertThat(card.getSupertype()).isEqualTo(Supertype.POKEMON);
    }

    @Test
    void canBePlayed_ThrowsNotImplemented() {
        PokemonCard card = createBasicPokemon();
        assertThrows(org.apache.commons.lang3.NotImplementedException.class,
                () -> card.canBePlayed(new GameContext()));
    }

    @Test
    void isFullArt_WhenRarityIsRareUltra_ReturnsTrue() {
        PokemonCard card = createBasicPokemon();
        card.setRarity(CardRarity.RARE_ULTRA);
        assertThat(card.isFullArt()).isTrue();
    }

    @Test
    void isFullArt_WhenRarityIsRareHoloEx_ReturnsFalse() {
        PokemonCard card = createBasicPokemon();
        card.setRarity(CardRarity.RARE_HOLO_EX);
        assertThat(card.isFullArt()).isFalse();
    }

    @Test
    void isFullArt_WhenRarityIsNull_ReturnsFalse() {
        PokemonCard card = createBasicPokemon();
        assertThat(card.isFullArt()).isFalse();
    }
}
