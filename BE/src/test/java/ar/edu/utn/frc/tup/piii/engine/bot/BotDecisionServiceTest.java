package ar.edu.utn.frc.tup.piii.engine.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frc.tup.piii.dtos.engine.GameAction;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.TurnPhase;

/**
 * Unit tests for {@link BotDecisionService}.
 *
 * <p>Behavioural tests use a real {@link GameContext} to ensure each difficulty produces a
 * legal action. Delegation tests replace the real strategies with mocks (via reflection over
 * the private {@code strategies} map) to prove that each difficulty routes to the right
 * strategy and that an unknown difficulty falls back to EASY.
 */
class BotDecisionServiceTest {

    private BotDecisionService service;
    private BotStrategy easy;
    private BotStrategy medium;
    private BotStrategy hard;
    private GameContext mockContext;

    @BeforeEach
    void setUp() throws Exception {
        service = new BotDecisionService();
        easy = mock(BotStrategy.class);
        medium = mock(BotStrategy.class);
        hard = mock(BotStrategy.class);
        mockContext = mock(GameContext.class);
    }

    @SuppressWarnings("unchecked")
    private Map<BotDifficulty, BotStrategy> strategies(BotDecisionService svc) throws Exception {
        Field field = BotDecisionService.class.getDeclaredField("strategies");
        field.setAccessible(true);
        return (Map<BotDifficulty, BotStrategy>) field.get(svc);
    }

    private void injectMockStrategies() throws Exception {
        Map<BotDifficulty, BotStrategy> map = strategies(service);
        map.put(BotDifficulty.EASY, easy);
        map.put(BotDifficulty.MEDIUM, medium);
        map.put(BotDifficulty.HARD, hard);
    }

    // ───────── default wiring ─────────

    @Test
    void constructor_registersOneStrategyPerDifficulty() throws Exception {
        Map<BotDifficulty, BotStrategy> map = strategies(new BotDecisionService());

        assertThat(map.get(BotDifficulty.EASY)).isInstanceOf(RandomBotStrategy.class);
        assertThat(map.get(BotDifficulty.MEDIUM)).isInstanceOf(GreedyBotStrategy.class);
        assertThat(map.get(BotDifficulty.HARD)).isInstanceOf(MinimaxBotStrategy.class);
    }

    // ───────── chooseAction — behavioural (real strategies) ─────────

    @Test
    void chooseAction_easy_returnsActionForMainPhase() {
        GameContext context = new GameContext();
        context.setCurrentPhase(TurnPhase.MAIN);

        assertThat(service.chooseAction(BotDifficulty.EASY, context)).isNotNull();
    }

    @Test
    void chooseAction_medium_returnsActionForMainPhase() {
        GameContext context = new GameContext();
        context.setCurrentPhase(TurnPhase.MAIN);

        assertThat(service.chooseAction(BotDifficulty.MEDIUM, context)).isNotNull();
    }

    @Test
    void chooseAction_hard_returnsActionForMainPhase() {
        GameContext context = new GameContext();
        context.setCurrentPhase(TurnPhase.MAIN);

        assertThat(service.chooseAction(BotDifficulty.HARD, context)).isNotNull();
    }

    @Test
    void chooseAction_nullDifficulty_returnsActionWithoutCrashing() {
        GameContext context = new GameContext();
        context.setCurrentPhase(TurnPhase.MAIN);

        assertThat(service.chooseAction(null, context)).isNotNull();
    }

    // ───────── chooseAction — delegation (mock strategies) ─────────

    @Test
    void chooseAction_easy_delegatesToEasyStrategyOnly() throws Exception {
        injectMockStrategies();
        GameAction action = mock(GameAction.class);
        when(easy.chooseAction(mockContext)).thenReturn(action);

        GameAction result = service.chooseAction(BotDifficulty.EASY, mockContext);

        assertThat(result).isSameAs(action);
        verify(easy).chooseAction(mockContext);
        verify(medium, never()).chooseAction(any());
        verify(hard, never()).chooseAction(any());
    }

    @Test
    void chooseAction_medium_delegatesToMediumStrategyOnly() throws Exception {
        injectMockStrategies();
        GameAction action = mock(GameAction.class);
        when(medium.chooseAction(mockContext)).thenReturn(action);

        GameAction result = service.chooseAction(BotDifficulty.MEDIUM, mockContext);

        assertThat(result).isSameAs(action);
        verify(medium).chooseAction(mockContext);
        verify(easy, never()).chooseAction(any());
        verify(hard, never()).chooseAction(any());
    }

    @Test
    void chooseAction_hard_delegatesToHardStrategyOnly() throws Exception {
        injectMockStrategies();
        GameAction action = mock(GameAction.class);
        when(hard.chooseAction(mockContext)).thenReturn(action);

        GameAction result = service.chooseAction(BotDifficulty.HARD, mockContext);

        assertThat(result).isSameAs(action);
        verify(hard).chooseAction(mockContext);
        verify(easy, never()).chooseAction(any());
        verify(medium, never()).chooseAction(any());
    }

    @Test
    void chooseAction_nullDifficulty_fallsBackToEasyStrategy() throws Exception {
        injectMockStrategies();
        GameAction action = mock(GameAction.class);
        when(easy.chooseAction(mockContext)).thenReturn(action);

        GameAction result = service.chooseAction(null, mockContext);

        assertThat(result).isSameAs(action);
        verify(easy).chooseAction(mockContext);
        verify(medium, never()).chooseAction(any());
        verify(hard, never()).chooseAction(any());
    }
}
