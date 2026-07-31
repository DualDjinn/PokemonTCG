package ar.edu.utn.frc.tup.piii.models.effects.trainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.PokemonStage;
import ar.edu.utn.frc.tup.piii.models.SupporterCard;

class TrainerDrawDiscardEffectsTest {

    private GameContext ctx;
    private PokemonCard pokemonCard;

    @BeforeEach
    void setUp() {
        pokemonCard = PokemonCard.builder()
                .id("p1").name("TestMon").hp(100)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.COLORLESS)
                .build();
        InPlayPokemon attacker = new InPlayPokemon(pokemonCard);
        InPlayPokemon defender = new InPlayPokemon(pokemonCard);
        ctx = new GameContext();
        ctx.setAttacker(attacker);
        ctx.setDefender(defender);
        ctx.setAttackerBench(new ArrayList<>());
        ctx.setDefenderBench(new ArrayList<>());
    }

    // ───────── Professor Sycamore Effect ─────────

    @Test
    void professorSycamore_DiscardsHandAndDraws7() {
        ctx.getAttackerHand().addAll(List.of(
                SupporterCard.builder().id("s1").name("Potion").build(),
                SupporterCard.builder().id("s2").name("Switch").build(),
                SupporterCard.builder().id("s3").name("Poke Ball").build()
        ));
        ctx.getAttackerDeck().addAll(List.of(
                PokemonCard.builder().id("d1").name("Bulbasaur").hp(60)
                        .stage(PokemonStage.BASIC).pokemonType(EnergyType.GRASS).build(),
                PokemonCard.builder().id("d2").name("Charmander").hp(60)
                        .stage(PokemonStage.BASIC).pokemonType(EnergyType.FIRE).build(),
                PokemonCard.builder().id("d3").name("Squirtle").hp(60)
                        .stage(PokemonStage.BASIC).pokemonType(EnergyType.WATER).build(),
                PokemonCard.builder().id("d4").name("Pidgey").hp(50)
                        .stage(PokemonStage.BASIC).pokemonType(EnergyType.COLORLESS).build(),
                PokemonCard.builder().id("d5").name("Rattata").hp(40)
                        .stage(PokemonStage.BASIC).pokemonType(EnergyType.COLORLESS).build(),
                PokemonCard.builder().id("d6").name("Spearow").hp(50)
                        .stage(PokemonStage.BASIC).pokemonType(EnergyType.COLORLESS).build(),
                PokemonCard.builder().id("d7").name("Weedle").hp(50)
                        .stage(PokemonStage.BASIC).pokemonType(EnergyType.GRASS).build()
        ));

        new ProfessorSycamoreEffect().apply(ctx);

        assertThat(ctx.getAttackerHand()).hasSize(7);
        assertThat(ctx.getAttackerDiscard()).hasSize(3);
        assertThat(ctx.getAttackerDeck()).isEmpty();
    }

    @Test
    void professorSycamore_EmptyHand_DoesNotThrow() {
        ctx.getAttackerDeck().addAll(List.of(
                PokemonCard.builder().id("d1").name("Bulbasaur").hp(60)
                        .stage(PokemonStage.BASIC).pokemonType(EnergyType.GRASS).build(),
                PokemonCard.builder().id("d2").name("Charmander").hp(60)
                        .stage(PokemonStage.BASIC).pokemonType(EnergyType.FIRE).build(),
                PokemonCard.builder().id("d3").name("Squirtle").hp(60)
                        .stage(PokemonStage.BASIC).pokemonType(EnergyType.WATER).build(),
                PokemonCard.builder().id("d4").name("Pidgey").hp(50)
                        .stage(PokemonStage.BASIC).pokemonType(EnergyType.COLORLESS).build(),
                PokemonCard.builder().id("d5").name("Rattata").hp(40)
                        .stage(PokemonStage.BASIC).pokemonType(EnergyType.COLORLESS).build(),
                PokemonCard.builder().id("d6").name("Spearow").hp(50)
                        .stage(PokemonStage.BASIC).pokemonType(EnergyType.COLORLESS).build(),
                PokemonCard.builder().id("d7").name("Weedle").hp(50)
                        .stage(PokemonStage.BASIC).pokemonType(EnergyType.GRASS).build()
        ));

        assertDoesNotThrow(() -> new ProfessorSycamoreEffect().apply(ctx));
        assertThat(ctx.getAttackerHand()).hasSize(7);
    }

    @Test
    void professorSycamore_DrawsLessThan7WhenDeckSmall() {
        ctx.getAttackerHand().add(SupporterCard.builder().id("s1").name("Potion").build());
        ctx.getAttackerDeck().add(PokemonCard.builder().id("d1").name("Bulbasaur").hp(60)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.GRASS).build());

        new ProfessorSycamoreEffect().apply(ctx);

        assertThat(ctx.getAttackerHand()).hasSize(1);
        assertThat(ctx.getAttackerDiscard()).hasSize(1);
    }

    // ───────── Shauna Effect ─────────

    @Test
    void shauna_ShufflesHandAndDraws5() {
        ctx.getAttackerHand().addAll(List.of(
                SupporterCard.builder().id("s1").name("Potion").build(),
                SupporterCard.builder().id("s2").name("Switch").build()
        ));
        ctx.getAttackerDeck().addAll(List.of(
                PokemonCard.builder().id("d1").name("Bulbasaur").hp(60)
                        .stage(PokemonStage.BASIC).pokemonType(EnergyType.GRASS).build(),
                PokemonCard.builder().id("d2").name("Charmander").hp(60)
                        .stage(PokemonStage.BASIC).pokemonType(EnergyType.FIRE).build(),
                PokemonCard.builder().id("d3").name("Squirtle").hp(60)
                        .stage(PokemonStage.BASIC).pokemonType(EnergyType.WATER).build(),
                PokemonCard.builder().id("d4").name("Pidgey").hp(50)
                        .stage(PokemonStage.BASIC).pokemonType(EnergyType.COLORLESS).build(),
                PokemonCard.builder().id("d5").name("Rattata").hp(40)
                        .stage(PokemonStage.BASIC).pokemonType(EnergyType.COLORLESS).build()
        ));

        new ShaunaEffect().apply(ctx);

        assertThat(ctx.getAttackerHand()).hasSize(5);
        assertThat(ctx.getAttackerDeck()).hasSize(2);
    }

    @Test
    void shauna_EmptyHand_DoesNotThrow() {
        ctx.getAttackerDeck().addAll(List.of(
                PokemonCard.builder().id("d1").name("Bulbasaur").hp(60)
                        .stage(PokemonStage.BASIC).pokemonType(EnergyType.GRASS).build(),
                PokemonCard.builder().id("d2").name("Charmander").hp(60)
                        .stage(PokemonStage.BASIC).pokemonType(EnergyType.FIRE).build(),
                PokemonCard.builder().id("d3").name("Squirtle").hp(60)
                        .stage(PokemonStage.BASIC).pokemonType(EnergyType.WATER).build(),
                PokemonCard.builder().id("d4").name("Pidgey").hp(50)
                        .stage(PokemonStage.BASIC).pokemonType(EnergyType.COLORLESS).build(),
                PokemonCard.builder().id("d5").name("Rattata").hp(40)
                        .stage(PokemonStage.BASIC).pokemonType(EnergyType.COLORLESS).build()
        ));

        assertDoesNotThrow(() -> new ShaunaEffect().apply(ctx));
        assertThat(ctx.getAttackerHand()).hasSize(5);
    }

    @Test
    void shauna_DrawsLessThan5WhenDeckSmall() {
        ctx.getAttackerHand().add(SupporterCard.builder().id("s1").name("Potion").build());
        ctx.getAttackerDeck().add(PokemonCard.builder().id("d1").name("Bulbasaur").hp(60)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.GRASS).build());

        new ShaunaEffect().apply(ctx);

        assertThat(ctx.getAttackerHand()).hasSize(2);
    }

    // ───────── Red Card Effect ─────────

    @Test
    void redCard_ShufflesDefenderHandAndDraws4() {
        ctx.getDefenderHand().addAll(List.of(
                SupporterCard.builder().id("s1").name("Potion").build(),
                SupporterCard.builder().id("s2").name("Switch").build()
        ));
        ctx.getDefenderDeck().addAll(List.of(
                PokemonCard.builder().id("d1").name("Bulbasaur").hp(60)
                        .stage(PokemonStage.BASIC).pokemonType(EnergyType.GRASS).build(),
                PokemonCard.builder().id("d2").name("Charmander").hp(60)
                        .stage(PokemonStage.BASIC).pokemonType(EnergyType.FIRE).build(),
                PokemonCard.builder().id("d3").name("Squirtle").hp(60)
                        .stage(PokemonStage.BASIC).pokemonType(EnergyType.WATER).build(),
                PokemonCard.builder().id("d4").name("Pidgey").hp(50)
                        .stage(PokemonStage.BASIC).pokemonType(EnergyType.COLORLESS).build()
        ));

        new RedCardEffect().apply(ctx);

        assertThat(ctx.getDefenderHand()).hasSize(4);
        assertThat(ctx.getDefenderDeck()).hasSize(2);
    }

    @Test
    void redCard_EmptyDefenderHand_DoesNotThrow() {
        ctx.getDefenderDeck().addAll(List.of(
                PokemonCard.builder().id("d1").name("Bulbasaur").hp(60)
                        .stage(PokemonStage.BASIC).pokemonType(EnergyType.GRASS).build(),
                PokemonCard.builder().id("d2").name("Charmander").hp(60)
                        .stage(PokemonStage.BASIC).pokemonType(EnergyType.FIRE).build(),
                PokemonCard.builder().id("d3").name("Squirtle").hp(60)
                        .stage(PokemonStage.BASIC).pokemonType(EnergyType.WATER).build(),
                PokemonCard.builder().id("d4").name("Pidgey").hp(50)
                        .stage(PokemonStage.BASIC).pokemonType(EnergyType.COLORLESS).build()
        ));

        assertDoesNotThrow(() -> new RedCardEffect().apply(ctx));
        assertThat(ctx.getDefenderHand()).hasSize(4);
    }

    @Test
    void redCard_DrawsLessThan4WhenDeckSmall() {
        ctx.getDefenderHand().add(SupporterCard.builder().id("s1").name("Potion").build());
        ctx.getDefenderDeck().add(PokemonCard.builder().id("d1").name("Bulbasaur").hp(60)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.GRASS).build());

        new RedCardEffect().apply(ctx);

        assertThat(ctx.getDefenderHand()).hasSize(2);
    }

    // ───────── Max Revive Effect ─────────

    @Test
    void maxRevive_MovesLastPokemonFromDiscardToTopOfDeck() {
        PokemonCard p1 = PokemonCard.builder().id("p2").name("Bulbasaur").hp(60)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.GRASS).build();
        PokemonCard p2 = PokemonCard.builder().id("p3").name("Charmander").hp(60)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.FIRE).build();
        ctx.getAttackerDiscard().addAll(List.of(
                SupporterCard.builder().id("s1").name("Potion").build(),
                p1,
                SupporterCard.builder().id("s2").name("Switch").build(),
                p2
        ));

        new MaxReviveEffect().apply(ctx);

        assertThat(ctx.getAttackerDiscard()).hasSize(3);
        assertThat(ctx.getAttackerDiscard()).doesNotContain(p2);
        assertThat(ctx.getAttackerDeck().getFirst()).isEqualTo(p2);
    }

    @Test
    void maxRevive_TakesMostRecentPokemonFromDiscard() {
        PokemonCard p1 = PokemonCard.builder().id("p2").name("Bulbasaur").hp(60)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.GRASS).build();
        PokemonCard p2 = PokemonCard.builder().id("p3").name("Charmander").hp(60)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.FIRE).build();
        ctx.getAttackerDiscard().addAll(List.of(p1, p2));

        new MaxReviveEffect().apply(ctx);

        assertThat(ctx.getAttackerDeck().getFirst()).isEqualTo(p2);
        assertThat(ctx.getAttackerDiscard()).containsExactly(p1);
    }

    @Test
    void maxRevive_NoPokemonInDiscard_NoOp() {
        ctx.getAttackerDiscard().addAll(List.of(
                SupporterCard.builder().id("s1").name("Potion").build(),
                SupporterCard.builder().id("s2").name("Switch").build()
        ));

        new MaxReviveEffect().apply(ctx);

        assertThat(ctx.getAttackerDiscard()).hasSize(2);
        assertThat(ctx.getAttackerDeck()).isEmpty();
    }

    @Test
    void maxRevive_EmptyDiscard_DoesNotThrow() {
        assertDoesNotThrow(() -> new MaxReviveEffect().apply(ctx));
    }

    // ───────── Team Flare Grunt Effect ─────────

    @Test
    void teamFlareGrunt_DiscardsOneEnergyFromDefender() {
        EnergyCard fire = EnergyCard.builder().id("e1").name("Fire Energy")
                .energyType(EnergyType.FIRE).build();
        EnergyCard water = EnergyCard.builder().id("e2").name("Water Energy")
                .energyType(EnergyType.WATER).build();
        ctx.getDefender().getEnergies().addAll(List.of(fire, water));
        ctx.getDefenderDiscard().add(SupporterCard.builder().id("s1").name("Old").build());

        new TeamFlareGruntEffect().apply(ctx);

        assertThat(ctx.getDefender().getEnergies()).hasSize(1);
        assertThat(ctx.getDefenderDiscard()).contains(fire);
        assertThat(ctx.getDefenderDiscard()).hasSize(2);
    }

    @Test
    void teamFlareGrunt_NoEnergyOnDefender_NoOp() {
        assertDoesNotThrow(() -> new TeamFlareGruntEffect().apply(ctx));
        assertThat(ctx.getDefender().getEnergies()).isEmpty();
    }

    @Test
    void teamFlareGrunt_NullDefender_NoOp() {
        ctx.setDefender(null);
        assertDoesNotThrow(() -> new TeamFlareGruntEffect().apply(ctx));
    }
}
