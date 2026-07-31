package ar.edu.utn.frc.tup.piii.models;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

/**
 * A {@link PokemonCard} as it exists on the battlefield, with all mutable in-game state.
 *
 * <p>Static card data (HP, attacks, type) lives on the immutable {@link PokemonCard}.
 * Everything that changes during a match — damage taken, attached energies, status
 * conditions, and "next-turn" flags — lives here.
 */
@Getter
@Setter
public class InPlayPokemon {

    private final PokemonCard card;
    private String instanceId;

    /** Total HP lost so far (not damage counters; 10 damage = 1 counter). */
    private int damageTaken;

    /** Energy cards currently attached. */
    private List<EnergyCard> energies;

    /** Pokémon Tool card attached, or {@code null} if none. */
    private Card tool;

    /**
     * Cards that this Pokémon evolved from.
     * <p>When a Pokémon evolves, the pre-evolution card is preserved here so that
     * when the Pokémon is knocked out, the full evolution chain lands in the
     * discard pile.
     */
    private List<PokemonCard> underlyingCards = new ArrayList<>();

    /** Active special conditions. Usually at most one; Crisis Vine is the exception. */
    private Set<StatusCondition> statuses;

    // ---- Incoming-damage protection flags (set this turn, read next opponent turn) ----

    /** All incoming damage and effects are ignored (Dig / Flash Needle all-heads). */
    private boolean preventAllEffects;

    /** All incoming damage is prevented (Scrunch heads / King's Shield). */
    private boolean preventAllIncomingDamage;

    /**
     * If > 0, incoming attacks dealing ≤ this threshold are fully prevented (Harden: 60).
     */
    private int preventIncomingDamageThreshold;

    /** Incoming damage is reduced by this amount before application (Moonblast: 20). */
    private int incomingDamageReduction;

    // ---- Restriction flags (apply to THIS Pokémon on its next action) ----

    /** This Pokémon cannot retreat on the opponent's next turn (Corner / Poison Ring). */
    private boolean cantRetreat;

    /** This Pokémon cannot attack on the owner's next turn (Rock Wrecker). */
    private boolean cantAttack;

    /**
     * Name of the one attack this Pokémon may not use next turn
     * (X Blast, King's Shield). {@code null} means no restriction.
     */
    private String cantUseAttackName;

    /**
     * If {@code true}, when this Pokémon tries to attack next turn it must first
     * flip a coin; tails causes the attack to do nothing (Mental Panic).
     */
    private boolean ifAttacksFlipCoin;

    /** Abilities are suppressed until end of the next turn (Gastro Acid). */
    private boolean abilitiesDisabled;

    /**
     * Bonus damage added to the next use of Metal Wallop specifically (Metal Wallop stacking).
     */
    private int metalWallopBonus;

    public InPlayPokemon(PokemonCard card) {
        this.card = card;
        this.instanceId = UUID.randomUUID().toString();
        this.damageTaken = 0;
        this.energies = new ArrayList<>();
        this.statuses = EnumSet.noneOf(StatusCondition.class);
    }

    /** Current remaining HP. */
    public int getCurrentHp() {
        return Math.max(0, card.getHp() - damageTaken);
    }

    /** Whether this Pokémon has been knocked out. */
    public boolean isKnockedOut() {
        return getCurrentHp() == 0;
    }

    /** Whether this Pokémon has the given status condition. */
    public boolean hasStatus(StatusCondition condition) {
        return statuses.contains(condition);
    }

    /** Applies a status condition, replacing existing ones (except when stacking is intended). */
    public void applyStatus(StatusCondition condition) {
        statuses.clear();
        statuses.add(condition);
    }

    /** Adds a status condition alongside any existing ones (for Crisis Vine etc.). */
    public void addStatus(StatusCondition condition) {
        statuses.add(condition);
    }

    /** Removes all status conditions. */
    public void clearStatuses() {
        statuses.clear();
    }

    /** Counts attached energies of the given type, or all energies if {@code type} is null. */
    public int countEnergies(EnergyType type) {
        if (type == null) {
            return energies.size();
        }
        return (int) energies.stream()
                .filter(e -> e.getEnergyType() == type)
                .count();
    }

    /** Counts distinct energy types among attached energies. */
    public long countDistinctEnergyTypes() {
        return energies.stream()
                .map(EnergyCard::getEnergyType)
                .filter(t -> t != null && t != EnergyType.COLORLESS)
                .distinct()
                .count();
    }

    /** Removes and returns the first attached energy matching {@code type}, or any if null. */
    public EnergyCard removeOneEnergy(EnergyType type) {
        for (int i = 0; i < energies.size(); i++) {
            EnergyCard e = energies.get(i);
            if (type == null || e.getEnergyType() == type) {
                energies.remove(i);
                return e;
            }
        }
        return null;
    }

    /** Removes all energies matching {@code type}, or all energies if {@code type} is null. */
    public List<EnergyCard> removeAllEnergies(EnergyType type) {
        List<EnergyCard> removed = new ArrayList<>();
        energies.removeIf(e -> {
            if (type == null || e.getEnergyType() == type) {
                removed.add(e);
                return true;
            }
            return false;
        });
        return removed;
    }

    /** Resets all next-turn flags at the start of a new turn. */
    public void resetTurnFlags() {
        preventAllEffects = false;
        preventAllIncomingDamage = false;
        preventIncomingDamageThreshold = 0;
        incomingDamageReduction = 0;
        cantRetreat = false;
        cantAttack = false;
        cantUseAttackName = null;
        ifAttacksFlipCoin = false;
        abilitiesDisabled = false;
        metalWallopBonus = 0;
    }
}
