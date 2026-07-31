package ar.edu.utn.frc.tup.piii.dtos.engine;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GameActionTest {

    @Test
    void attackFactory_CreatesAttackCommandWithExpectedPayload() {
        GameAction action = GameAction.attack("Volt Tackle");

        assertThat(action.getType()).isEqualTo(GameActionType.ATTACK);
        assertThat(action.getAttackName()).isEqualTo("Volt Tackle");
    }

    @Test
    void endTurnFactory_CreatesEndTurnCommandWithoutAttackName() {
        GameAction action = GameAction.endTurn();

        assertThat(action.getType()).isEqualTo(GameActionType.END_TURN);
        assertThat(action.getAttackName()).isNull();
    }
}
