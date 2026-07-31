package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.dtos.engine.GameAction;
import ar.edu.utn.frc.tup.piii.models.effects.CardEffectRegistry;
import ar.edu.utn.frc.tup.piii.models.effects.AbilityRegistry;
import ar.edu.utn.frc.tup.piii.models.effects.TrainerEffectRegistry;
import ar.edu.utn.frc.tup.piii.models.Attack;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.ItemCard;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.PokemonStage;
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
 * Tests for ownership validation and the rejection branches of the main-phase board actions in
 * {@link TurnManager}. These paths use the real engine collaborators because they never reach the
 * non-deterministic damage maths.
 */
class TurnManagerValidationTest {

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

    // ---- Ownership / lifecycle validation ----

    @Test
    void submitAction_WhenPlayerIdIsBlank_RejectsAction() {
        Battle match = createBattle();

        assertThatThrownBy(() -> turnManager.submitAction(match, "  ", GameAction.advanceToAttack()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Player id must not be blank");
    }

    @Test
    void submitAction_WhenPlayerDoesNotBelongToMatch_RejectsAction() {
        Battle match = createBattle();

        assertThatThrownBy(() -> turnManager.submitAction(match, "ghost", GameAction.advanceToAttack()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Player does not belong to match");
    }

    @Test
    void submitAction_WhenMatchIsFinished_RejectsAction() {
        Battle match = createBattle();
        match.finish("player-one");

        assertThatThrownBy(() -> turnManager.submitAction(match, "player-one", GameAction.advanceToAttack()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Match is already finished");
    }

    @Test
    void submitAction_WhenNotCurrentTurnPlayer_RejectsAction() {
        Battle match = createBattle();

        assertThatThrownBy(() -> turnManager.submitAction(match, "player-two", GameAction.advanceToAttack()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("It is not the turn of player");
    }

    @Test
    void submitAction_WhenWaitingForActiveSelection_RejectsAction() {
        Battle match = createBattle();
        match.markPendingActiveSelection("player-two");

        assertThatThrownBy(() -> turnManager.submitAction(match, "player-one", GameAction.advanceToAttack()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("waiting for active Pokemon selection");
    }

    @Test
    void submitAction_WhenActionTypeIsNull_RejectsAction() {
        Battle match = createBattle();
        GameAction action = new GameAction();

        assertThatThrownBy(() -> turnManager.submitAction(match, "player-one", action))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Action type must not be null");
    }

    @Test
    void submitAction_WhenMatchIsNull_RejectsAction() {
        assertThatThrownBy(() -> turnManager.submitAction(null, "player-one", GameAction.advanceToAttack()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("match must not be null");
    }

    @Test
    void submitAction_WhenActionIsNull_RejectsAction() {
        Battle match = createBattle();

        assertThatThrownBy(() -> turnManager.submitAction(match, "player-one", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("action must not be null");
    }

    @Test
    void submitAction_AdvanceToAttackWithoutActivePokemon_RejectsAction() {
        Battle match = createBattle();
        match.getContext().setDefender(null);

        assertThatThrownBy(() -> turnManager.submitAction(match, "player-one", GameAction.advanceToAttack()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Both players must have an active Pokemon");
    }

    // ---- Main-phase action rejections ----

    @Test
    void submitAction_PlayBasicWithBlankCardId_RejectsAction() {
        Battle match = createBattle();

        assertThatThrownBy(() -> turnManager.submitAction(match, "player-one", GameAction.playBasic("  ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Card id must not be blank");
    }

    @Test
    void submitAction_PlayBasicWhenCardNotInHand_RejectsAction() {
        Battle match = createBattle();

        assertThatThrownBy(() -> turnManager.submitAction(match, "player-one", GameAction.playBasic("missing")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Card not found in player hand");
    }

    @Test
    void submitAction_PlayBasicWhenBenchIsFull_RejectsAction() {
        Battle match = createBattle();
        GameContext context = match.getContext();
        for (int i = 0; i < 5; i++) {
            context.getAttackerBench().add(new InPlayPokemon(pokemonCard("bench-" + i, "Bench", PokemonStage.BASIC, null)));
        }
        context.getAttackerHand().add(pokemonCard("hand-basic", "Benchmon", PokemonStage.BASIC, null));

        assertThatThrownBy(() -> turnManager.submitAction(match, "player-one", GameAction.playBasic("hand-basic")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("A Basic Pokemon cannot be played right now");
    }

    @Test
    void submitAction_EvolveWithNonPokemonCard_RejectsAction() {
        Battle match = createBattle();
        match.getContext().getAttackerHand().add(energyCard("hand-energy"));

        assertThatThrownBy(() -> turnManager.submitAction(match, "player-one",
                GameAction.evolve("hand-energy", "attacker")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Selected card is not a Pokemon");
    }

    @Test
    void submitAction_EvolveWithBlankTargetId_RejectsAction() {
        Battle match = createBattle();
        match.getContext().getAttackerHand()
                .add(pokemonCard("hand-evo", "Evo", PokemonStage.STAGE_1, "attacker"));

        assertThatThrownBy(() -> turnManager.submitAction(match, "player-one",
                GameAction.evolve("hand-evo", "  ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Target card id must not be blank");
    }

    @Test
    void submitAction_EvolveWhenTargetNotInPlay_RejectsAction() {
        Battle match = createBattle();
        match.getContext().getAttackerHand()
                .add(pokemonCard("hand-evo", "Evo", PokemonStage.STAGE_1, "attacker"));

        assertThatThrownBy(() -> turnManager.submitAction(match, "player-one",
                GameAction.evolve("hand-evo", "not-in-play")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("In-play Pokemon not found");
    }

    @Test
    void submitAction_EvolveWithIllegalEvolution_RejectsAction() {
        Battle match = createBattle();
        // Evolution card evolves from a different species than the active Pokemon.
        match.getContext().getAttackerHand()
                .add(pokemonCard("hand-evo", "Evo", PokemonStage.STAGE_1, "someone-else"));

        assertThatThrownBy(() -> turnManager.submitAction(match, "player-one",
                GameAction.evolve("hand-evo", "attacker")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot evolve into");
    }

    @Test
    void submitAction_EvolveBenchedPokemon_ReplacesItOnBench() {
        Battle match = createBattle();
        GameContext context = match.getContext();
        InPlayPokemon benched = new InPlayPokemon(pokemonCard("bench-basic", "Basicmon", PokemonStage.BASIC, null));
        benched.setInstanceId("bench-basic");
        benched.setDamageTaken(20);
        context.getAttackerBench().add(benched);
        context.getAttackerHand()
                .add(pokemonCard("hand-evo", "Evo", PokemonStage.STAGE_1, "Basicmon"));

        turnManager.submitAction(match, "player-one", GameAction.evolve("hand-evo", "bench-basic"));

        assertThat(context.getAttackerBench()).hasSize(1);
        assertThat(context.getAttackerBench().get(0).getCard().getId()).isEqualTo("hand-evo");
        assertThat(context.getAttackerBench().get(0).getDamageTaken()).isEqualTo(20);
    }

    @Test
    void submitAction_AttachEnergyWithNonEnergyCard_RejectsAction() {
        Battle match = createBattle();
        match.getContext().getAttackerHand()
                .add(pokemonCard("hand-poke", "Poke", PokemonStage.BASIC, null));

        assertThatThrownBy(() -> turnManager.submitAction(match, "player-one",
                GameAction.attachEnergy("hand-poke", "attacker")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Selected card is not an Energy");
    }

    @Test
    void submitAction_AttachEnergyToBenchedPokemon_AttachesToBenchTarget() {
        Battle match = createBattle();
        GameContext context = match.getContext();
        InPlayPokemon benchTarget = new InPlayPokemon(pokemonCard("bench-basic", "Bench", PokemonStage.BASIC, null));
        benchTarget.setInstanceId("bench-basic");
        context.getAttackerBench().add(benchTarget);
        context.getAttackerHand().add(energyCard("hand-energy"));

        turnManager.submitAction(match, "player-one",
                GameAction.attachEnergy("hand-energy", "bench-basic"));

        assertThat(context.getAttackerBench().get(0).countEnergies(null)).isEqualTo(1);
    }

    @Test
    void submitAction_PlayItemWithNonItemCard_RejectsAction() {
        Battle match = createBattle();
        match.getContext().getAttackerHand()
                .add(SupporterCard.builder().id("supp").name("Shauna").build());

        assertThatThrownBy(() -> turnManager.submitAction(match, "player-one", GameAction.playItem("supp")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Selected card is not an Item");
    }

    @Test
    void submitAction_PlaySupporterWithNonSupporterCard_RejectsAction() {
        Battle match = createBattle();
        match.getContext().getAttackerHand()
                .add(ItemCard.builder().id("item").name("Potion").build());

        assertThatThrownBy(() -> turnManager.submitAction(match, "player-one", GameAction.playSupporter("item")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Selected card is not a Supporter");
    }

    @Test
    void submitAction_PlaySupporterAsPlayerTwoAfterRotation_UsesPlayerTwoIdentity() {
        Battle match = createBattle();
        // Rotate so player-two becomes the current attacker, exercising the currentPlayer() fallback.
        turnManager.submitAction(match, "player-one", GameAction.advanceToAttack());
        turnManager.submitAction(match, "player-one", GameAction.endTurn());
        match.getContext().getAttackerHand()
                .add(SupporterCard.builder().id("supp-two").name("Cassius").build());

        turnManager.submitAction(match, "player-two", GameAction.playSupporter("supp-two"));

        assertThat(match.getContext().getAttackerSupportersPlayedThisTurn()).isEqualTo(1);
        assertThat(match.getContext().getAttackerDiscard())
                .anyMatch(card -> card.getId().equals("supp-two"));
    }

    // ---- Attack-phase argument validation ----

    @Test
    void submitAction_AttackWithBlankName_RejectsAction() {
        Battle match = createBattle();
        match.getContext().setCurrentPhase(TurnPhase.ATTACK);

        assertThatThrownBy(() -> turnManager.submitAction(match, "player-one", GameAction.attack("  ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Attack name must not be blank");
    }

    @Test
    void submitAction_AttackWhenActiveHasNoAttacks_RejectsAction() {
        Battle match = createBattle();
        GameContext context = match.getContext();
        context.setCurrentPhase(TurnPhase.ATTACK);
        context.setAttacker(new InPlayPokemon(pokemonWithoutAttacks("no-attacks")));

        assertThatThrownBy(() -> turnManager.submitAction(match, "player-one", GameAction.attack("Tackle")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Active Pokemon has no attacks available");
    }

    @Test
    void submitAction_EvolveWhenBenchZoneIsNull_RejectsWithStateError() {
        Battle match = createBattle();
        GameContext context = match.getContext();
        context.setAttackerBench(null);
        context.getAttackerHand().add(pokemonCard("hand-evo", "Evo", PokemonStage.STAGE_1, "attacker"));

        assertThatThrownBy(() -> turnManager.submitAction(match, "player-one",
                GameAction.evolve("hand-evo", "not-active")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bench must not be null");
    }

    // ---- Fixtures ----

    private PokemonCard pokemonWithoutAttacks(String id) {
        return PokemonCard.builder()
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
        InPlayPokemon attackPoke = new InPlayPokemon(pokemonCard("attacker", "attacker", PokemonStage.BASIC, null));
        attackPoke.setInstanceId("attacker");
        gameContext.setAttacker(attackPoke);
        gameContext.setDefender(new InPlayPokemon(pokemonCard("defender", "defender", PokemonStage.BASIC, null)));
        gameContext.setAttackerBench(new ArrayList<>());
        gameContext.setDefenderBench(new ArrayList<>());
        gameContext.setAttackerPrizes(new ArrayList<>(List.of(energyCard("ap1"))));
        gameContext.setDefenderPrizes(new ArrayList<>(List.of(energyCard("dp1"))));
        gameContext.setAttackerDeck(new ArrayList<>(List.of(energyCard("attacker-deck-card"))));
        gameContext.setDefenderDeck(new ArrayList<>(List.of(energyCard("defender-deck-card"))));
        gameContext.setAttackerHand(new ArrayList<>());
        gameContext.setDefenderHand(new ArrayList<>());
        gameContext.setAttackerDiscard(new ArrayList<>());
        gameContext.setDefenderDiscard(new ArrayList<>());
        gameContext.setCurrentPhase(TurnPhase.MAIN);
        // Turno normal (no de apertura): permite evolucionar/atacar.
        gameContext.setFirstTurn(false);
        return gameContext;
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
