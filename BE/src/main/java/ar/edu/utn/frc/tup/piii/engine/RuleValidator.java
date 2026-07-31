package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.models.Attack;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.ItemCard;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.PokemonToolCard;
import ar.edu.utn.frc.tup.piii.models.StadiumCard;
import ar.edu.utn.frc.tup.piii.models.StatusCondition;
import ar.edu.utn.frc.tup.piii.models.SupporterCard;
import ar.edu.utn.frc.tup.piii.models.TurnPhase;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Validates whether common player actions are currently legal under the XY1 ruleset.
 *
 * <p>This class is intentionally pure: every method derives its result only from the provided
 * arguments, performs no mutations and has no framework dependencies. The design centralizes
 * reusable checks so future action validators can compose the same phase, resource and
 * restriction helpers without duplicating logic.
 */
public final class RuleValidator {

    private static final int MAX_BENCH_SIZE = 5;
    private static final int BASE_MANUAL_ENERGY_ATTACHMENTS = 1;
    private static final int MAX_SUPPORTERS_PER_TURN = 1;
    private static final String PLAYER_MUST_NOT_BE_NULL = "player must not be null";
    private static final String CONTEXT_MUST_NOT_BE_NULL = "context must not be null";

    /**
     * Validates whether the current player may put a Basic Pokémon onto the Bench.
     *
     * <p>XY1 only allows the action during the {@link TurnPhase#MAIN} phase and only while the
     * Bench has fewer than five Pokémon.
     *
     * @param player current player attempting the action; must not be {@code null}
     * @param context current game state; must not be {@code null}
     * @return {@code true} when the Bench still has room and the turn is in the main phase
     * @throws NullPointerException if any required argument is {@code null}
     */
    public boolean canPlayBasicPokemon(Player player, GameContext context) {
        Objects.requireNonNull(player, PLAYER_MUST_NOT_BE_NULL);
        Objects.requireNonNull(context, CONTEXT_MUST_NOT_BE_NULL);

        // Playing a Basic Pokémon is a bench-placement action, so phase and bench capacity decide
        // its legality here.
        return isPhase(context, TurnPhase.MAIN)
                && countNonNull(context.getAttackerBench()) < MAX_BENCH_SIZE;
    }

    /**
     * Validates whether one Pokémon card may legally evolve into another.
     *
     * <p>The evolution must match the card metadata, cannot happen on the first turn of the
     * match and cannot target a Pokémon that entered play during the same turn.
     *
     * @param base current in-play Pokémon card; must not be {@code null}
     * @param evolution candidate evolution card; must not be {@code null}
     * @param context current game state; must not be {@code null}
     * @return {@code true} when the evolution path is valid and the XY1 timing restrictions allow
     *         it
     * @throws NullPointerException if any required argument is {@code null}
     */
    public boolean canEvolve(PokemonCard base, PokemonCard evolution, GameContext context) {
        Objects.requireNonNull(base, "base must not be null");
        Objects.requireNonNull(evolution, "evolution must not be null");
        Objects.requireNonNull(context, CONTEXT_MUST_NOT_BE_NULL);

        if (!isValidEvolution(base, evolution)) {
            return false;
        }
        if (context.isFirstTurn()) {
            return false;
        }
        if (wasPlayedThisTurn(base, context)) {
            return false;
        }
        return true;
    }

    /**
     * Validates whether the player may attach the given energy card manually this turn.
     *
     * <p>XY1 allows one manual energy attachment during the {@link TurnPhase#MAIN} phase, unless
     * card effects grant additional attachments. This validator only checks the legal window and
     * per-turn quota; it does not enforce card-specific targeting text.
     *
     * @param player current player attempting the action; must not be {@code null}
     * @param energyCard energy card being attached; must not be {@code null}
     * @param target Pokémon receiving the energy; must not be {@code null}
     * @param context current game state; must not be {@code null}
     * @return {@code true} when the player is still in the main phase and has at least one manual
     *         attachment available
     * @throws NullPointerException if any required argument is {@code null}
     */
    public boolean canAttachEnergy(Player player,
                                   EnergyCard energyCard,
                                   InPlayPokemon target,
                                   GameContext context) {
        Objects.requireNonNull(player, PLAYER_MUST_NOT_BE_NULL);
        Objects.requireNonNull(energyCard, "energyCard must not be null");
        Objects.requireNonNull(target, "target must not be null");
        Objects.requireNonNull(context, CONTEXT_MUST_NOT_BE_NULL);

        // The quota is computed centrally so future effect-based attachment exceptions can reuse
        // the same allowance calculation.
        return isPhase(context, TurnPhase.MAIN) && hasEnergyAttachmentAvailable(context);
    }

    /**
     * Validates whether a Pokémon may declare the given attack right now.
     *
     * <p>The action is legal only during the {@link TurnPhase#ATTACK} phase, when the Pokémon has
     * enough attached energy to pay the attack cost and is not blocked by special conditions or
     * explicit next-turn attack restrictions.
     *
     * @param attacker attacking Pokémon; must not be {@code null}
     * @param attack attack to declare; must not be {@code null}
     * @param context current game state; must not be {@code null}
     * @return {@code true} when the attacker satisfies the XY1 timing, status and energy checks
     * @throws NullPointerException if any required argument is {@code null}
     */
    public boolean canAttack(InPlayPokemon attacker, Attack attack, GameContext context) {
        Objects.requireNonNull(attacker, "attacker must not be null");
        Objects.requireNonNull(attack, "attack must not be null");
        Objects.requireNonNull(context, CONTEXT_MUST_NOT_BE_NULL);

        if (context.isFirstTurn()) {
            return false;
        }

        // Attack validation combines mandatory phase gating, status checks, explicit card-effect
        // restrictions and the reusable energy-cost matcher.
        return isPhase(context, TurnPhase.ATTACK)
                && !hasBlockingAttackStatus(attacker)
                && !hasAttackRestriction(attacker, attack)
                && canPayAttackCost(attacker, attack);
    }

    /**
     * Validates whether the active Pokémon may retreat.
     *
     * <p>The Pokémon must have enough attached energy to pay its retreat cost and must not be
     * affected by retreat-preventing restrictions such as explicit lock effects or immobilizing
     * statuses.
     *
     * @param pokemon active Pokémon attempting to retreat; must not be {@code null}
     * @param context current game state; must not be {@code null}
     * @return {@code true} when retreat cost and restriction checks both pass
     * @throws NullPointerException if any required argument is {@code null}
     */
    public boolean canRetreat(InPlayPokemon pokemon, GameContext context) {
        Objects.requireNonNull(pokemon, "pokemon must not be null");
        Objects.requireNonNull(context, CONTEXT_MUST_NOT_BE_NULL);

        // Fairy Garden (xy1-117): a Pokémon with any Fairy Energy attached has no retreat cost.
        int retreatCost = pokemon.getCard().getRetreatCost();
        if (retreatCost > 0 && isFairyGardenActive(context) && pokemon.countEnergies(EnergyType.FAIRY) > 0) {
            retreatCost = 0;
        }
        // Retreat has a flat energy requirement plus a set of active restrictions that can fully
        // block the action even when enough energy is attached.
        return hasEnoughTotalEnergy(pokemon, retreatCost)
                && !hasRetreatRestriction(pokemon);
    }

    private static boolean isFairyGardenActive(GameContext context) {
        return context.getActiveStadium() != null
                && "xy1-117".equals(context.getActiveStadium().getId());
    }

    /**
     * Validates whether the player may play a Supporter card this turn.
     *
     * <p>XY1 allows at most one Supporter during the player's turn, before attacking. The
     * validator therefore checks both the per-turn quota and any active supporter lock effect.
     *
     * @param player current player attempting the action; must not be {@code null}
     * @param supporterCard supporter card being played; must not be {@code null}
     * @param context current game state; must not be {@code null}
     * @return {@code true} when the player is still in the main phase, is not locked out of
     *         Supporters and has not already used one this turn
     * @throws NullPointerException if any required argument is {@code null}
     */
    public boolean canUseSupporterCard(Player player,
                                       SupporterCard supporterCard,
                                       GameContext context) {
        Objects.requireNonNull(player, PLAYER_MUST_NOT_BE_NULL);
        Objects.requireNonNull(supporterCard, "supporterCard must not be null");
        Objects.requireNonNull(context, CONTEXT_MUST_NOT_BE_NULL);

        // Supporters share the same main-phase timing as other turn actions, but also respect the
        // dedicated "one Supporter per turn" quota and explicit card-effect lockouts.
        return isPhase(context, TurnPhase.MAIN)
                && !context.isAttackerCantPlaySupporters()
                && context.getAttackerSupportersPlayedThisTurn() < MAX_SUPPORTERS_PER_TURN;
    }

    /**
     * Validates whether the player may play an Item card this turn.
     *
     * <p>XY1 allows any number of Item cards during the {@link TurnPhase#MAIN} phase. This validator
     * only checks the legal window; it does not enforce card-specific targeting text.
     *
     * @param player current player attempting the action; must not be {@code null}
     * @param itemCard item card being played; must not be {@code null}
     * @param context current game state; must not be {@code null}
     * @return {@code true} when the player is still in the main phase
     * @throws NullPointerException if any required argument is {@code null}
     */
    public boolean canPlayItem(Player player, ItemCard itemCard, GameContext context) {
        Objects.requireNonNull(player, PLAYER_MUST_NOT_BE_NULL);
        Objects.requireNonNull(itemCard, "itemCard must not be null");
        Objects.requireNonNull(context, CONTEXT_MUST_NOT_BE_NULL);

        // Items share the main-phase timing of other board actions but carry no per-turn quota.
        return isPhase(context, TurnPhase.MAIN);
    }

    /**
     * Validates whether the current player may play a Stadium card.
     *
     * <p>XY1 rules: a Stadium can be played during the main phase. If another Stadium
     * with the same name is already in play, the new one cannot be played.
     *
     * @param player  current player attempting the action; must not be {@code null}
     * @param stadium stadium card being played; must not be {@code null}
     * @param context current game state; must not be {@code null}
     * @return {@code true} when the Stadium may legally be played
     */
    public boolean canPlayStadium(Player player, StadiumCard stadium, GameContext context) {
        Objects.requireNonNull(player, PLAYER_MUST_NOT_BE_NULL);
        Objects.requireNonNull(stadium, "stadium must not be null");
        Objects.requireNonNull(context, CONTEXT_MUST_NOT_BE_NULL);

        if (!isPhase(context, TurnPhase.MAIN)) {
            return false;
        }
        StadiumCard active = context.getActiveStadium();
        return active == null || !Objects.equals(active.getName(), stadium.getName());
    }

    /**
     * Validates whether the current player may attach a Pokémon Tool to a target.
     *
     * <p>XY1 rules: a tool can be attached during the main phase to a Pokémon that
     * does not already have a tool attached.
     *
     * @param player current player attempting the action; must not be {@code null}
     * @param tool   tool card being attached; must not be {@code null}
     * @param target in-play Pokémon to receive the tool; must not be {@code null}
     * @param context current game state; must not be {@code null}
     * @return {@code true} when the tool may legally be attached
     */
    public boolean canAttachTool(Player player, PokemonToolCard tool,
                                 InPlayPokemon target, GameContext context) {
        Objects.requireNonNull(player, PLAYER_MUST_NOT_BE_NULL);
        Objects.requireNonNull(tool, "tool must not be null");
        Objects.requireNonNull(target, "target must not be null");
        Objects.requireNonNull(context, CONTEXT_MUST_NOT_BE_NULL);

        return isPhase(context, TurnPhase.MAIN) && target.getTool() == null;
    }

    /**
     * Checks whether the current phase matches the one required by the action.
     *
     * @param context current game state
     * @param expectedPhase phase expected by the validator
     * @return {@code true} when the action is being attempted in the correct phase
     */
    private boolean isPhase(GameContext context, TurnPhase expectedPhase) {
        return context.getCurrentPhase() == expectedPhase;
    }

    /**
     * Checks whether the target card represents a legal evolution step.
     *
     * @param base current Pokémon card
     * @param evolution candidate evolution card
     * @return {@code true} when the evolution metadata says the second card evolves from the first
     */
    private boolean isValidEvolution(PokemonCard base, PokemonCard evolution) {
        return evolution.canEvolveFrom(base);
    }

    /**
     * Checks whether the base Pokémon entered play during the current turn.
     *
     * @param base current Pokémon card
     * @param context current game state
     * @return {@code true} when the current turn tracker contains the same card instance
     */
    private boolean wasPlayedThisTurn(PokemonCard base, GameContext context) {
        List<PokemonCard> playedThisTurn = context.getAttackerPokemonPlayedThisTurn();
        return playedThisTurn != null && playedThisTurn.contains(base);
    }

    /**
     * Checks whether the player still has at least one manual energy attachment available.
     *
     * @param context current game state
     * @return {@code true} when the used attachments are still below the current allowance
     */
    private boolean hasEnergyAttachmentAvailable(GameContext context) {
        int usedAttachments = Math.max(0, context.getAttackerEnergyAttachmentsThisTurn());
        int additionalAttachments = Math.max(0, context.getAttackerAdditionalEnergyAttachments());
        int allowedAttachments = BASE_MANUAL_ENERGY_ATTACHMENTS + additionalAttachments;
        return usedAttachments < allowedAttachments;
    }

    /**
     * Checks whether a Pokémon is blocked from attacking by XY1 special conditions.
     *
     * @param attacker attacking Pokémon
     * @return {@code true} when Sleep or Paralysis currently prevent the action
     */
    private boolean hasBlockingAttackStatus(InPlayPokemon attacker) {
        return attacker.hasStatus(StatusCondition.ASLEEP)
                || attacker.hasStatus(StatusCondition.PARALYZED);
    }

    /**
     * Checks whether the Pokémon is affected by a direct attack restriction.
     *
     * @param attacker attacking Pokémon
     * @param attack chosen attack
     * @return {@code true} when the Pokémon cannot attack at all or cannot use this specific move
     */
    private boolean hasAttackRestriction(InPlayPokemon attacker, Attack attack) {
        return attacker.isCantAttack() || Objects.equals(attacker.getCantUseAttackName(),
                attack.getName());
    }

    /**
     * Checks whether a Pokémon may pay the declared attack's energy cost.
     *
     * <p>The check first validates every typed requirement and then confirms that the total number
     * of attached energies also covers any remaining Colorless slots.
     *
     * @param attacker attacking Pokémon
     * @param attack chosen attack
     * @return {@code true} when the attached energies satisfy the full attack cost
     */
    private boolean canPayAttackCost(InPlayPokemon attacker, Attack attack) {
        Map<EnergyType, Integer> energyCost = attack.getEnergyCost();
        if (energyCost == null || energyCost.isEmpty()) {
            return true;
        }
        for (Map.Entry<EnergyType, Integer> costEntry : energyCost.entrySet()) {
            EnergyType energyType = costEntry.getKey();
            int requiredAmount = normalizeRequiredAmount(costEntry.getValue());
            if (energyType != EnergyType.COLORLESS
                    && attacker.countEnergies(energyType) < requiredAmount) {
                return false;
            }
        }
        return hasEnoughTotalEnergy(attacker, totalRequiredEnergy(energyCost));
    }

    /**
     * Checks whether the Pokémon has enough total attached energy cards.
     *
     * @param pokemon Pokémon being validated
     * @param requiredEnergy total energy amount required for the action
     * @return {@code true} when the Pokémon has at least that many energies attached
     */
    private boolean hasEnoughTotalEnergy(InPlayPokemon pokemon, int requiredEnergy) {
        return pokemon.countEnergies(null) >= Math.max(0, requiredEnergy);
    }

    /**
     * Checks whether any active retreat-preventing restriction is currently applied.
     *
     * @param pokemon Pokémon attempting to retreat
     * @return {@code true} when an explicit retreat lock or immobilizing status blocks retreat
     */
    private boolean hasRetreatRestriction(InPlayPokemon pokemon) {
        return pokemon.isCantRetreat()
                || pokemon.hasStatus(StatusCondition.ASLEEP)
                || pokemon.hasStatus(StatusCondition.PARALYZED);
    }

    /**
     * Calculates the total amount of energy required by an attack.
     *
     * @param energyCost attack cost map
     * @return sum of all normalized cost entries
     */
    private int totalRequiredEnergy(Map<EnergyType, Integer> energyCost) {
        return energyCost.values().stream()
                .mapToInt(this::normalizeRequiredAmount)
                .sum();
    }

    /**
     * Normalizes nullable or negative cost values to a legal non-negative requirement.
     *
     * @param requiredAmount raw amount read from card data
     * @return normalized non-negative cost
     */
    private int normalizeRequiredAmount(Integer requiredAmount) {
        return Math.max(0, requiredAmount == null ? 0 : requiredAmount);
    }

    /**
     * Returns a null-safe size for tracked lists inside the game context.
     *
     * @param list list to inspect, possibly {@code null}
     * @return the list size, or {@code 0} when the list is {@code null}
     */
    private int sizeOf(List<?> list) {
        return list == null ? 0 : list.size();
    }

    private long countNonNull(List<InPlayPokemon> bench) {
        return bench == null ? 0 : bench.stream().filter(Objects::nonNull).count();
    }
}
