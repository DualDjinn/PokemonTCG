package ar.edu.utn.frc.tup.piii.engine.turnstates;

import ar.edu.utn.frc.tup.piii.dtos.engine.GameAction;
import ar.edu.utn.frc.tup.piii.dtos.engine.GameActionType;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for turn phase state dispatch: verifies that each state routes actions
 * to the correct {@link TurnPhaseActionContext} operation and rejects invalid inputs.
 */
class TurnPhaseStatesTest {

    // ---- MainPhaseState — valid action dispatch ----

    @Test
    void mainPhaseState_WhenAdvanceToAttackAction_DelegatesToAdvanceOperation() {
        RecordingPhaseActionContext context = new RecordingPhaseActionContext();
        MainPhaseState state = new MainPhaseState();

        state.handle(GameAction.advanceToAttack(), context);

        assertThat(context.advanceToAttackCalled).isTrue();
        assertThat(context.resolveAttackCalled).isFalse();
        assertThat(context.endTurnCalled).isFalse();
    }

    @Test
    void mainPhaseState_WhenPlayBasicAction_DelegatesToContext() {
        RecordingPhaseActionContext context = new RecordingPhaseActionContext();
        MainPhaseState state = new MainPhaseState();
        GameAction action = GameAction.playBasic("bulbasaur-1");

        state.handle(action, context);

        assertThat(context.playBasicCalled).isTrue();
        assertThat(context.lastActionPassed).isSameAs(action);
    }

    @Test
    void mainPhaseState_WhenEvolveAction_DelegatesToContext() {
        RecordingPhaseActionContext context = new RecordingPhaseActionContext();
        MainPhaseState state = new MainPhaseState();
        GameAction action = GameAction.evolve("ivysaur-1", "bulbasaur-1");

        state.handle(action, context);

        assertThat(context.evolveCalled).isTrue();
        assertThat(context.lastActionPassed).isSameAs(action);
    }

    @Test
    void mainPhaseState_WhenAttachEnergyAction_DelegatesToContext() {
        RecordingPhaseActionContext context = new RecordingPhaseActionContext();
        MainPhaseState state = new MainPhaseState();
        GameAction action = GameAction.attachEnergy("grass-energy-1", "bulbasaur-1");

        state.handle(action, context);

        assertThat(context.attachEnergyCalled).isTrue();
        assertThat(context.lastActionPassed).isSameAs(action);
    }

    @Test
    void mainPhaseState_WhenRetreatAction_DelegatesToContext() {
        RecordingPhaseActionContext context = new RecordingPhaseActionContext();
        MainPhaseState state = new MainPhaseState();
        GameAction action = new GameAction();
        action.setType(GameActionType.RETREAT);
        action.setCardId("pidgey-1");

        state.handle(action, context);

        assertThat(context.retreatCalled).isTrue();
        assertThat(context.lastActionPassed).isSameAs(action);
    }

    @Test
    void mainPhaseState_WhenPlayItemAction_DelegatesToContext() {
        RecordingPhaseActionContext context = new RecordingPhaseActionContext();
        MainPhaseState state = new MainPhaseState();
        GameAction action = GameAction.playItem("potion-1");

        state.handle(action, context);

        assertThat(context.playItemCalled).isTrue();
        assertThat(context.lastActionPassed).isSameAs(action);
    }

    @Test
    void mainPhaseState_WhenPlaySupporterAction_DelegatesToContext() {
        RecordingPhaseActionContext context = new RecordingPhaseActionContext();
        MainPhaseState state = new MainPhaseState();
        GameAction action = GameAction.playSupporter("professor-sycamore-1");

        state.handle(action, context);

        assertThat(context.playSupporterCalled).isTrue();
        assertThat(context.lastActionPassed).isSameAs(action);
    }

    @Test
    void mainPhaseState_WhenPlayStadiumAction_DelegatesToContext() {
        RecordingPhaseActionContext context = new RecordingPhaseActionContext();
        MainPhaseState state = new MainPhaseState();
        GameAction action = GameAction.playStadium("fairy-garden-1");

        state.handle(action, context);

        assertThat(context.playStadiumCalled).isTrue();
        assertThat(context.lastActionPassed).isSameAs(action);
    }

    @Test
    void mainPhaseState_WhenAttachToolAction_DelegatesToContext() {
        RecordingPhaseActionContext context = new RecordingPhaseActionContext();
        MainPhaseState state = new MainPhaseState();
        GameAction action = GameAction.attachPokemonTool("muscle-band-1", "bulbasaur-1");

        state.handle(action, context);

        assertThat(context.attachToolCalled).isTrue();
        assertThat(context.lastActionPassed).isSameAs(action);
    }

    @Test
    void mainPhaseState_WhenActivateAbilityAction_DelegatesToContext() {
        RecordingPhaseActionContext context = new RecordingPhaseActionContext();
        MainPhaseState state = new MainPhaseState();
        GameAction action = new GameAction();
        action.setType(GameActionType.ACTIVATE_ABILITY);
        action.setCardId("venusaur-1");
        action.setAttackName("Energy Trans");

        state.handle(action, context);

        assertThat(context.activateAbilityCalled).isTrue();
        assertThat(context.lastActionPassed).isSameAs(action);
    }

    @Test
    void mainPhaseState_WhenExecuteAttackAction_AdvancesToAttackThenResolves() {
        RecordingPhaseActionContext context = new RecordingPhaseActionContext();
        MainPhaseState state = new MainPhaseState();
        GameAction action = GameAction.executeAttack("Vine Whip", List.of("grass-1"));

        state.handle(action, context);

        assertThat(context.advanceToAttackCalled).isTrue();
        assertThat(context.resolveAttackCalled).isTrue();
        assertThat(context.lastAttackAction).isSameAs(action);
    }

    // ---- MainPhaseState — rejection edge cases ----

    @Test
    void mainPhaseState_WhenAttackAction_RejectsAction() {
        MainPhaseState state = new MainPhaseState();
        GameAction action = GameAction.attack("Spark");
        RecordingPhaseActionContext context = new RecordingPhaseActionContext();

        assertThatThrownBy(() -> state.handle(action, context))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Attack cannot be declared before entering the attack phase");
    }

    @Test
    void mainPhaseState_WhenUnsupportedActionType_ThrowsIllegalArgument() {
        MainPhaseState state = new MainPhaseState();
        GameAction action = new GameAction();
        action.setType(GameActionType.CHOOSE_COIN_SIDE);

        assertThatThrownBy(() -> state.handle(action, new RecordingPhaseActionContext()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported action type");
    }

    @Test
    void mainPhaseState_WhenNullAction_ThrowsNullPointer() {
        MainPhaseState state = new MainPhaseState();

        assertThatThrownBy(() -> state.handle(null, new RecordingPhaseActionContext()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void mainPhaseState_WhenNullContext_ThrowsNullPointer() {
        MainPhaseState state = new MainPhaseState();

        assertThatThrownBy(() -> state.handle(GameAction.advanceToAttack(), null))
                .isInstanceOf(NullPointerException.class);
    }

    // ---- AttackPhaseState — valid action dispatch ----

    @Test
    void attackPhaseState_WhenAttackAction_DelegatesToResolveAttackOperation() {
        RecordingPhaseActionContext context = new RecordingPhaseActionContext();
        AttackPhaseState state = new AttackPhaseState();
        GameAction attackAction = GameAction.attack("Spark");

        state.handle(attackAction, context);

        assertThat(context.resolveAttackCalled).isTrue();
        assertThat(context.lastAttackAction).isSameAs(attackAction);
        assertThat(context.advanceToAttackCalled).isFalse();
        assertThat(context.endTurnCalled).isFalse();
    }

    @Test
    void attackPhaseState_WhenExecuteAttackAction_DelegatesToResolveAttack() {
        RecordingPhaseActionContext context = new RecordingPhaseActionContext();
        AttackPhaseState state = new AttackPhaseState();
        GameAction action = GameAction.executeAttack("Spark", List.of("lightning-1"));

        state.handle(action, context);

        assertThat(context.resolveAttackCalled).isTrue();
        assertThat(context.lastAttackAction).isSameAs(action);
    }

    @Test
    void attackPhaseState_WhenEndTurnAction_DelegatesToEndTurnOperation() {
        RecordingPhaseActionContext context = new RecordingPhaseActionContext();
        AttackPhaseState state = new AttackPhaseState();

        state.handle(GameAction.endTurn(), context);

        assertThat(context.endTurnCalled).isTrue();
        assertThat(context.advanceToAttackCalled).isFalse();
        assertThat(context.resolveAttackCalled).isFalse();
    }

    // ---- AttackPhaseState — rejection edge cases ----

    @Test
    void attackPhaseState_WhenAdvanceToAttackAction_RejectsAction() {
        AttackPhaseState state = new AttackPhaseState();
        GameAction action = GameAction.advanceToAttack();
        RecordingPhaseActionContext context = new RecordingPhaseActionContext();

        assertThatThrownBy(() -> state.handle(action, context))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Attack phase has already started for the current turn");
    }

    @Test
    void attackPhaseState_WhenBoardActionType_ThrowsIllegalArgument() {
        AttackPhaseState state = new AttackPhaseState();
        GameAction action = GameAction.playBasic("bulbasaur-1");

        assertThatThrownBy(() -> state.handle(action, new RecordingPhaseActionContext()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported action type");
    }

    @Test
    void attackPhaseState_WhenNullAction_ThrowsNullPointer() {
        AttackPhaseState state = new AttackPhaseState();

        assertThatThrownBy(() -> state.handle(null, new RecordingPhaseActionContext()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void attackPhaseState_WhenNullContext_ThrowsNullPointer() {
        AttackPhaseState state = new AttackPhaseState();

        assertThatThrownBy(() -> state.handle(GameAction.attack("Spark"), null))
                .isInstanceOf(NullPointerException.class);
    }

    /**
     * Deterministic context stub that records which phase operation was called and with what action.
     */
    private static final class RecordingPhaseActionContext implements TurnPhaseActionContext {

        private boolean advanceToAttackCalled;
        private boolean resolveAttackCalled;
        private boolean endTurnCalled;
        private boolean playBasicCalled;
        private boolean evolveCalled;
        private boolean attachEnergyCalled;
        private boolean retreatCalled;
        private boolean playItemCalled;
        private boolean playSupporterCalled;
        private boolean playStadiumCalled;
        private boolean attachToolCalled;
        private boolean activateAbilityCalled;
        private GameAction lastAttackAction;
        private GameAction lastActionPassed;

        @Override
        public void playBasicPokemon(GameAction action) {
            this.playBasicCalled = true;
            this.lastActionPassed = action;
        }

        @Override
        public void evolvePokemon(GameAction action) {
            this.evolveCalled = true;
            this.lastActionPassed = action;
        }

        @Override
        public void attachEnergy(GameAction action) {
            this.attachEnergyCalled = true;
            this.lastActionPassed = action;
        }

        @Override
        public void playItem(GameAction action) {
            this.playItemCalled = true;
            this.lastActionPassed = action;
        }

        @Override
        public void playSupporter(GameAction action) {
            this.playSupporterCalled = true;
            this.lastActionPassed = action;
        }

        @Override
        public void playStadium(GameAction action) {
            this.playStadiumCalled = true;
            this.lastActionPassed = action;
        }

        @Override
        public void advanceToAttack() {
            this.advanceToAttackCalled = true;
        }

        @Override
        public void resolveAttack(GameAction action) {
            this.resolveAttackCalled = true;
            this.lastAttackAction = action;
        }

        @Override
        public void endTurn() {
            this.endTurnCalled = true;
        }

        @Override
        public void retreat(GameAction action) {
            this.retreatCalled = true;
            this.lastActionPassed = action;
        }

        @Override
        public void attachTool(GameAction action) {
            this.attachToolCalled = true;
            this.lastActionPassed = action;
        }

        @Override
        public void activateAbility(GameAction action) {
            this.activateAbilityCalled = true;
            this.lastActionPassed = action;
        }
    }
}
