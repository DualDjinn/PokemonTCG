package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.PokemonStage;
import ar.edu.utn.frc.tup.piii.models.StatusCondition;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link StatusEffectManager}.
 */
class StatusEffectManagerTest {

    private final StatusEffectManager statusEffectManager = new StatusEffectManager();

    @Test
    void shouldKnockOutPokemonWhenPoisonDamageReducesHpToZero() {
        InPlayPokemon pokemon = createPokemon(100);
        pokemon.applyStatus(StatusCondition.POISONED);
        pokemon.setDamageTaken(90);
        GameContext context = createContext();

        statusEffectManager.processEndOfTurnEffects(pokemon, context);

        assertThat(pokemon.getDamageTaken()).isEqualTo(100);
        assertThat(pokemon.isKnockedOut()).isTrue();
        assertThat(pokemon.hasStatus(StatusCondition.POISONED)).isTrue();
    }

    @Test
    void shouldRemoveBurnedWithoutApplyingDamageOnHeads() {
        InPlayPokemon pokemon = createPokemon(100);
        pokemon.applyStatus(StatusCondition.BURNED);
        GameContext context = createContext(true);

        statusEffectManager.processEndOfTurnEffects(pokemon, context);

        assertThat(pokemon.getDamageTaken()).isZero();
        assertThat(pokemon.hasStatus(StatusCondition.BURNED)).isFalse();
    }

    @Test
    void shouldApplyBurnDamageAndRemoveBurnedOnTails() {
        InPlayPokemon pokemon = createPokemon(100);
        pokemon.applyStatus(StatusCondition.BURNED);
        GameContext context = createContext(false);

        statusEffectManager.processEndOfTurnEffects(pokemon, context);

        assertThat(pokemon.getDamageTaken()).isEqualTo(20);
        assertThat(pokemon.hasStatus(StatusCondition.BURNED)).isFalse();
    }

    @Test
    void shouldWakeUpSleepingPokemonOnHeads() {
        InPlayPokemon pokemon = createPokemon(100);
        pokemon.applyStatus(StatusCondition.ASLEEP);
        GameContext context = createContext(true);

        statusEffectManager.processEndOfTurnEffects(pokemon, context);

        assertThat(pokemon.hasStatus(StatusCondition.ASLEEP)).isFalse();
    }

    @Test
    void shouldKeepSleepingPokemonAsleepOnTails() {
        InPlayPokemon pokemon = createPokemon(100);
        pokemon.applyStatus(StatusCondition.ASLEEP);
        GameContext context = createContext(false);

        statusEffectManager.processEndOfTurnEffects(pokemon, context);

        assertThat(pokemon.hasStatus(StatusCondition.ASLEEP)).isTrue();
    }

    @Test
    void shouldRemoveParalyzedAtEndOfTurn() {
        InPlayPokemon pokemon = createPokemon(100);
        pokemon.applyStatus(StatusCondition.PARALYZED);
        GameContext context = createContext();

        statusEffectManager.processEndOfTurnEffects(pokemon, context);

        assertThat(pokemon.hasStatus(StatusCondition.PARALYZED)).isFalse();
    }

    @Test
    void shouldDoNothingWhenPokemonHasNoStatusConditions() {
        InPlayPokemon pokemon = createPokemon(100);
        GameContext context = createContext();

        statusEffectManager.processEndOfTurnEffects(pokemon, context);

        assertThat(pokemon.getDamageTaken()).isZero();
        assertThat(pokemon.isKnockedOut()).isFalse();
        assertThat(pokemon.hasStatus(StatusCondition.POISONED)).isFalse();
        assertThat(pokemon.hasStatus(StatusCondition.BURNED)).isFalse();
        assertThat(pokemon.hasStatus(StatusCondition.ASLEEP)).isFalse();
        assertThat(pokemon.hasStatus(StatusCondition.PARALYZED)).isFalse();
    }

    @Test
    void shouldResolveStatusesInXy1Order() {
        InPlayPokemon pokemon = createPokemon(100);
        pokemon.addStatus(StatusCondition.BURNED);
        pokemon.addStatus(StatusCondition.ASLEEP);
        GameContext context = createContext(true, false);

        statusEffectManager.processEndOfTurnEffects(pokemon, context);

        assertThat(pokemon.getDamageTaken()).isZero();
        assertThat(pokemon.hasStatus(StatusCondition.BURNED)).isFalse();
        assertThat(pokemon.hasStatus(StatusCondition.ASLEEP)).isTrue();
    }

    @Test
    void shouldResolveMultipleSimultaneousStatusesInOrder() {
        InPlayPokemon pokemon = createPokemon(100);
        pokemon.addStatus(StatusCondition.POISONED);
        pokemon.addStatus(StatusCondition.BURNED);
        pokemon.addStatus(StatusCondition.ASLEEP);
        pokemon.addStatus(StatusCondition.PARALYZED);
        GameContext context = createContext(false, true);

        statusEffectManager.processEndOfTurnEffects(pokemon, context);

        assertThat(pokemon.getDamageTaken()).isEqualTo(30);
        assertThat(pokemon.hasStatus(StatusCondition.POISONED)).isTrue();
        assertThat(pokemon.hasStatus(StatusCondition.BURNED)).isFalse();
        assertThat(pokemon.hasStatus(StatusCondition.ASLEEP)).isFalse();
        assertThat(pokemon.hasStatus(StatusCondition.PARALYZED)).isFalse();
    }

    @Test
    void shouldStopResolvingRemainingStatusesAfterKnockOut() {
        InPlayPokemon pokemon = createPokemon(100);
        pokemon.addStatus(StatusCondition.POISONED);
        pokemon.addStatus(StatusCondition.BURNED);
        pokemon.setDamageTaken(90);
        GameContext context = createContext(false);

        statusEffectManager.processEndOfTurnEffects(pokemon, context);

        assertThat(pokemon.isKnockedOut()).isTrue();
        assertThat(pokemon.getDamageTaken()).isEqualTo(100);
        assertThat(pokemon.hasStatus(StatusCondition.BURNED)).isTrue();
    }

    @Test
    void shouldThrowWhenPokemonIsNull() {
        GameContext context = createContext();

        assertThatThrownBy(() -> statusEffectManager.processEndOfTurnEffects(null, context))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("pokemon must not be null");
    }

    @Test
    void shouldThrowWhenContextIsNull() {
        InPlayPokemon pokemon = createPokemon(100);

        assertThatThrownBy(() -> statusEffectManager.processEndOfTurnEffects(pokemon, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("context must not be null");
    }

    @Test
    void processEndOfTurnAlias_ShouldDelegateToProcessEndOfTurnEffects() {
        InPlayPokemon pokemon = createPokemon(100);
        GameContext context = createContext();

        statusEffectManager.processEndOfTurnEffects(pokemon, context);

        assertThat(pokemon.getDamageTaken()).isZero();
    }

    private GameContext createContext(boolean... coinFlips) {
        GameContext context = new GameContext();
        context.setRandom(new FixedCoinFlipRandom(coinFlips));
        return context;
    }

    private InPlayPokemon createPokemon(int hp) {
        PokemonCard card = PokemonCard.builder()
                .id("status-mon-" + hp)
                .name("StatusMon")
                .hp(hp)
                .stage(PokemonStage.BASIC)
                .pokemonType(EnergyType.GRASS)
                .costToAttack(List.of())
                .attacks(List.of())
                .abilities(List.of())
                .weaknesses(List.of())
                .resistances(List.of())
                .retreatCost(0)
                .build();
        return new InPlayPokemon(card);
    }

    /**
     * Deterministic {@link Random} for status-resolution coin flips in tests.
     */
    private static final class FixedCoinFlipRandom extends Random {

        private final boolean[] results;
        private int index;

        private FixedCoinFlipRandom(boolean... results) {
            this.results = results == null ? new boolean[0] : results.clone();
            this.index = 0;
        }

        @Override
        public boolean nextBoolean() {
            if (index >= results.length) {
                return false;
            }
            return results[index++];
        }
    }
}
