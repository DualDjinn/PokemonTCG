package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.dtos.engine.GameAction;
import ar.edu.utn.frc.tup.piii.models.Attack;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.ItemCard;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.PokemonStage;
import ar.edu.utn.frc.tup.piii.models.TurnPhase;
import ar.edu.utn.frc.tup.piii.models.effects.AbilityRegistry;
import ar.edu.utn.frc.tup.piii.models.effects.TrainerEffectRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Mockito-driven tests for the attack-phase, knockout and victory branches of {@link TurnManager}.
 *
 * <p>The four engine collaborators are mocked so each branch — successful attack, knockout prize
 * awarding, deck-out/total-knockout victory and turn rotation — can be exercised deterministically
 * without depending on the real damage maths or rule timing.
 */
@ExtendWith(MockitoExtension.class)
class TurnManagerAttackResolutionTest {

    private static final String ATTACK_NAME = "Spark";

    @Mock
    private RuleValidator ruleValidator;
    @Mock
    private AttackResolutionPipeline attackResolutionPipeline;
    @Mock
    private StatusEffectManager statusEffectManager;
    @Mock
    private VictoryConditionChecker victoryConditionChecker;

    private TurnManager turnManager;

    @BeforeEach
    void setUp() {
        turnManager = new TurnManager(
                ruleValidator,
                attackResolutionPipeline,
                new TriggeredAbilityDispatcher(),
                statusEffectManager,
                victoryConditionChecker,
                new TrainerEffectRegistry(),
                new AbilityRegistry()
        );
    }

    @Test
    void resolveAttack_WhenAttackResolvesWithoutKnockout_RotatesIntoNextPlayerMainPhase() {
        Battle match = matchInAttackPhase();
        when(ruleValidator.canAttack(any(), any(), any())).thenReturn(true);

        turnManager.submitAction(match, "player-one", GameAction.attack(ATTACK_NAME));

        verify(attackResolutionPipeline).resolve(any(GameContext.class), any(Attack.class));
        verify(statusEffectManager, times(2)).processEndOfTurnEffects(any(), any());
        assertThat(match.isFinished()).isFalse();
        assertThat(match.getCurrentTurnPlayerId()).isEqualTo("player-two");
        assertThat(match.getContext().getCurrentPhase()).isEqualTo(TurnPhase.MAIN);
    }

    @Test
    void resolveAttack_WhenRuleValidatorRejectsAttack_ThrowsAndDoesNotResolve() {
        Battle match = matchInAttackPhase();
        when(ruleValidator.canAttack(any(), any(), any())).thenReturn(false);

        assertThatThrownBy(() ->
                turnManager.submitAction(match, "player-one", GameAction.attack(ATTACK_NAME)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Attack cannot be used right now");

        verify(attackResolutionPipeline, never()).resolve(any(), any());
    }

    @Test
    void resolveAttack_WhenAttackNameDoesNotExist_ThrowsBeforeValidation() {
        Battle match = matchInAttackPhase();

        assertThatThrownBy(() ->
                turnManager.submitAction(match, "player-one", GameAction.attack("Unknown")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Attack not found on active Pokemon");
    }

    @Test
    void resolveAttack_WhenDefenderIsKnockedOutWithBenchReplacement_PrizesPendingAndPromptsActiveSelection() {
        Battle match = matchInAttackPhase();
        GameContext context = match.getContext();
        context.getDefenderBench().add(basicPokemon("defender-bench"));
        List<Card> defenderDiscardBefore = context.getDefenderDiscard();
        when(ruleValidator.canAttack(any(), any(), any())).thenReturn(true);
        knockOutDefenderOnResolve(context);

        turnManager.submitAction(match, "player-one", GameAction.attack(ATTACK_NAME));

        assertThat(match.isFinished()).isFalse();
        assertThat(match.isPendingPrizeSelection()).isTrue();
        assertThat(defenderDiscardBefore)
                .anyMatch(card -> card.getId().equals("defender"));
        assertThat(match.requiresActiveSelection("player-two")).isTrue();
    }

    @Test
    void resolveAttack_WhenDefenderKnockoutTakesLastPrize_FinishesMatchForAttacker() {
        Battle match = matchInAttackPhase();
        GameContext context = match.getContext();
        context.setAttackerPrizes(new ArrayList<>(List.of(energyCard("only-prize"))));
        when(ruleValidator.canAttack(any(), any(), any())).thenReturn(true);
        knockOutDefenderOnResolve(context);

        turnManager.submitAction(match, "player-one", GameAction.attack(ATTACK_NAME));

        assertThat(match.isFinished()).isTrue();
        assertThat(match.getWinnerPlayerId()).isEqualTo("player-one");
    }

    @Test
    void resolveAttack_WhenDefenderKnockoutLeavesEmptyBench_FinishesMatchForAttacker() {
        Battle match = matchInAttackPhase();
        GameContext context = match.getContext();
        // Bench already empty in the fixture and prizes still available -> win by total knockout.
        when(ruleValidator.canAttack(any(), any(), any())).thenReturn(true);
        knockOutDefenderOnResolve(context);

        turnManager.submitAction(match, "player-one", GameAction.attack(ATTACK_NAME));

        assertThat(match.isFinished()).isTrue();
        assertThat(match.getWinnerPlayerId()).isEqualTo("player-one");
    }

    @Test
    void resolveAttack_WhenAttackerKnocksItselfOut_PrizesPendingAndPromptsActiveSelection() {
        Battle match = matchInAttackPhase();
        GameContext context = match.getContext();
        context.getAttackerBench().add(basicPokemon("attacker-bench"));
        context.getAttacker().setTool(ItemCard.builder().id("attached-tool").name("Tool").build());
        context.getAttacker().getEnergies().add(energyCard("attacker-energy"));
        List<Card> attackerDiscardBefore = context.getAttackerDiscard();
        when(ruleValidator.canAttack(any(), any(), any())).thenReturn(true);
        doAnswer(invocation -> {
            context.getAttacker().setDamageTaken(context.getAttacker().getCard().getHp());
            return null;
        }).when(attackResolutionPipeline).resolve(any(), any());

        turnManager.submitAction(match, "player-one", GameAction.attack(ATTACK_NAME));

        // The defender (player-two) has a pending prize selection and the KO'd card + tool + energy
        // land in the attacker discard pile.
        assertThat(match.isPendingPrizeSelection()).isTrue();
        assertThat(match.isPendingPrizeSelectionForPlayer("player-two")).isTrue();
        assertThat(attackerDiscardBefore)
                .anyMatch(card -> card.getId().equals("attacker"))
                .anyMatch(card -> card.getId().equals("attached-tool"))
                .anyMatch(card -> card.getId().equals("attacker-energy"));
        assertThat(match.requiresActiveSelection("player-one")).isTrue();
    }

    @Test
    void resolveAttack_WhenVictoryCheckerReportsPlayerTwoWins_FinishesWithoutEnteringBetweenTurns() {
        Battle match = matchInAttackPhase();
        when(ruleValidator.canAttack(any(), any(), any())).thenReturn(true);
        when(victoryConditionChecker.evaluateAfterAction(match)).thenReturn(MatchResult.PLAYER_TWO_WINS);

        turnManager.submitAction(match, "player-one", GameAction.attack(ATTACK_NAME));

        assertThat(match.isFinished()).isTrue();
        assertThat(match.getWinnerPlayerId()).isEqualTo("player-two");
        assertThat(match.getContext().getCurrentPhase()).isEqualTo(TurnPhase.ATTACK);
        verify(statusEffectManager, never()).processEndOfTurnEffects(any(), any());
    }

    @Test
    void resolveAttack_WhenVictoryCheckerReportsPlayerOneWins_FinishesForPlayerOne() {
        Battle match = matchInAttackPhase();
        when(ruleValidator.canAttack(any(), any(), any())).thenReturn(true);
        when(victoryConditionChecker.evaluateAfterAction(match)).thenReturn(MatchResult.PLAYER_ONE_WINS);

        turnManager.submitAction(match, "player-one", GameAction.attack(ATTACK_NAME));

        assertThat(match.isFinished()).isTrue();
        assertThat(match.getWinnerPlayerId()).isEqualTo("player-one");
    }

    @Test
    void endTurn_WhenNoVictory_ProcessesBothActivesAndStartsOpponentTurn() {
        Battle match = matchInAttackPhase();

        turnManager.submitAction(match, "player-one", GameAction.endTurn());

        verify(statusEffectManager, times(2)).processEndOfTurnEffects(any(), any());
        assertThat(match.getCurrentTurnPlayerId()).isEqualTo("player-two");
        assertThat(match.getContext().getCurrentPhase()).isEqualTo(TurnPhase.MAIN);
    }

    @Test
    void beginNextTurn_WhenTurnStartReportsDeckOut_FinishesBeforeDrawing() {
        Battle match = matchInAttackPhase();
        when(victoryConditionChecker.evaluateTurnStart(match)).thenReturn(MatchResult.PLAYER_ONE_WINS);

        turnManager.submitAction(match, "player-one", GameAction.endTurn());

        assertThat(match.isFinished()).isTrue();
        assertThat(match.getWinnerPlayerId()).isEqualTo("player-one");
        // The turn already rotated before the deck-out evaluation finished the match.
        assertThat(match.getCurrentTurnPlayerId()).isEqualTo("player-two");
    }

    // ---- Fixtures ----

    private Battle matchInAttackPhase() {
        Battle match = createBattle();
        match.getContext().setCurrentPhase(TurnPhase.ATTACK);
        return match;
    }

    private void knockOutDefenderOnResolve(GameContext context) {
        doAnswer(invocation -> {
            context.getDefender().setDamageTaken(context.getDefender().getCard().getHp());
            return null;
        }).when(attackResolutionPipeline).resolve(any(), any());
    }

    private Battle createBattle() {
        Player playerOne = player("player-one");
        Player playerTwo = player("player-two");
        Battle match = new Battle("match-1", playerOne, playerTwo);
        match.setCurrentTurnPlayerId(playerOne.getId());
        match.setContext(context());
        return match;
    }

    private Player player(String playerId) {
        Player player = new Player();
        player.setId(playerId);
        player.setUsername(playerId);
        return player;
    }

    private GameContext context() {
        GameContext gameContext = new GameContext();
        gameContext.setAttacker(basicPokemon("attacker"));
        gameContext.setDefender(basicPokemon("defender"));
        gameContext.setAttackerBench(new ArrayList<>());
        gameContext.setDefenderBench(new ArrayList<>());
        gameContext.setAttackerPrizes(new ArrayList<>(List.of(energyCard("ap1"), energyCard("ap2"))));
        gameContext.setDefenderPrizes(new ArrayList<>(List.of(energyCard("dp1"), energyCard("dp2"))));
        gameContext.setAttackerDeck(new ArrayList<>(List.of(energyCard("attacker-deck-card"))));
        gameContext.setDefenderDeck(new ArrayList<>(List.of(energyCard("defender-deck-card"))));
        gameContext.setAttackerHand(new ArrayList<>());
        gameContext.setDefenderHand(new ArrayList<>());
        gameContext.setAttackerDiscard(new ArrayList<>());
        gameContext.setDefenderDiscard(new ArrayList<>());
        return gameContext;
    }

    private InPlayPokemon basicPokemon(String id) {
        Attack attack = Attack.builder()
                .name(ATTACK_NAME)
                .energyCost(Map.of())
                .baseDamage(10)
                .build();
        PokemonCard card = PokemonCard.builder()
                .id(id)
                .name(id)
                .hp(100)
                .stage(PokemonStage.BASIC)
                .pokemonType(EnergyType.LIGHTNING)
                .costToAttack(List.of())
                .attacks(List.of(attack))
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
}
