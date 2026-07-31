package ar.edu.utn.frc.tup.piii.engine.turnstates;

import ar.edu.utn.frc.tup.piii.dtos.engine.GameAction;
import ar.edu.utn.frc.tup.piii.dtos.engine.GameActionType;
import ar.edu.utn.frc.tup.piii.engine.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.PokemonStage;
import ar.edu.utn.frc.tup.piii.models.StatusCondition;
import ar.edu.utn.frc.tup.piii.models.TurnPhase;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Lifecycle and contract tests for the four turn-phase state objects: phase identity, valid action
 * sets, {@code enter}/{@code exit} hooks and the actions each state rejects.
 */
class TurnPhaseStateLifecycleTest {

    // ---- DrawPhaseState ----

    @Test
    void drawPhaseState_Enter_DrawsTopCardForCurrentPlayer() {
        DrawPhaseState state = new DrawPhaseState();
        GameContext context = new GameContext();
        // En el turno de apertura no se roba; este test simula un turno normal.
        context.setFirstTurn(false);
        context.setAttackerDeck(new ArrayList<>(List.of(energyCard("top"), energyCard("next"))));
        context.setAttackerHand(new ArrayList<>());

        state.enter(context);

        assertThat(context.getAttackerHand()).extracting("id").containsExactly("top");
        assertThat(context.getAttackerDeck()).hasSize(1);
    }

    @Test
    void drawPhaseState_EnterWithEmptyDeck_ThrowsInconsistentState() {
        DrawPhaseState state = new DrawPhaseState();
        GameContext context = new GameContext();
        // En el turno de apertura no se roba; este test simula un turno normal con deck vacío.
        context.setFirstTurn(false);
        context.setAttackerDeck(new ArrayList<>());
        context.setAttackerHand(new ArrayList<>());

        assertThatThrownBy(() -> state.enter(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("should have drawn a card");
    }

    @Test
    void drawPhaseState_Handle_RejectsAnyAction() {
        DrawPhaseState state = new DrawPhaseState();

        assertThatThrownBy(() -> state.handle(GameAction.advanceToAttack(), new NoOpActionContext()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No actions can be submitted during the draw phase");
    }

    @Test
    void drawPhaseState_Contract_ExposesDrawPhaseAndNoValidActions() {
        DrawPhaseState state = new DrawPhaseState();

        assertThat(state.getPhase()).isEqualTo(TurnPhase.DRAW);
        assertThat(state.getValidActions()).isEmpty();
        assertThatThrownBy(() -> state.enter(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> state.exit(null)).isInstanceOf(NullPointerException.class);
    }

    // ---- BetweenTurnsPhaseState ----

    @Test
    void betweenTurnsPhaseState_Enter_ProcessesEndOfTurnStatusesOnBothActives() {
        BetweenTurnsPhaseState state = new BetweenTurnsPhaseState(new StatusEffectManager());
        GameContext context = new GameContext();
        InPlayPokemon attacker = pokemon("attacker");
        InPlayPokemon defender = pokemon("defender");
        attacker.applyStatus(StatusCondition.POISONED);
        defender.applyStatus(StatusCondition.POISONED);
        context.setAttacker(attacker);
        context.setDefender(defender);

        state.enter(context);

        assertThat(attacker.getDamageTaken()).isEqualTo(10);
        assertThat(defender.getDamageTaken()).isEqualTo(10);
    }

    @Test
    void betweenTurnsPhaseState_EnterWithoutActives_DoesNotFail() {
        BetweenTurnsPhaseState state = new BetweenTurnsPhaseState(new StatusEffectManager());
        GameContext context = new GameContext();
        context.setAttacker(null);
        context.setDefender(null);

        state.enter(context);

        assertThat(context.getAttacker()).isNull();
        assertThat(context.getDefender()).isNull();
    }

    @Test
    void betweenTurnsPhaseState_Handle_RejectsAnyAction() {
        BetweenTurnsPhaseState state = new BetweenTurnsPhaseState(new StatusEffectManager());

        assertThatThrownBy(() -> state.handle(GameAction.endTurn(), new NoOpActionContext()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No actions can be submitted during the between-turns phase");
    }

    @Test
    void betweenTurnsPhaseState_Constructor_RejectsNullManager() {
        assertThatThrownBy(() -> new BetweenTurnsPhaseState(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("statusEffectManager must not be null");
    }

    @Test
    void betweenTurnsPhaseState_Contract_ExposesPhaseAndNoValidActions() {
        BetweenTurnsPhaseState state = new BetweenTurnsPhaseState(new StatusEffectManager());

        assertThat(state.getPhase()).isEqualTo(TurnPhase.BETWEEN_TURNS);
        assertThat(state.getValidActions()).isEmpty();
        assertThatThrownBy(() -> state.enter(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> state.exit(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void betweenTurnsPhaseState_ExitWithValidContext_CompletesWithoutSideEffects() {
        BetweenTurnsPhaseState state = new BetweenTurnsPhaseState(new StatusEffectManager());
        GameContext context = new GameContext();
        context.setCurrentPhase(TurnPhase.BETWEEN_TURNS);

        state.exit(context);

        // exit() is a no-op guard: it must not mutate the phase it is leaving.
        assertThat(context.getCurrentPhase()).isEqualTo(TurnPhase.BETWEEN_TURNS);
    }

    // ---- MainPhaseState ----

    @Test
    void mainPhaseState_HandleEndTurn_AdvancesToAttackThenEndsTurn() {
        MainPhaseState state = new MainPhaseState();

        // END_TURN desde MAIN no se rechaza: auto-avanza a ATTACK y termina el turno
        // (el FE lo usa para "terminar turno sin atacar"). El bridge no-op no lanza.
        assertThatCode(() -> state.handle(GameAction.endTurn(), new NoOpActionContext()))
                .doesNotThrowAnyException();
    }

    @Test
    void mainPhaseState_Contract_ExposesPhaseAndBoardActions() {
        MainPhaseState state = new MainPhaseState();

        assertThat(state.getPhase()).isEqualTo(TurnPhase.MAIN);
        assertThat(state.getValidActions()).containsExactlyInAnyOrder(
                GameActionType.PLAY_BASIC,
                GameActionType.EVOLVE,
                GameActionType.ATTACH_ENERGY,
                GameActionType.RETREAT,
                GameActionType.PLAY_ITEM,
                GameActionType.PLAY_SUPPORTER,
                GameActionType.PLAY_STADIUM,
                GameActionType.ATTACH_TOOL,
                GameActionType.ACTIVATE_ABILITY,
                GameActionType.ADVANCE_TO_ATTACK
        );
        assertThatThrownBy(() -> state.enter(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> state.exit(null)).isInstanceOf(NullPointerException.class);
    }

    // ---- AttackPhaseState ----

    @Test
    void attackPhaseState_HandleBoardAction_RejectsUnsupportedAction() {
        AttackPhaseState state = new AttackPhaseState();

        assertThatThrownBy(() -> state.handle(GameAction.playBasic("card"), new NoOpActionContext()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported action type");
    }

    @Test
    void attackPhaseState_Contract_ExposesPhaseAndAttackActions() {
        AttackPhaseState state = new AttackPhaseState();

        assertThat(state.getPhase()).isEqualTo(TurnPhase.ATTACK);
        assertThat(state.getValidActions()).containsExactlyInAnyOrder(
                GameActionType.ATTACK,
                GameActionType.END_TURN
        );
        assertThatThrownBy(() -> state.enter(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> state.exit(null)).isInstanceOf(NullPointerException.class);
    }

    // ---- Helpers ----

    private InPlayPokemon pokemon(String id) {
        PokemonCard card = PokemonCard.builder()
                .id(id)
                .name(id)
                .hp(100)
                .stage(PokemonStage.BASIC)
                .pokemonType(EnergyType.LIGHTNING)
                .costToAttack(List.of())
                .attacks(List.of())
                .abilities(List.of())
                .weaknesses(List.of())
                .resistances(List.of())
                .retreatCost(0)
                .build();
        return new InPlayPokemon(card);
    }

    private EnergyCard energyCard(String id) {
        return EnergyCard.builder()
                .id(id)
                .name(id)
                .energyType(EnergyType.COLORLESS)
                .build();
    }

    /**
     * No-op bridge used by handle()-rejection tests where the action never reaches the context.
     */
    private static final class NoOpActionContext implements TurnPhaseActionContext {
        @Override
        public void playBasicPokemon(GameAction action) {
            // Never invoked: these tests assert the state rejects the action first.
        }

        @Override
        public void evolvePokemon(GameAction action) {
            // Never invoked.
        }

        @Override
        public void attachEnergy(GameAction action) {
            // Never invoked.
        }

        @Override
        public void playItem(GameAction action) {
            // Never invoked.
        }

        @Override
        public void playSupporter(GameAction action) {
            // Never invoked.
        }

        @Override
        public void playStadium(GameAction action) {
            // Never invoked.
        }

        @Override
        public void advanceToAttack() {
            // Never invoked.
        }

        @Override
        public void resolveAttack(GameAction action) {
            // Never invoked.
        }

        @Override
        public void endTurn() {
            // Never invoked.
        }

        @Override
        public void retreat(GameAction action) {
            // Never invoked.
        }

        @Override
        public void attachTool(GameAction action) {
            // Never invoked.
        }

        @Override
        public void activateAbility(GameAction action) {
            // Never invoked.
        }
    }
}
