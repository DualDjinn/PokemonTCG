package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.dtos.engine.GameAction;
import ar.edu.utn.frc.tup.piii.engine.events.AbilityActivatedGameEvent;
import ar.edu.utn.frc.tup.piii.engine.events.TrainerEffectGameEvent;
import ar.edu.utn.frc.tup.piii.engine.turnstates.AttackPhaseState;
import ar.edu.utn.frc.tup.piii.engine.turnstates.BetweenTurnsPhaseState;
import ar.edu.utn.frc.tup.piii.engine.turnstates.DrawPhaseState;
import ar.edu.utn.frc.tup.piii.engine.turnstates.MainPhaseState;
import ar.edu.utn.frc.tup.piii.engine.turnstates.TurnPhaseActionContext;
import ar.edu.utn.frc.tup.piii.engine.turnstates.TurnPhaseState;
import ar.edu.utn.frc.tup.piii.dtos.engine.GameActionType;
import ar.edu.utn.frc.tup.piii.models.Attack;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.ItemCard;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.PokemonToolCard;
import ar.edu.utn.frc.tup.piii.models.StadiumCard;
import ar.edu.utn.frc.tup.piii.models.SupporterCard;
import ar.edu.utn.frc.tup.piii.models.Ability;
import ar.edu.utn.frc.tup.piii.models.AbilityEffect;
import ar.edu.utn.frc.tup.piii.models.AbilityType;
import ar.edu.utn.frc.tup.piii.models.TrainerEffect;
import ar.edu.utn.frc.tup.piii.models.TurnPhase;
import ar.edu.utn.frc.tup.piii.models.effects.AbilityRegistry;
import ar.edu.utn.frc.tup.piii.models.effects.TrainerEffectRegistry;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Heart of the per-turn flow: a finite-state machine over the four {@link TurnPhase} values.
 *
 * <p>The manager keeps the current phase (held inside {@link GameContext}) and the active player
 * (held inside {@link Battle}) and drives the fixed progression
 * {@code DRAW → MAIN → ATTACK → BETWEEN_TURNS}. Phase-specific routing lives in the
 * {@link TurnPhaseState} objects (GoF <em>State</em> pattern); the manager owns the actual engine
 * mutations and the cross-aggregate orchestration — board actions, attack resolution, knockouts,
 * victory checks and rotating the turn into the next player's draw phase.
 *
 * <p>The class has no Spring or transport dependencies.
 */
class TurnManager {

    // private static final int MAX_BENCH_SIZE = 5;

    private final RuleValidator ruleValidator;
    private final AttackResolutionPipeline attackResolutionPipeline;
    private final TriggeredAbilityDispatcher triggeredAbilityDispatcher;
    private final VictoryConditionChecker victoryConditionChecker;
    private final TrainerEffectRegistry trainerEffectRegistry;
    private final AbilityRegistry abilityRegistry;
    private final Map<TurnPhase, TurnPhaseState> phaseStates;

    TurnManager(RuleValidator ruleValidator,
                AttackResolutionPipeline attackResolutionPipeline,
                TriggeredAbilityDispatcher triggeredAbilityDispatcher,
                StatusEffectManager statusEffectManager,
                VictoryConditionChecker victoryConditionChecker,
                TrainerEffectRegistry trainerEffectRegistry,
                AbilityRegistry abilityRegistry) {
        this.ruleValidator = ruleValidator;
        this.attackResolutionPipeline = attackResolutionPipeline;
        this.triggeredAbilityDispatcher = triggeredAbilityDispatcher;
        this.victoryConditionChecker = victoryConditionChecker;
        this.trainerEffectRegistry = trainerEffectRegistry;
        this.abilityRegistry = abilityRegistry;
        this.phaseStates = Map.of(
                TurnPhase.DRAW, new DrawPhaseState(),
                TurnPhase.MAIN, new MainPhaseState(),
                TurnPhase.ATTACK, new AttackPhaseState(),
                TurnPhase.BETWEEN_TURNS, new BetweenTurnsPhaseState(statusEffectManager)
        );
    }

    /**
     * Resolves one submitted action for the current player.
     *
     * @param match match being advanced
     * @param playerId acting player identifier
     * @param action action to execute
     */
    void submitAction(Battle match, String playerId, GameAction action) {
        Objects.requireNonNull(match, "match must not be null");
        Objects.requireNonNull(action, "action must not be null");

        validateActionOwnership(match, playerId);

        if (action.getType() == null) {
            throw new IllegalArgumentException("Action type must not be null");
        }

        GameContext context = requireMatchContext(match);
        context.setAttackerPlayerId(match.getCurrentTurnPlayerId());

        // RESOLVE_PLAYER_CHOICE bypasses the phase FSM — it resumes a paused action.
        if (action.getType() == GameActionType.RESOLVE_PLAYER_CHOICE) {
            resolvePlayerChoice(match, playerId, action);
            return;
        }

        // Block other actions while the player has a pending choice.
        if (context.getPendingPlayerChoice() != null
                && playerId.equals(context.getPendingPlayerChoice().getPlayerId())) {
            throw new IllegalArgumentException(
                    "A previous action is awaiting a player choice; resolve it before submitting new actions");
        }

        currentPhaseState(match).handle(action, new PhaseActionContext(match));
    }

    private void resolvePlayerChoice(Battle match, String playerId, GameAction action) {
        GameContext context = requireMatchContext(match);
        ar.edu.utn.frc.tup.piii.engine.choice.PendingPlayerChoice choice = context.getPendingPlayerChoice();
        if (choice == null) {
            throw new IllegalArgumentException("No player choice is currently pending");
        }
        if (!choice.getPlayerId().equals(playerId)) {
            throw new IllegalArgumentException("This player choice belongs to a different player");
        }
        List<String> chosen = action.getChosenCardIds() == null ? List.of() : action.getChosenCardIds();
        if (chosen.size() < choice.getMinPick() || chosen.size() > choice.getMaxPick()) {
            throw new IllegalArgumentException(
                    "Picked " + chosen.size() + " cards; expected between "
                            + choice.getMinPick() + " and " + choice.getMaxPick());
        }
        // Clear before invoking so the callback can raise a follow-up choice.
        context.setPendingPlayerChoice(null);
        choice.getCallback().accept(chosen);
    }

    // ---- Phase transitions ----

    private void transitionTo(Battle match, TurnPhase nextPhase) {
        GameContext context = requireMatchContext(match);
        TurnPhaseState currentState = phaseStates.get(context.getCurrentPhase());
        if (currentState != null) {
            currentState.exit(context);
        }
        context.setCurrentPhase(nextPhase);
        phaseStates.get(nextPhase).enter(context);
    }

    private void advanceToAttackPhase(Battle match) {
        GameContext context = requireMatchContext(match);
        requireActivePokemon(context);
        requirePhase(context, TurnPhase.MAIN, "Attack phase can only start from the main phase");
        transitionTo(match, TurnPhase.ATTACK);
    }

    // ---- Main-phase board actions ----

    private void playBasicPokemon(Battle match, GameAction action) {
        GameContext context = requireMatchContext(match);
        Card handCard = findHandCard(context.getAttackerHand(), requireCardId(action.getCardId()));
        if (!(handCard instanceof PokemonCard pokemonCard) || !pokemonCard.isBasic()) {
            throw new IllegalArgumentException("Selected card is not a Basic Pokemon: " + handCard.getId());
        }
        if (!ruleValidator.canPlayBasicPokemon(currentPlayer(match), context)) {
            throw new IllegalArgumentException("A Basic Pokemon cannot be played right now");
        }

        context.getAttackerHand().remove(handCard);
        List<InPlayPokemon> bench = context.getAttackerBench();
        for (int i = 0; i < bench.size(); i++) {
            if (bench.get(i) == null) {
                bench.set(i, new InPlayPokemon(pokemonCard));
                break;
            }
        }
        context.getAttackerPokemonPlayedThisTurn().add(pokemonCard);
        context.addLogEntry(currentPlayer(match).getId(), "PLAY_BASIC",
                "Jugó a " + pokemonCard.getName() + " en la banca");
    }

    private void evolvePokemon(Battle match, GameAction action) {
        GameContext context = requireMatchContext(match);
        Card handCard = findHandCard(context.getAttackerHand(), requireCardId(action.getCardId()));
        if (!(handCard instanceof PokemonCard evolutionCard)) {
            throw new IllegalArgumentException("Selected card is not a Pokemon: " + handCard.getId());
        }

        InPlayPokemon target = findOwnInPlayPokemon(context, requireTargetId(action.getTargetCardId()));
        if (context.getAttackerEvolvedThisTurn().contains(target.getInstanceId())) {
            throw new IllegalArgumentException(
                "Cannot evolve: " + target.getCard().getName() + " already evolved this turn");
        }
        if (!ruleValidator.canEvolve(target.getCard(), evolutionCard, context)) {
            throw new IllegalArgumentException("Cannot evolve into " + evolutionCard.getName());
        }

        InPlayPokemon evolved = evolve(target, evolutionCard);
        replaceInPlayPokemon(context, target, evolved);
        context.getAttackerHand().remove(handCard);
        context.getAttackerPokemonPlayedThisTurn().add(evolutionCard);
        context.getAttackerEvolvedThisTurn().add(target.getInstanceId());
        context.addLogEntry(currentPlayer(match).getId(), "EVOLVE",
                evolutionCard.getName() + " evolucionó de " + target.getCard().getName());
    }

    private void attachEnergy(Battle match, GameAction action) {
        GameContext context = requireMatchContext(match);
        Card handCard = findHandCard(context.getAttackerHand(), requireCardId(action.getCardId()));
        if (!(handCard instanceof EnergyCard energyCard)) {
            throw new IllegalArgumentException("Selected card is not an Energy: " + handCard.getId());
        }

        InPlayPokemon target = findOwnInPlayPokemon(context, requireTargetId(action.getTargetCardId()));
        if (!ruleValidator.canAttachEnergy(currentPlayer(match), energyCard, target, context)) {
            throw new IllegalArgumentException("Energy cannot be attached right now");
        }

        context.getAttackerHand().remove(handCard);
        target.getEnergies().add(energyCard);
        context.setAttackerEnergyAttachmentsThisTurn(context.getAttackerEnergyAttachmentsThisTurn() + 1);
        context.addLogEntry(currentPlayer(match).getId(), "ATTACH_ENERGY",
                "Energía " + energyCard.getEnergyType() + " asignada a " + target.getCard().getName());
    }

    private void playItem(Battle match, GameAction action) {
        GameContext context = requireMatchContext(match);
        Card handCard = findHandCard(context.getAttackerHand(), requireCardId(action.getCardId()));
        if (!(handCard instanceof ItemCard itemCard)) {
            throw new IllegalArgumentException("Selected card is not an Item: " + handCard.getId());
        }
        if (!ruleValidator.canPlayItem(currentPlayer(match), itemCard, context)) {
            throw new IllegalArgumentException("An Item cannot be played right now");
        }

        context.getAttackerHand().remove(handCard);
        context.getAttackerDiscard().add(handCard);
        context.addLogEntry(currentPlayer(match).getId(), "PLAY_ITEM",
                "Usó " + itemCard.getName());

        TrainerEffect effect = trainerEffectRegistry.getTrainerEffect(handCard.getId());
        if (effect != null) {
            effect.apply(context);
        }
        context.recordEvent(new TrainerEffectGameEvent(handCard.getId(), handCard.getName()));
    }

    private void playSupporter(Battle match, GameAction action) {
        GameContext context = requireMatchContext(match);
        Card handCard = findHandCard(context.getAttackerHand(), requireCardId(action.getCardId()));
        if (!(handCard instanceof SupporterCard supporterCard)) {
            throw new IllegalArgumentException("Selected card is not a Supporter: " + handCard.getId());
        }
        if (!ruleValidator.canUseSupporterCard(currentPlayer(match), supporterCard, context)) {
            throw new IllegalArgumentException("A Supporter cannot be played right now");
        }

        context.getAttackerHand().remove(handCard);
        context.getAttackerDiscard().add(handCard);
        context.setAttackerSupportersPlayedThisTurn(context.getAttackerSupportersPlayedThisTurn() + 1);
        context.addLogEntry(currentPlayer(match).getId(), "PLAY_SUPPORTER",
                "Usó " + supporterCard.getName());

        TrainerEffect effect = trainerEffectRegistry.getTrainerEffect(handCard.getId());
        if (effect != null) {
            effect.apply(context);
        }
        context.recordEvent(new TrainerEffectGameEvent(handCard.getId(), handCard.getName()));
    }

    private void playStadium(Battle match, GameAction action) {
        GameContext context = requireMatchContext(match);
        Card handCard = findHandCard(context.getAttackerHand(), requireCardId(action.getCardId()));
        if (!(handCard instanceof StadiumCard stadiumCard)) {
            throw new IllegalArgumentException("Selected card is not a Stadium: " + handCard.getId());
        }
        if (!ruleValidator.canPlayStadium(currentPlayer(match), stadiumCard, context)) {
            throw new IllegalArgumentException("A Stadium cannot be played right now");
        }

        context.getAttackerHand().remove(handCard);

        StadiumCard previousStadium = context.getActiveStadium();
        if (previousStadium != null) {
            context.getAttackerDiscard().add(previousStadium);
        }
        context.setActiveStadium(stadiumCard);

        TrainerEffect effect = trainerEffectRegistry.getTrainerEffect(handCard.getId());
        if (effect != null) {
            effect.apply(context);
        }
        context.recordEvent(new TrainerEffectGameEvent(handCard.getId(), handCard.getName()));
    }

    private void attachTool(Battle match, GameAction action) {
        GameContext context = requireMatchContext(match);
        Card handCard = findHandCard(context.getAttackerHand(), requireCardId(action.getCardId()));
        if (!(handCard instanceof PokemonToolCard toolCard)) {
            throw new IllegalArgumentException("Selected card is not a Pokemon Tool: " + handCard.getId());
        }

        InPlayPokemon target = findOwnInPlayPokemon(context, requireTargetId(action.getTargetCardId()));
        if (!ruleValidator.canAttachTool(currentPlayer(match), toolCard, target, context)) {
            throw new IllegalArgumentException("A Pokemon Tool cannot be attached right now");
        }

        context.getAttackerHand().remove(handCard);
        target.setTool(toolCard);
    }

    private void activateAbility(Battle match, GameAction action) {
        GameContext context = requireMatchContext(match);
        String instanceId = requireCardId(action.getCardId());
        String abilityName = Objects.requireNonNull(action.getAttackName(),
                "abilityName (attackName field) must not be null");

        InPlayPokemon holder = findOwnInPlayPokemon(context, instanceId);
        if (holder.getCard() == null) {
            throw new IllegalArgumentException("Holder has no card: " + instanceId);
        }

        List<Ability> abilities = holder.getCard().getAbilities();
        int abilityIndex = -1;
        Ability target = null;
        for (int i = 0; i < abilities.size(); i++) {
            if (abilityName.equals(abilities.get(i).getName())) {
                abilityIndex = i;
                target = abilities.get(i);
                break;
            }
        }
        if (target == null) {
            throw new IllegalArgumentException("Ability not found: " + abilityName);
        }
        if (target.getType() != AbilityType.ACTIVATED) {
            throw new IllegalArgumentException("Ability is not an activated ability: " + abilityName);
        }

        String key = instanceId + ":" + abilityName;
        if (context.getAbilitiesUsedThisTurn().contains(key)) {
            throw new IllegalArgumentException("Ability already used this turn: " + abilityName);
        }

        AbilityEffect effect = abilityRegistry.getAbilityEffect(holder.getCard().getId(), abilityIndex);
        effect.apply(context, holder, target);
        context.getAbilitiesUsedThisTurn().add(key);
        context.recordEvent(new AbilityActivatedGameEvent(
                holder.getCard().getId(), holder.getCard().getName(), abilityName));
    }

    private void retreat(Battle match, GameAction action) {
        GameContext context = requireMatchContext(match);
        requirePhase(context, TurnPhase.MAIN, "Retreat can only be declared from the main phase");
        InPlayPokemon active = context.getAttacker();
        if (active == null) {
            throw new IllegalArgumentException("No active Pokemon to retreat");
        }
        if (!ruleValidator.canRetreat(active, context)) {
            throw new IllegalArgumentException("Cannot retreat right now");
        }

        List<InPlayPokemon> bench = context.getAttackerBench();
        long occupiedSlots = bench.stream().filter(Objects::nonNull).count();
        if (occupiedSlots < 1) {
            throw new IllegalArgumentException("No bench Pokemon to replace the active");
        }

        // Discard the retreat cost in energies (Fairy Garden zero-out already applied by canRetreat).
        int retreatCost = active.getCard().getRetreatCost();
        if (!(isFairyGardenActive(context) && active.countEnergies(ar.edu.utn.frc.tup.piii.models.EnergyType.FAIRY) > 0)) {
            for (int i = 0; i < retreatCost; i++) {
                ar.edu.utn.frc.tup.piii.models.EnergyCard removed = active.removeOneEnergy(null);
                if (removed != null) {
                    context.getAttackerDiscard().add(removed);
                }
            }
        }

        // Mark this Pokémon as having retreated so it can't be promoted back this same turn.
        context.getAttackerRetreatedThisTurn().add(active.getInstanceId());

        // Place active on the bench — either in an empty slot, or store as pending
        // placement so promoteFromBench can put it in the vacated slot later.
        long freeSlots = bench.stream().filter(Objects::isNull).count();
        if (freeSlots > 0) {
            for (int i = 0; i < bench.size(); i++) {
                if (bench.get(i) == null) {
                    bench.set(i, active);
                    break;
                }
            }
        } else {
            context.setPendingBenchPlacement(active);
        }
        context.setAttacker(null);
        match.markPendingActiveSelection(match.getCurrentTurnPlayerId());
        context.addLogEntry(currentPlayer(match).getId(), "RETREAT",
                active.getCard().getName() + " se retiró a la banca");
    }

    private static boolean isFairyGardenActive(GameContext context) {
        return context.getActiveStadium() != null
                && "xy1-117".equals(context.getActiveStadium().getId());
    }

    // ---- Attack phase and turn rotation ----

    private void resolveAttack(Battle match, GameAction action) {
        GameContext context = requireMatchContext(match);
        requireActivePokemon(context);
        requirePhase(context, TurnPhase.ATTACK, "Attack cannot be declared before entering the attack phase");

        Attack attack = findAttack(context.getAttacker(), action.getAttackName());

        if (!ruleValidator.canAttack(context.getAttacker(), attack, context)) {
            throw new IllegalArgumentException("Attack cannot be used right now: " + attack.getName());
        }

        attackResolutionPipeline.resolve(context, attack);
        String defenderName = context.getDefender() != null
                ? context.getDefender().getCard().getName() : "desconocido";
        context.addLogEntry(currentPlayer(match).getId(), "ATTACK",
                attack.getName() + " por " + context.getPendingDamage() + " de daño a " + defenderName);
        resolveKnockouts(match);
        finishMatchIfNeeded(match, victoryConditionChecker.evaluateAfterAction(match));

        if (!match.isFinished()) {
            enterBetweenTurns(match);
        }
    }

    private void endTurn(Battle match) {
        GameContext context = requireMatchContext(match);
        requireActivePokemon(context);
        requirePhase(context, TurnPhase.ATTACK, "Turn can only end from the attack phase");
        enterBetweenTurns(match);
    }

    private void enterBetweenTurns(Battle match) {
        // Entering the between-turns phase resolves end-of-turn special conditions on both actives.
        transitionTo(match, TurnPhase.BETWEEN_TURNS);
        resolveKnockouts(match);
        finishMatchIfNeeded(match, victoryConditionChecker.evaluateAfterAction(match));
        if (match.isFinished()) {
            return;
        }

        beginNextTurn(match);
    }

    private void beginNextTurn(Battle match) {
        match.advanceTurnTimer();
        match.swapTurn();
        finishMatchIfNeeded(match, victoryConditionChecker.evaluateTurnStart(match));
        if (match.isFinished()) {
            return;
        }

        // Draw is transient: it draws for the new active player and then yields to the main phase.
        transitionTo(match, TurnPhase.DRAW);
        transitionTo(match, TurnPhase.MAIN);
    }

    private void finishMatchIfNeeded(Battle match, MatchResult result) {
        if (match.isFinished() || result == null || result == MatchResult.NO_WINNER_YET) {
            return;
        }

        if (result == MatchResult.PLAYER_ONE_WINS) {
            match.finish(match.getPlayerOne().getId());
        } else if (result == MatchResult.PLAYER_TWO_WINS) {
            match.finish(match.getPlayerTwo().getId());
        }
    }

    // ---- Knockout resolution ----

    private void resolveKnockouts(Battle match) {
        GameContext context = requireMatchContext(match);
        resolveDefenderKnockout(match, context);
        resolveAttackerKnockout(match, context);
    }

    private void resolveDefenderKnockout(Battle match, GameContext context) {
        InPlayPokemon knockedOutPokemon = context.getDefender();
        if (knockedOutPokemon == null || !knockedOutPokemon.isKnockedOut()) {
            return;
        }

        String knockedOutPlayerId = match.getOpponentPlayerId(match.getCurrentTurnPlayerId());
        List<InPlayPokemon> defenderBench = requireZone(context.getDefenderBench(), "bench");

        int prizeValue = knockedOutPokemon.getCard().getPrizeValue();
        boolean attackerTookLastPrize = context.getAttackerPrizes().size() <= prizeValue;
        triggeredAbilityDispatcher.onKnockedOut(context, knockedOutPokemon, context.getAttacker());
        moveToDiscard(knockedOutPokemon, context.getDefenderDiscard());
        context.setDefender(null);

        if (attackerTookLastPrize || !hasAnyLiveBenchPokemon(defenderBench)) {
            if (attackerTookLastPrize) {
                awardPrizeCards(context.getAttackerPrizes(), context.getAttackerHand(), prizeValue);
            }
            finishMatchIfUnfinished(match, match.getCurrentTurnPlayerId());
            return;
        }

        match.setPendingPrizeSelectionPlayerId(match.getCurrentTurnPlayerId());
        match.markPendingActiveSelection(knockedOutPlayerId);
    }

    private void resolveAttackerKnockout(Battle match, GameContext context) {
        InPlayPokemon knockedOutPokemon = context.getAttacker();
        if (knockedOutPokemon == null || !knockedOutPokemon.isKnockedOut()) {
            return;
        }

        String knockedOutPlayerId = match.getCurrentTurnPlayerId();
        String opposingPlayerId = match.getOpponentPlayerId(knockedOutPlayerId);
        List<InPlayPokemon> attackerBench = requireZone(context.getAttackerBench(), "bench");

        int prizeValue = knockedOutPokemon.getCard().getPrizeValue();
        boolean opponentTookLastPrize = context.getDefenderPrizes().size() <= prizeValue;
        moveToDiscard(knockedOutPokemon, context.getAttackerDiscard());
        context.setAttacker(null);

        if (opponentTookLastPrize || !hasAnyLiveBenchPokemon(attackerBench)) {
            if (opponentTookLastPrize) {
                awardPrizeCards(context.getDefenderPrizes(), context.getDefenderHand(), prizeValue);
            }
            finishMatchIfUnfinished(match, opposingPlayerId);
            return;
        }

        match.setPendingPrizeSelectionPlayerId(opposingPlayerId);
        match.markPendingActiveSelection(knockedOutPlayerId);
    }

    private boolean hasAnyLiveBenchPokemon(List<InPlayPokemon> bench) {
        if (bench == null) {
            return false;
        }
        return bench.stream().anyMatch(Objects::nonNull);
    }

    private void finishMatchIfUnfinished(Battle match, String winnerPlayerId) {
        if (!match.isFinished()) {
            match.finish(winnerPlayerId);
        }
    }

    private void awardPrizeCards(List<Card> prizeCards, List<Card> hand, int prizeValue) {
        requireZone(prizeCards, "prize cards");
        requireZone(hand, "hand");
        int cardsToTake = Math.clamp(prizeValue, 0, prizeCards.size());
        for (int i = 0; i < cardsToTake; i++) {
            hand.add(prizeCards.removeFirst());
        }
    }

    private void moveToDiscard(InPlayPokemon pokemon, List<Card> discardPile) {
        requireZone(discardPile, "discard pile");
        discardPile.add(pokemon.getCard());
        discardPile.addAll(pokemon.getUnderlyingCards());
        discardPile.addAll(pokemon.getEnergies());
        if (pokemon.getTool() != null) {
            discardPile.add(pokemon.getTool());
        }
    }

    // ---- Validation helpers ----

    private void validateActionOwnership(Battle match, String playerId) {
        if (playerId == null || playerId.isBlank()) {
            throw new IllegalArgumentException("Player id must not be blank");
        }
        if (!match.containsPlayer(playerId)) {
            throw new IllegalArgumentException("Player does not belong to match: " + playerId);
        }
        if (match.isFinished()) {
            throw new IllegalArgumentException("Match is already finished");
        }
        if (!match.isCurrentTurnPlayer(playerId)) {
            throw new IllegalArgumentException("It is not the turn of player: " + playerId);
        }
        if (match.isWaitingForActiveSelection()) {
            throw new IllegalArgumentException("Match is waiting for active Pokemon selection");
        }
        if (match.isPendingPrizeSelection()) {
            throw new IllegalArgumentException("Match is waiting for prize card selection");
        }
    }

    private void requireActivePokemon(GameContext context) {
        if (context.getAttacker() == null || context.getDefender() == null) {
            throw new IllegalArgumentException("Both players must have an active Pokemon before acting");
        }
    }

    private GameContext requireMatchContext(Battle match) {
        return Objects.requireNonNull(match.getContext(), "match context must not be null");
    }

    private TurnPhaseState currentPhaseState(Battle match) {
        TurnPhase currentPhase = requireMatchContext(match).getCurrentPhase();
        TurnPhaseState state = phaseStates.get(currentPhase);
        if (state == null) {
            throw new IllegalStateException("Unsupported turn phase: " + currentPhase);
        }
        return state;
    }

    private void requirePhase(GameContext context, TurnPhase expectedPhase, String errorMessage) {
        if (context.getCurrentPhase() != expectedPhase) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private Player currentPlayer(Battle match) {
        String currentTurnPlayerId = match.getCurrentTurnPlayerId();
        if (Objects.equals(match.getPlayerOne().getId(), currentTurnPlayerId)) {
            return match.getPlayerOne();
        }
        return match.getPlayerTwo();
    }

    private String requireCardId(String cardId) {
        if (cardId == null || cardId.isBlank()) {
            throw new IllegalArgumentException("Card id must not be blank");
        }
        return cardId;
    }

    private String requireTargetId(String targetCardId) {
        if (targetCardId == null || targetCardId.isBlank()) {
            throw new IllegalArgumentException("Target card id must not be blank");
        }
        return targetCardId;
    }

    private Card findHandCard(List<Card> hand, String cardId) {
        requireZone(hand, "hand");
        return hand.stream()
                .filter(Objects::nonNull)
                .filter(card -> Objects.equals(card.getId(), cardId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Card not found in player hand: " + cardId));
    }

    private InPlayPokemon findOwnInPlayPokemon(GameContext context, String instanceId) {
        InPlayPokemon active = context.getAttacker();
        if (active != null && Objects.equals(active.getInstanceId(), instanceId)) {
            return active;
        }
        List<InPlayPokemon> bench = requireZone(context.getAttackerBench(), "bench");
        return bench.stream()
                .filter(Objects::nonNull)
                .filter(pokemon -> Objects.equals(pokemon.getInstanceId(), instanceId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("In-play Pokemon not found: " + instanceId));
    }

    private InPlayPokemon evolve(InPlayPokemon target, PokemonCard evolutionCard) {
        // Evolution keeps damage, attached energies and tool but removes special conditions.
        // The pre-evolution card is preserved in the underlying chain so that the entire
        // evolution history lands in the discard pile when the Pokemon is knocked out.
        InPlayPokemon evolved = new InPlayPokemon(evolutionCard);
        evolved.setDamageTaken(target.getDamageTaken());
        evolved.setEnergies(target.getEnergies());
        evolved.setTool(target.getTool());
        evolved.setUnderlyingCards(target.getUnderlyingCards());
        evolved.getUnderlyingCards().add(target.getCard());
        return evolved;
    }

    private void replaceInPlayPokemon(GameContext context, InPlayPokemon target, InPlayPokemon replacement) {
        if (target == context.getAttacker()) {
            context.setAttacker(replacement);
            return;
        }
        List<InPlayPokemon> bench = requireZone(context.getAttackerBench(), "bench");
        int index = bench.indexOf(target);
        if (index < 0) {
            throw new IllegalStateException("In-play Pokemon is no longer on the battlefield");
        }
        bench.set(index, replacement);
    }

    private Attack findAttack(InPlayPokemon attacker, String attackName) {
        Objects.requireNonNull(attacker, "attacker must not be null");
        if (attackName == null || attackName.isBlank()) {
            throw new IllegalArgumentException("Attack name must not be blank");
        }
        if (attacker.getCard() == null) {
            throw new IllegalStateException("Active Pokemon must reference a card");
        }
        List<Attack> attacks = attacker.getCard().getAttacks();
        if (attacks == null || attacks.isEmpty()) {
            throw new IllegalArgumentException("Active Pokemon has no attacks available");
        }

        return attacks.stream()
                .filter(attack -> Objects.equals(attack.getName(), attackName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Attack not found on active Pokemon: "
                        + attackName));
    }

    private <T> List<T> requireZone(List<T> zone, String zoneName) {
        if (zone == null) {
            throw new IllegalStateException("Match context " + zoneName + " must not be null");
        }
        return zone;
    }

    /**
     * Adapter that exposes phase-scoped operations to state objects.
     */
    private final class PhaseActionContext implements TurnPhaseActionContext {

        private final Battle match;

        private PhaseActionContext(Battle match) {
            this.match = Objects.requireNonNull(match, "match must not be null");
        }

        @Override
        public void playBasicPokemon(GameAction action) {
            TurnManager.this.playBasicPokemon(match, action);
        }

        @Override
        public void evolvePokemon(GameAction action) {
            TurnManager.this.evolvePokemon(match, action);
        }

        @Override
        public void attachEnergy(GameAction action) {
            TurnManager.this.attachEnergy(match, action);
        }

        @Override
        public void playItem(GameAction action) {
            TurnManager.this.playItem(match, action);
        }

        @Override
        public void playSupporter(GameAction action) {
            TurnManager.this.playSupporter(match, action);
        }

        @Override
        public void playStadium(GameAction action) {
            TurnManager.this.playStadium(match, action);
        }

        @Override
        public void attachTool(GameAction action) {
            TurnManager.this.attachTool(match, action);
        }

        @Override
        public void retreat(GameAction action) {
            TurnManager.this.retreat(match, action);
        }

        @Override
        public void advanceToAttack() {
            advanceToAttackPhase(match);
        }

        @Override
        public void resolveAttack(GameAction action) {
            TurnManager.this.resolveAttack(match, action);
        }

        @Override
        public void endTurn() {
            TurnManager.this.endTurn(match);
        }

        @Override
        public void activateAbility(GameAction action) {
            TurnManager.this.activateAbility(match, action);
        }
    }
}
