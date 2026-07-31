package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class BattleTest {

    private Player playerOne;
    private Player playerTwo;
    private Battle match;

    @BeforeEach
    void setUp() {
        playerOne = new Player();
        playerOne.setId("p1");
        playerOne.setUsername("ash");

        playerTwo = new Player();
        playerTwo.setId("p2");
        playerTwo.setUsername("misty");

        match = new Battle("match-1", playerOne, playerTwo);
    }

    @Test
    void constructor_InitializesFields() {
        assertThat(match.getId()).isEqualTo("match-1");
        assertThat(match.getPlayerOne()).isEqualTo(playerOne);
        assertThat(match.getPlayerTwo()).isEqualTo(playerTwo);
        assertThat(match.isFinished()).isFalse();
        assertThat(match.getPendingActiveSelections()).isEmpty();
    }

    @Test
    void constructor_NullId_ThrowsNullPointerException() {
        assertThatThrownBy(() -> new Battle(null, playerOne, playerTwo))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_NullPlayerOne_ThrowsNullPointerException() {
        assertThatThrownBy(() -> new Battle("id", null, playerTwo))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_NullPlayerTwo_ThrowsNullPointerException() {
        assertThatThrownBy(() -> new Battle("id", playerOne, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void containsPlayer_WithPlayerOne_ReturnsTrue() {
        assertThat(match.containsPlayer("p1")).isTrue();
    }

    @Test
    void containsPlayer_WithPlayerTwo_ReturnsTrue() {
        assertThat(match.containsPlayer("p2")).isTrue();
    }

    @Test
    void containsPlayer_WithUnknownPlayer_ReturnsFalse() {
        assertThat(match.containsPlayer("unknown")).isFalse();
    }

    @Test
    void isCurrentTurnPlayer_WithCurrentPlayer_ReturnsTrue() {
        match.setCurrentTurnPlayerId("p1");
        assertThat(match.isCurrentTurnPlayer("p1")).isTrue();
    }

    @Test
    void isCurrentTurnPlayer_WithOtherPlayer_ReturnsFalse() {
        match.setCurrentTurnPlayerId("p1");
        assertThat(match.isCurrentTurnPlayer("p2")).isFalse();
    }

    @Test
    void isCurrentTurnPlayer_WithNullCurrent_ReturnsFalse() {
        assertThat(match.isCurrentTurnPlayer("p1")).isFalse();
    }

    @Test
    void getOpponentPlayerId_ForPlayerOne_ReturnsPlayerTwo() {
        assertThat(match.getOpponentPlayerId("p1")).isEqualTo("p2");
    }

    @Test
    void getOpponentPlayerId_ForPlayerTwo_ReturnsPlayerOne() {
        assertThat(match.getOpponentPlayerId("p2")).isEqualTo("p1");
    }

    @Test
    void getOpponentPlayerId_ForUnknownPlayer_ThrowsException() {
        assertThatThrownBy(() -> match.getOpponentPlayerId("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Player does not belong to match");
    }

    @Test
    void isWaitingForActiveSelection_WithPendingSelections_ReturnsTrue() {
        match.markPendingActiveSelection("p1");
        assertThat(match.isWaitingForActiveSelection()).isTrue();
    }

    @Test
    void isWaitingForActiveSelection_WithNoPendingSelections_ReturnsFalse() {
        assertThat(match.isWaitingForActiveSelection()).isFalse();
    }

    @Test
    void markAndClearPendingActiveSelection() {
        match.markPendingActiveSelection("p1");
        assertThat(match.requiresActiveSelection("p1")).isTrue();

        match.clearPendingActiveSelection("p1");
        assertThat(match.requiresActiveSelection("p1")).isFalse();
    }

    @Test
    void finish_SetsWinnerAndFinished() {
        match.finish("p1");
        assertThat(match.isFinished()).isTrue();
        assertThat(match.getWinnerPlayerId()).isEqualTo("p1");
    }

    @Test
    void getActiveForPlayer_ForCurrentTurnPlayer_ReturnsAttacker() {
        InPlayPokemon attacker = mock(InPlayPokemon.class);
        InPlayPokemon defender = mock(InPlayPokemon.class);
        GameContext context = new GameContext();
        context.setAttacker(attacker);
        context.setDefender(defender);
        match.setContext(context);
        match.setCurrentTurnPlayerId("p1");

        assertThat(match.getActiveForPlayer("p1")).isEqualTo(attacker);
    }

    @Test
    void getActiveForPlayer_ForDefenderPlayer_ReturnsDefender() {
        InPlayPokemon attacker = mock(InPlayPokemon.class);
        InPlayPokemon defender = mock(InPlayPokemon.class);
        GameContext context = new GameContext();
        context.setAttacker(attacker);
        context.setDefender(defender);
        match.setContext(context);
        match.setCurrentTurnPlayerId("p1");

        assertThat(match.getActiveForPlayer("p2")).isEqualTo(defender);
    }

    @Test
    void swapTurn_SwapsContext() {
        InPlayPokemon attacker = mock(InPlayPokemon.class);
        InPlayPokemon defender = mock(InPlayPokemon.class);
        InPlayPokemon benchMon = mock(InPlayPokemon.class);
        List<InPlayPokemon> attackerBench = new java.util.ArrayList<>(List.of(benchMon));
        List<InPlayPokemon> defenderBench = new java.util.ArrayList<>(List.of(mock(InPlayPokemon.class)));
        // List<ar.edu.utn.frc.tup.piii.models.Card> empty = new java.util.ArrayList<>();

        GameContext context = new GameContext();
        context.setAttacker(attacker);
        context.setDefender(defender);
        context.setAttackerBench(attackerBench);
        context.setDefenderBench(defenderBench);
        context.setAttackerHand(new java.util.ArrayList<>());
        context.setDefenderHand(new java.util.ArrayList<>());
        context.setAttackerDeck(new java.util.ArrayList<>());
        context.setDefenderDeck(new java.util.ArrayList<>());
        context.setAttackerDiscard(new java.util.ArrayList<>());
        context.setDefenderDiscard(new java.util.ArrayList<>());
        context.setAttackerPrizes(new java.util.ArrayList<>());
        context.setDefenderPrizes(new java.util.ArrayList<>());
        match.setContext(context);
        match.setCurrentTurnPlayerId("p1");

        match.swapTurn();

        assertThat(match.getCurrentTurnPlayerId()).isEqualTo("p2");
        assertThat(match.getContext().getAttacker()).isEqualTo(defender);
        assertThat(match.getContext().getDefender()).isEqualTo(attacker);
    }

    @Test
    void swapTurn_ResetsTurnFlagsOnActive() {
        InPlayPokemon attacker = mock(InPlayPokemon.class);
        InPlayPokemon defender = mock(InPlayPokemon.class);
        GameContext context = new GameContext();
        context.setAttacker(attacker);
        context.setDefender(defender);
        context.setAttackerBench(List.of());
        context.setDefenderBench(List.of());
        match.setContext(context);
        match.setCurrentTurnPlayerId("p1");

        match.swapTurn();

        verify(defender).resetTurnFlags();
        verify(attacker, never()).resetTurnFlags();
    }
}
