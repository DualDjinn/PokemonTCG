package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.dtos.engine.GameAction;
import ar.edu.utn.frc.tup.piii.models.effects.CardEffectRegistry;
import ar.edu.utn.frc.tup.piii.models.effects.AbilityRegistry;
import ar.edu.utn.frc.tup.piii.models.effects.TrainerEffectRegistry;
import ar.edu.utn.frc.tup.piii.models.Attack;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.PokemonStage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Focused unit tests for {@link TurnManager}.
 */
class TurnManagerTest {

    private TurnManager turnManager;

    @BeforeEach
    void setUp() {
        TriggeredAbilityDispatcher triggeredAbilityDispatcher = new TriggeredAbilityDispatcher();
        AttackResolutionPipeline attackResolutionPipeline = new AttackResolutionPipeline(
                new DamageCalculator(),
                new CardEffectRegistry(),
                new PassiveAbilityHandler(),
                triggeredAbilityDispatcher
        );
        turnManager = new TurnManager(
                new RuleValidator(),
                attackResolutionPipeline,
                triggeredAbilityDispatcher,
                new StatusEffectManager(),
                new VictoryConditionChecker(),
                new TrainerEffectRegistry(),
                new AbilityRegistry()
        );
    }

    @Test
    void submitAction_EndTurnWhenNextPlayerDeckIsEmpty_FinishesMatchForCurrentPlayer() {
        Battle match = createBattle();
        match.getContext().setDefenderDeck(new ArrayList<>());
        int previousDefenderHandSize = match.getContext().getDefenderHand().size();

        turnManager.submitAction(match, "player-one", GameAction.advanceToAttack());

        turnManager.submitAction(match, "player-one", GameAction.endTurn());

        assertThat(match.isFinished()).isTrue();
        assertThat(match.getWinnerPlayerId()).isEqualTo("player-one");
        assertThat(match.getCurrentTurnPlayerId()).isEqualTo("player-two");
        assertThat(match.getContext().getAttackerHand()).hasSize(previousDefenderHandSize);
    }

    @Test
    void submitAction_EndTurnWhenNextPlayerDeckHasCards_SwapsTurnWithoutFinishingMatch() {
        Battle match = createBattle();
        int previousDefenderHandSize = match.getContext().getDefenderHand().size();
        int previousDefenderDeckSize = match.getContext().getDefenderDeck().size();

        turnManager.submitAction(match, "player-one", GameAction.advanceToAttack());

        turnManager.submitAction(match, "player-one", GameAction.endTurn());

        assertThat(match.isFinished()).isFalse();
        assertThat(match.getWinnerPlayerId()).isNull();
        assertThat(match.getCurrentTurnPlayerId()).isEqualTo("player-two");
        assertThat(match.getContext().getAttackerHand()).hasSize(previousDefenderHandSize + 1);
        assertThat(match.getContext().getAttackerDeck()).hasSize(previousDefenderDeckSize - 1);
    }

    @Test
    void submitAction_AdvanceToAttackFromMain_ChangesPhaseWithoutEndingTurn() {
        Battle match = createBattle();

        turnManager.submitAction(match, "player-one", GameAction.advanceToAttack());

        assertThat(match.isFinished()).isFalse();
        assertThat(match.getCurrentTurnPlayerId()).isEqualTo("player-one");
        assertThat(match.getContext().getCurrentPhase()).isEqualTo(ar.edu.utn.frc.tup.piii.models.TurnPhase.ATTACK);
    }

    @Test
    void submitAction_EndTurnFromMain_AdvancesToAttackAndEndsTurn() {
        Battle match = createBattle();

        // END_TURN desde MAIN auto-avanza a ATTACK y termina el turno: el FE lo usa para
        // "terminar el turno sin atacar". El turno pasa al rival.
        turnManager.submitAction(match, "player-one", GameAction.endTurn());

        assertThat(match.isFinished()).isFalse();
        assertThat(match.getCurrentTurnPlayerId()).isEqualTo("player-two");
    }

    @Test
    void submitAction_AdvanceToAttackWhenAlreadyInAttackPhase_RejectsAction() {
        Battle match = createBattle();
        turnManager.submitAction(match, "player-one", GameAction.advanceToAttack());

        assertThatThrownBy(() -> turnManager.submitAction(match, "player-one", GameAction.advanceToAttack()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Attack phase has already started for the current turn");
    }

    private Battle createBattle() {
        Player playerOne = createPlayer("player-one");
        Player playerTwo = createPlayer("player-two");
        Battle match = new Battle("match-1", playerOne, playerTwo);
        match.setCurrentTurnPlayerId(playerOne.getId());
        match.setContext(createContext());
        return match;
    }

    private Player createPlayer(String playerId) {
        Player player = new Player();
        player.setId(playerId);
        player.setUsername(playerId);
        return player;
    }

    private GameContext createContext() {
        GameContext context = new GameContext();
        context.setAttacker(createPokemon("attacker", "Spark", 30));
        context.setDefender(createPokemon("defender", "Punch", 20));
        context.setAttackerBench(new ArrayList<>(List.of(createPokemon("bench-attacker", "Bench Hit", 10))));
        context.setDefenderBench(new ArrayList<>(List.of(createPokemon("bench-defender", "Bench Hit", 10))));
        context.setAttackerPrizes(new ArrayList<>(List.of(createCard("attacker-prize"))));
        context.setDefenderPrizes(new ArrayList<>(List.of(createCard("defender-prize"))));
        context.setAttackerDeck(new ArrayList<>(List.of(createCard("attacker-deck-card"))));
        context.setDefenderDeck(new ArrayList<>(List.of(createCard("defender-deck-card"))));
        context.setAttackerHand(new ArrayList<>());
        context.setDefenderHand(new ArrayList<>());
        context.setAttackerDiscard(new ArrayList<>());
        context.setDefenderDiscard(new ArrayList<>());
        return context;
    }

    private InPlayPokemon createPokemon(String id, String attackName, int damage) {
        Attack attack = Attack.builder()
                .name(attackName)
                .energyCost(Map.of())
                .baseDamage(damage)
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

    private Card createCard(String id) {
        return EnergyCard.builder()
                .id(id)
                .name(id)
                .energyType(EnergyType.COLORLESS)
                .build();
    }
}
