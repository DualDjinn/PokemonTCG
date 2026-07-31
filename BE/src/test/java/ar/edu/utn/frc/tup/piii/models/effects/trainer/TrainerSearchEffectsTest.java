package ar.edu.utn.frc.tup.piii.models.effects.trainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frc.tup.piii.engine.choice.PendingPlayerChoice;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.PokemonStage;

class TrainerSearchEffectsTest {

    private static class FixedRandom extends Random {
        private final boolean[] values;
        private int idx;
        FixedRandom(boolean... values) { this.values = values; this.idx = 0; }
        @Override public boolean nextBoolean() { return values[idx++ % values.length]; }
        @Override public int nextInt(int bound) { return nextBoolean() ? bound - 1 : 0; }
        protected int next(int bits) { return nextBoolean() ? 1 : 0; }
    }

    private GameContext ctx;

    @BeforeEach
    void setUp() {
        PokemonCard base = PokemonCard.builder()
                .id("p1").name("TestMon").hp(100)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.COLORLESS)
                .build();
        ctx = new GameContext();
        ctx.setAttacker(new InPlayPokemon(base));
        ctx.setDefender(new InPlayPokemon(base));
        ctx.setAttackerBench(new ArrayList<>());
        ctx.setDefenderBench(new ArrayList<>());
        ctx.setAttackerPlayerId("p1");
    }

    // ───────── Great Ball Effect ─────────

    @Test
    void greatBall_SetsPendingChoiceWithPokemonFromTop7() {
        PokemonCard bulbasaur = PokemonCard.builder().id("d1").name("Bulbasaur").hp(60)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.GRASS).build();
        ctx.getAttackerDeck().add(bulbasaur);

        new GreatBallEffect().apply(ctx);

        PendingPlayerChoice choice = ctx.getPendingPlayerChoice();
        assertThat(choice).isNotNull();
        assertThat(choice.getOptions()).containsExactly(bulbasaur);
    }

    @Test
    void greatBall_NoPokemonInTop7_ShufflesAndNoPrompt() {
        for (int i = 0; i < 7; i++) {
            ctx.getAttackerDeck().add(EnergyCard.builder().id("e" + i).name("Energy")
                    .energyType(EnergyType.FIRE).build());
        }

        new GreatBallEffect().apply(ctx);

        assertThat(ctx.getPendingPlayerChoice()).isNull();
        assertThat(ctx.getAttackerDeck()).hasSize(7);
    }

    @Test
    void greatBall_FewerThan7Cards_UsesAvailableCount() {
        PokemonCard bulbasaur = PokemonCard.builder().id("d1").name("Bulbasaur").hp(60)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.GRASS).build();
        ctx.getAttackerDeck().add(bulbasaur);

        new GreatBallEffect().apply(ctx);

        assertThat(ctx.getPendingPlayerChoice().getOptions()).hasSize(1);
    }

    @Test
    void greatBall_CallbackPutsPokemonInHand() {
        PokemonCard bulbasaur = PokemonCard.builder().id("bulba").name("Bulbasaur").hp(60)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.GRASS).build();
        ctx.getAttackerDeck().add(bulbasaur);

        new GreatBallEffect().apply(ctx);
        ctx.getPendingPlayerChoice().getCallback().accept(List.of("bulba"));

        assertThat(ctx.getAttackerHand()).containsExactly(bulbasaur);
        assertThat(ctx.getAttackerDeck()).isEmpty();
    }

    @Test
    void greatBall_CallbackEmpty_ShufflesWithoutAdding() {
        PokemonCard bulbasaur = PokemonCard.builder().id("bulba").name("Bulbasaur").hp(60)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.GRASS).build();
        ctx.getAttackerDeck().add(bulbasaur);

        new GreatBallEffect().apply(ctx);
        assertDoesNotThrow(() -> ctx.getPendingPlayerChoice().getCallback().accept(List.of()));

        assertThat(ctx.getAttackerHand()).isEmpty();
        assertThat(ctx.getAttackerDeck()).hasSize(1);
    }

    // ───────── Professor's Letter Effect ─────────

    @Test
    void professorsLetter_SetsPendingChoiceWithEnergies() {
        EnergyCard fire = EnergyCard.builder().id("e1").name("Fire Energy")
                .energyType(EnergyType.FIRE).build();
        EnergyCard water = EnergyCard.builder().id("e2").name("Water Energy")
                .energyType(EnergyType.WATER).build();
        ctx.getAttackerDeck().addAll(List.of(fire, water));

        new ProfessorsLetterEffect().apply(ctx);

        PendingPlayerChoice choice = ctx.getPendingPlayerChoice();
        assertThat(choice).isNotNull();
        assertThat(choice.getOptions()).containsExactly(fire, water);
        assertThat(choice.getMaxPick()).isEqualTo(2);
    }

    @Test
    void professorsLetter_Only1Energy_MaxPickIs1() {
        ctx.getAttackerDeck().add(EnergyCard.builder().id("e1").name("Fire Energy")
                .energyType(EnergyType.FIRE).build());

        new ProfessorsLetterEffect().apply(ctx);

        assertThat(ctx.getPendingPlayerChoice().getMaxPick()).isEqualTo(1);
    }

    @Test
    void professorsLetter_NoEnergies_ShufflesAndNoPrompt() {
        ctx.getAttackerDeck().add(PokemonCard.builder().id("d1").name("Bulbasaur").hp(60)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.GRASS).build());

        new ProfessorsLetterEffect().apply(ctx);

        assertThat(ctx.getPendingPlayerChoice()).isNull();
    }

    @Test
    void professorsLetter_CallbackPutsEnergiesInHand() {
        EnergyCard fire = EnergyCard.builder().id("e1").name("Fire Energy")
                .energyType(EnergyType.FIRE).build();
        EnergyCard water = EnergyCard.builder().id("e2").name("Water Energy")
                .energyType(EnergyType.WATER).build();
        ctx.getAttackerDeck().addAll(List.of(fire, water));

        new ProfessorsLetterEffect().apply(ctx);
        ctx.getPendingPlayerChoice().getCallback().accept(List.of("e1"));

        assertThat(ctx.getAttackerHand()).containsExactly(fire);
        assertThat(ctx.getAttackerDeck()).containsExactly(water);
    }

    @Test
    void professorsLetter_CallbackPicksUpTo2() {
        EnergyCard fire = EnergyCard.builder().id("e1").name("Fire Energy")
                .energyType(EnergyType.FIRE).build();
        EnergyCard water = EnergyCard.builder().id("e2").name("Water Energy")
                .energyType(EnergyType.WATER).build();
        EnergyCard grass = EnergyCard.builder().id("e3").name("Grass Energy")
                .energyType(EnergyType.GRASS).build();
        ctx.getAttackerDeck().addAll(List.of(fire, water, grass));

        new ProfessorsLetterEffect().apply(ctx);
        ctx.getPendingPlayerChoice().getCallback().accept(List.of("e1", "e3"));

        assertThat(ctx.getAttackerHand()).containsExactly(fire, grass);
        assertThat(ctx.getAttackerDeck()).containsExactly(water);
    }

    @Test
    void professorsLetter_CallbackEmpty_NoOp() {
        ctx.getAttackerDeck().add(EnergyCard.builder().id("e1").name("Fire Energy")
                .energyType(EnergyType.FIRE).build());

        new ProfessorsLetterEffect().apply(ctx);
        assertDoesNotThrow(() -> ctx.getPendingPlayerChoice().getCallback().accept(List.of()));
        assertThat(ctx.getAttackerHand()).isEmpty();
        assertThat(ctx.getAttackerDeck()).hasSize(1);
    }

    // ───────── Roller Skates Effect ─────────

    @Test
    void rollerSkates_GuessesHeadsCorrectly_Draws3() {
        ctx.setRandom(new FixedRandom(true)); // heads
        ctx.getAttackerDeck().addAll(List.of(
                PokemonCard.builder().id("d1").name("Bulbasaur").hp(60)
                        .stage(PokemonStage.BASIC).pokemonType(EnergyType.GRASS).build(),
                PokemonCard.builder().id("d2").name("Charmander").hp(60)
                        .stage(PokemonStage.BASIC).pokemonType(EnergyType.FIRE).build(),
                PokemonCard.builder().id("d3").name("Squirtle").hp(60)
                        .stage(PokemonStage.BASIC).pokemonType(EnergyType.WATER).build()
        ));

        new RollerSkatesEffect().apply(ctx);
        ctx.getPendingPlayerChoice().getCallback().accept(List.of("heads"));

        assertThat(ctx.getAttackerHand()).hasSize(3);
        assertThat(ctx.getAttackerDeck()).isEmpty();
    }

    @Test
    void rollerSkates_GuessesHeadsWrong_DrawsNothing() {
        ctx.setRandom(new FixedRandom(false)); // tails
        ctx.getAttackerDeck().addAll(List.of(
                PokemonCard.builder().id("d1").name("Bulbasaur").hp(60)
                        .stage(PokemonStage.BASIC).pokemonType(EnergyType.GRASS).build(),
                PokemonCard.builder().id("d2").name("Charmander").hp(60)
                        .stage(PokemonStage.BASIC).pokemonType(EnergyType.FIRE).build()
        ));

        new RollerSkatesEffect().apply(ctx);
        ctx.getPendingPlayerChoice().getCallback().accept(List.of("heads"));

        assertThat(ctx.getAttackerHand()).isEmpty();
    }

    @Test
    void rollerSkates_GuessesTailsWhenIsTails_Draws3() {
        ctx.setRandom(new FixedRandom(false)); // tails
        ctx.getAttackerDeck().addAll(List.of(
                PokemonCard.builder().id("d1").name("Bulbasaur").hp(60)
                        .stage(PokemonStage.BASIC).pokemonType(EnergyType.GRASS).build(),
                PokemonCard.builder().id("d2").name("Charmander").hp(60)
                        .stage(PokemonStage.BASIC).pokemonType(EnergyType.FIRE).build(),
                PokemonCard.builder().id("d3").name("Squirtle").hp(60)
                        .stage(PokemonStage.BASIC).pokemonType(EnergyType.WATER).build()
        ));

        new RollerSkatesEffect().apply(ctx);
        ctx.getPendingPlayerChoice().getCallback().accept(List.of("tails"));

        assertThat(ctx.getAttackerHand()).hasSize(3);
    }

    @Test
    void rollerSkates_EmptyChoice_TreatedAsWrongGuess() {
        ctx.setRandom(new FixedRandom(true));
        ctx.getAttackerDeck().add(PokemonCard.builder().id("d1").name("Bulbasaur").hp(60)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.GRASS).build());

        new RollerSkatesEffect().apply(ctx);
        assertDoesNotThrow(() -> ctx.getPendingPlayerChoice().getCallback().accept(List.of()));
        assertThat(ctx.getAttackerHand()).isEmpty();
    }
}
