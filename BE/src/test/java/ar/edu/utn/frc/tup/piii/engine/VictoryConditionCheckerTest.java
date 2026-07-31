package ar.edu.utn.frc.tup.piii.engine;

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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link VictoryConditionChecker}.
 */
class VictoryConditionCheckerTest {

    private final VictoryConditionChecker victoryConditionChecker = new VictoryConditionChecker();

    @Test
    void evaluateAfterAction_WhenCurrentPlayerHasNoPrizeCardsLeft_ReturnsCurrentPlayerWin() {
        Battle match = createBattle("player-one");
        match.getContext().setAttackerPrizes(new ArrayList<>());

        MatchResult result = victoryConditionChecker.evaluateAfterAction(match);

        assertThat(result).isEqualTo(MatchResult.PLAYER_ONE_WINS);
    }

    @Test
    void evaluateAfterAction_WhenOpponentHasNoPrizeCardsLeftDuringPlayerTwoTurn_ReturnsPlayerOneWin() {
        Battle match = createBattle("player-two");
        match.getContext().setDefenderPrizes(new ArrayList<>());

        MatchResult result = victoryConditionChecker.evaluateAfterAction(match);

        assertThat(result).isEqualTo(MatchResult.PLAYER_ONE_WINS);
    }

    @Test
    void evaluateAfterAction_WhenOpponentHasNoActivePokemonAndNoBench_ReturnsCurrentPlayerWin() {
        Battle match = createBattle("player-one");
        match.getContext().setDefender(null);
        match.getContext().setDefenderBench(new ArrayList<>());

        MatchResult result = victoryConditionChecker.evaluateAfterAction(match);

        assertThat(result).isEqualTo(MatchResult.PLAYER_ONE_WINS);
    }

    @Test
    void evaluateAfterAction_WhenCurrentPlayerHasNoActivePokemonAndNoBench_ReturnsOpponentWin() {
        Battle match = createBattle("player-one");
        match.getContext().setAttacker(null);
        match.getContext().setAttackerBench(new ArrayList<>());

        MatchResult result = victoryConditionChecker.evaluateAfterAction(match);

        assertThat(result).isEqualTo(MatchResult.PLAYER_TWO_WINS);
    }

    @Test
    void evaluateAfterAction_WhenPrizeAndKnockoutConditionsCoexist_PrioritizesPrizeCards() {
        Battle match = createBattle("player-two");
        match.getContext().setAttackerPrizes(new ArrayList<>());
        match.getContext().setDefender(null);
        match.getContext().setDefenderBench(new ArrayList<>());

        MatchResult result = victoryConditionChecker.evaluateAfterAction(match);

        assertThat(result).isEqualTo(MatchResult.PLAYER_TWO_WINS);
    }

    @Test
    void evaluateTurnStart_WhenCurrentTurnPlayerDeckIsEmpty_ReturnsOpponentWin() {
        Battle match = createBattle("player-one");
        match.getContext().setAttackerDeck(new ArrayList<>());

        MatchResult result = victoryConditionChecker.evaluateTurnStart(match);

        assertThat(result).isEqualTo(MatchResult.PLAYER_TWO_WINS);
    }

    @Test
    void evaluateTurnStart_WhenImmediateVictoryAlreadyExists_DoesNotOverrideItWithDeckOut() {
        Battle match = createBattle("player-two");
        match.getContext().setAttackerPrizes(new ArrayList<>());
        match.getContext().setAttackerDeck(new ArrayList<>());

        MatchResult result = victoryConditionChecker.evaluateTurnStart(match);

        assertThat(result).isEqualTo(MatchResult.PLAYER_TWO_WINS);
    }

    @Test
    void evaluateAfterAction_WhenOpponentHasNoBenchButHasPokemonInHand_ReturnsCurrentPlayerWin() {
        Battle match = createBattle("player-one");
        match.getContext().setDefender(null);
        match.getContext().setDefenderBench(new ArrayList<>());
        PokemonCard basicInHand = PokemonCard.builder()
                .id("hand-basic").name("Charmander").hp(60)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.FIRE)
                .costToAttack(List.of()).attacks(List.of()).abilities(List.of())
                .weaknesses(List.of()).resistances(List.of()).retreatCost(0).build();
        match.getContext().setDefenderHand(new ArrayList<>(List.of(basicInHand)));

        MatchResult result = victoryConditionChecker.evaluateAfterAction(match);

        assertThat(result).isEqualTo(MatchResult.PLAYER_ONE_WINS);
    }

    @Test
    void evaluateAfterAction_WhenOpponentHasNoActivePokemonNoBench_ReturnsCurrentPlayerWin() {
        Battle match = createBattle("player-one");
        match.getContext().setDefender(null);
        match.getContext().setDefenderBench(new ArrayList<>());

        MatchResult result = victoryConditionChecker.evaluateAfterAction(match);

        assertThat(result).isEqualTo(MatchResult.PLAYER_ONE_WINS);
    }

    @Test
    void evaluateAfterAction_WhenCurrentPlayerHasNoBenchButHasPokemonInHand_ReturnsOpponentWin() {
        Battle match = createBattle("player-one");
        match.getContext().setAttacker(null);
        match.getContext().setAttackerBench(new ArrayList<>());
        PokemonCard basicInHand = PokemonCard.builder()
                .id("hand-basic").name("Squirtle").hp(60)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.WATER)
                .costToAttack(List.of()).attacks(List.of()).abilities(List.of())
                .weaknesses(List.of()).resistances(List.of()).retreatCost(0).build();
        match.getContext().setAttackerHand(new ArrayList<>(List.of(basicInHand)));

        MatchResult result = victoryConditionChecker.evaluateAfterAction(match);

        assertThat(result).isEqualTo(MatchResult.PLAYER_TWO_WINS);
    }

    @Test
    void evaluateAfterAction_WhenNoVictoryConditionIsMet_ReturnsNoWinnerYet() {
        Battle match = createBattle("player-one");

        MatchResult result = victoryConditionChecker.evaluateAfterAction(match);

        assertThat(result).isEqualTo(MatchResult.NO_WINNER_YET);
    }

    @Test
    void evaluateAfterAction_WhenMatchIsNull_ThrowsMeaningfulError() {
        assertThatThrownBy(() -> victoryConditionChecker.evaluateAfterAction(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("match must not be null");
    }

    @Test
    void evaluateTurnStart_WhenContextIsNull_ThrowsMeaningfulError() {
        Player playerOne = createPlayer("player-one");
        Player playerTwo = createPlayer("player-two");
        Battle match = new Battle("match-1", playerOne, playerTwo);
        match.setCurrentTurnPlayerId(playerOne.getId());

        assertThatThrownBy(() -> victoryConditionChecker.evaluateTurnStart(match))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("match context must not be null");
    }

    private Battle createBattle(String currentTurnPlayerId) {
        Player playerOne = createPlayer("player-one");
        Player playerTwo = createPlayer("player-two");
        Battle match = new Battle("match-1", playerOne, playerTwo);
        match.setCurrentTurnPlayerId(currentTurnPlayerId);
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
        context.setAttacker(createPokemon("attacker"));
        context.setDefender(createPokemon("defender"));
        context.setAttackerBench(new ArrayList<>(List.of(createPokemon("bench-attacker"))));
        context.setDefenderBench(new ArrayList<>(List.of(createPokemon("bench-defender"))));
        context.setAttackerPrizes(new ArrayList<>(List.of(createPrizeCard("attacker-prize"))));
        context.setDefenderPrizes(new ArrayList<>(List.of(createPrizeCard("defender-prize"))));
        context.setAttackerDeck(new ArrayList<>(List.of(createPrizeCard("attacker-deck-card"))));
        context.setDefenderDeck(new ArrayList<>(List.of(createPrizeCard("defender-deck-card"))));
        context.setPrizesDealt(true);
        return context;
    }

    private InPlayPokemon createPokemon(String id) {
        PokemonCard card = PokemonCard.builder()
                .id(id)
                .name(id)
                .hp(100)
                .stage(PokemonStage.BASIC)
                .pokemonType(EnergyType.COLORLESS)
                .costToAttack(List.of())
                .attacks(List.of())
                .abilities(List.of())
                .weaknesses(List.of())
                .resistances(List.of())
                .retreatCost(0)
                .build();
        return new InPlayPokemon(card);
    }

    private Card createPrizeCard(String id) {
        return EnergyCard.builder()
                .id(id)
                .name(id)
                .energyType(EnergyType.COLORLESS)
                .build();
    }
}
