package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.dtos.engine.GameAction;
import ar.edu.utn.frc.tup.piii.models.effects.CardEffectRegistry;
import ar.edu.utn.frc.tup.piii.models.effects.AbilityRegistry;
import ar.edu.utn.frc.tup.piii.models.effects.TrainerEffectRegistry;
import ar.edu.utn.frc.tup.piii.models.Attack;
// import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.ItemCard;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.PokemonStage;
import ar.edu.utn.frc.tup.piii.models.StatusCondition;
import ar.edu.utn.frc.tup.piii.models.SupporterCard;
import ar.edu.utn.frc.tup.piii.models.TurnPhase;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the main-phase board actions and turn rotation handled by {@link TurnManager}.
 */
class TurnManagerMainPhaseTest {

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
    void submitAction_PlayBasic_MovesCardFromHandToBench() {
        Battle match = createBattle();
        GameContext context = match.getContext();
        PokemonCard basic = pokemonCard("hand-basic", "Benchmon", PokemonStage.BASIC, null);
        context.getAttackerHand().add(basic);

        turnManager.submitAction(match, "player-one", GameAction.playBasic("hand-basic"));

        assertThat(context.getAttackerHand()).isEmpty();
        assertThat(context.getAttackerBench())
                .anyMatch(p -> p.getCard().getId().equals("hand-basic"));
        assertThat(context.getAttackerPokemonPlayedThisTurn()).contains(basic);
    }

    @Test
    void submitAction_PlayBasicWithNonBasicCard_RejectsAction() {
        Battle match = createBattle();
        match.getContext().getAttackerHand()
                .add(pokemonCard("hand-stage1", "Evomon", PokemonStage.STAGE_1, "Benchmon"));

        assertThatThrownBy(() ->
                turnManager.submitAction(match, "player-one", GameAction.playBasic("hand-stage1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not a Basic Pokemon");
    }

    @Test
    void submitAction_Evolve_ReplacesActiveAndKeepsDamageAndEnergy() {
        Battle match = createBattle();
        GameContext context = match.getContext();
        InPlayPokemon active = new InPlayPokemon(
                pokemonCard("active-basic", "Basicmon", PokemonStage.BASIC, null));
        active.setInstanceId("active-basic");
        active.setDamageTaken(30);
        active.getEnergies().add(energyCard("attached-energy"));
        context.setAttacker(active);
        context.getAttackerHand()
                .add(pokemonCard("hand-evolution", "Stagemon", PokemonStage.STAGE_1, "Basicmon"));

        turnManager.submitAction(match, "player-one",
                GameAction.evolve("hand-evolution", "active-basic"));

        InPlayPokemon evolved = context.getAttacker();
        assertThat(evolved.getCard().getId()).isEqualTo("hand-evolution");
        assertThat(evolved.getDamageTaken()).isEqualTo(30);
        assertThat(evolved.countEnergies(null)).isEqualTo(1);
        assertThat(context.getAttackerHand()).isEmpty();
    }

    @Test
    void submitAction_AttachEnergy_AttachesToTargetAndConsumesAttachment() {
        Battle match = createBattle();
        GameContext context = match.getContext();
        context.getAttackerHand().add(energyCard("hand-energy"));

        turnManager.submitAction(match, "player-one",
                GameAction.attachEnergy("hand-energy", "attacker"));

        assertThat(context.getAttacker().countEnergies(null)).isEqualTo(1);
        assertThat(context.getAttackerHand()).isEmpty();
        assertThat(context.getAttackerEnergyAttachmentsThisTurn()).isEqualTo(1);
    }

    @Test
    void submitAction_AttachEnergyTwice_RejectsSecondAttachment() {
        Battle match = createBattle();
        GameContext context = match.getContext();
        context.getAttackerHand().add(energyCard("energy-1"));
        context.getAttackerHand().add(energyCard("energy-2"));

        turnManager.submitAction(match, "player-one",
                GameAction.attachEnergy("energy-1", "attacker"));

        assertThatThrownBy(() -> turnManager.submitAction(match, "player-one",
                GameAction.attachEnergy("energy-2", "attacker")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Energy cannot be attached");
    }

    @Test
    void submitAction_PlayItem_MovesCardToDiscard() {
        Battle match = createBattle();
        GameContext context = match.getContext();
        context.getAttackerHand().add(ItemCard.builder().id("hand-item").name("Potion").build());

        turnManager.submitAction(match, "player-one", GameAction.playItem("hand-item"));

        assertThat(context.getAttackerHand()).isEmpty();
        assertThat(context.getAttackerDiscard())
                .anyMatch(card -> card.getId().equals("hand-item"));
    }

    @Test
    void submitAction_PlaySupporterTwice_RejectsSecondSupporter() {
        Battle match = createBattle();
        GameContext context = match.getContext();
        context.getAttackerHand().add(SupporterCard.builder().id("supp-1").name("Shauna").build());
        context.getAttackerHand().add(SupporterCard.builder().id("supp-2").name("Cassius").build());

        turnManager.submitAction(match, "player-one", GameAction.playSupporter("supp-1"));

        assertThat(context.getAttackerSupportersPlayedThisTurn()).isEqualTo(1);
        assertThatThrownBy(() ->
                turnManager.submitAction(match, "player-one", GameAction.playSupporter("supp-2")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Supporter cannot be played");
    }

    @Test
    void submitAction_MainPhaseActionDuringAttackPhase_RejectsAction() {
        Battle match = createBattle();
        match.getContext().getAttackerHand()
                .add(pokemonCard("hand-basic", "Benchmon", PokemonStage.BASIC, null));
        turnManager.submitAction(match, "player-one", GameAction.advanceToAttack());

        assertThatThrownBy(() ->
                turnManager.submitAction(match, "player-one", GameAction.playBasic("hand-basic")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported action type");
    }

    @Test
    void submitAction_EndTurn_ProcessesBetweenTurnsStatusesAndStartsNextDrawPhase() {
        Battle match = createBattle();
        GameContext context = match.getContext();
        InPlayPokemon poisoned = context.getAttacker();
        poisoned.applyStatus(StatusCondition.POISONED);
        int previousDefenderHandSize = context.getDefenderHand().size();

        turnManager.submitAction(match, "player-one", GameAction.advanceToAttack());
        turnManager.submitAction(match, "player-one", GameAction.endTurn());

        assertThat(poisoned.getDamageTaken()).isEqualTo(10);
        assertThat(match.getCurrentTurnPlayerId()).isEqualTo("player-two");
        assertThat(context.getCurrentPhase()).isEqualTo(TurnPhase.MAIN);
        assertThat(context.getAttackerHand()).hasSize(previousDefenderHandSize + 1);
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
        // Turno normal (no de apertura): permite evolucionar/atacar.
        gameContext.setFirstTurn(false);
        gameContext.setAttacker(activePokemon("attacker"));
        gameContext.getAttacker().setInstanceId("attacker");
        gameContext.setDefender(activePokemon("defender"));
        gameContext.setAttackerBench(new ArrayList<>(java.util.Collections.nCopies(5, (InPlayPokemon) null)));
        gameContext.setDefenderBench(new ArrayList<>(java.util.Collections.nCopies(5, (InPlayPokemon) null)));
        gameContext.setAttackerPrizes(new ArrayList<>(List.of(energyCard("attacker-prize"))));
        gameContext.setDefenderPrizes(new ArrayList<>(List.of(energyCard("defender-prize"))));
        gameContext.setAttackerDeck(new ArrayList<>(List.of(energyCard("attacker-deck-card"))));
        gameContext.setDefenderDeck(new ArrayList<>(List.of(energyCard("defender-deck-card"))));
        gameContext.setAttackerHand(new ArrayList<>());
        gameContext.setDefenderHand(new ArrayList<>());
        gameContext.setAttackerDiscard(new ArrayList<>());
        gameContext.setDefenderDiscard(new ArrayList<>());
        return gameContext;
    }

    private InPlayPokemon activePokemon(String id) {
        return new InPlayPokemon(pokemonCard(id, id, PokemonStage.BASIC, null));
    }

    private PokemonCard pokemonCard(String id, String name, PokemonStage stage, String evolvesFrom) {
        Attack attack = Attack.builder()
                .name("Tackle")
                .energyCost(Map.of())
                .baseDamage(10)
                .build();
        return PokemonCard.builder()
                .id(id)
                .name(name)
                .hp(100)
                .stage(stage)
                .evolvesFrom(evolvesFrom)
                .pokemonType(EnergyType.LIGHTNING)
                .costToAttack(List.of())
                .attacks(List.of(attack))
                .abilities(List.of())
                .weaknesses(List.of())
                .resistances(List.of())
                .retreatCost(0)
                .build();
    }

    private EnergyCard energyCard(String id) {
        return EnergyCard.builder()
                .id(id)
                .name(id)
                .energyType(EnergyType.COLORLESS)
                .build();
    }
}
