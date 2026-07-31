package ar.edu.utn.frc.tup.piii.models.effects;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import ar.edu.utn.frc.tup.piii.models.Ability;
import ar.edu.utn.frc.tup.piii.models.AbilityEffect;
import ar.edu.utn.frc.tup.piii.models.AbilityEffect.TriggerContext;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;

/**
 * Registry that resolves the {@link AbilityEffect} strategy for a given card and
 * ability index.
 *
 * <p>Mappings are keyed by the composite {@code "cardId:abilityIndex"} so a
 * single card with several abilities can register a different effect per slot.
 * The default fallback is a no-op, so unmapped abilities are safely inert.
 */
@Component
public class AbilityRegistry {

    /** No-op ability used when no strategy is registered for a key. */
    public static final AbilityEffect NO_OP = (GameContext ctx, InPlayPokemon holder, Ability ability) -> {
        // intentionally empty
    };

    private final Map<String, AbilityEffect> registry = new HashMap<>();

    public AbilityRegistry() {
        registerDefaultAbilities();
    }

    /**
     * Returns the {@link AbilityEffect} associated with a specific ability on a
     * specific card. Returns the no-op effect when no strategy is registered.
     *
     * @param cardId        the card identifier (e.g., {@code "xy1-1"})
     * @param abilityIndex  zero-based index of the ability within the card's list
     * @return the registered effect, or {@link #NO_OP} if none is registered
     */
    public AbilityEffect getAbilityEffect(String cardId, int abilityIndex) {
        return registry.getOrDefault(buildKey(cardId, abilityIndex), NO_OP);
    }

    /**
     * Registers an ability effect for the given card and ability index.
     *
     * <p>Useful for tests and for future card-specific extensions.
     *
     * @param cardId       the card identifier
     * @param abilityIndex zero-based ability index
     * @param effect       the strategy to associate with the key
     */
    public void register(String cardId, int abilityIndex, AbilityEffect effect) {
        registry.put(buildKey(cardId, abilityIndex), effect);
    }

    private void registerDefaultAbilities() {
        // xy1-14 Chesnaught — Spiky Shield (TRIGGERED/on-damaged): place 3 damage counters on the attacker.
        register("xy1-14", 0, new AbilityEffect() {
            @Override
            public void apply(GameContext ctx, InPlayPokemon holder, Ability ability) {
                // called by PASSIVE dispatch — no-op for Spiky Shield
            }

            @Override
            public void apply(GameContext ctx, InPlayPokemon holder, Ability ability, TriggerContext trigger) {
                if (trigger.attacker() != null) {
                    trigger.attacker().setDamageTaken(trigger.attacker().getDamageTaken() + 30);
                }
            }
        });

        // xy1-44 Voltorb — Destiny Burst (TRIGGERED/on-KO): flip a coin; heads → 50 damage on attacker.
        register("xy1-44", 0, new AbilityEffect() {
            @Override
            public void apply(GameContext ctx, InPlayPokemon holder, Ability ability) {
                // called by PASSIVE dispatch — no-op for Destiny Burst
            }

            @Override
            public void apply(GameContext ctx, InPlayPokemon holder, Ability ability, TriggerContext trigger) {
                if (ctx.flipCoin("destinyBurst") && trigger.attacker() != null) {
                    trigger.attacker().setDamageTaken(trigger.attacker().getDamageTaken() + 50);
                }
            }
        });

        // xy1-114 Furfrou — Fur Coat (PASSIVE): incoming damage is reduced by 20.
        // Effect is invoked twice per attack (offensive + defensive pass); only act
        // when the holder is currently defending, so attacker passives don't shrink
        // their own outgoing damage.
        register("xy1-114", 0, (ctx, holder, ability) -> {
            if (ctx.getDefender() == holder) {
                ctx.setPendingDamage(ctx.getPendingDamage() - 20);
            }
        });

        // xy1-74 Inkay — Upside-Down Evolution (ACTIVATED):
        // if opponent has taken more prizes, search deck for a card that evolves from Inkay and evolve.
        register("xy1-74", 0, (ctx, holder, ability) -> {
            int attackerPrizesLeft = ctx.getAttackerPrizes() != null ? ctx.getAttackerPrizes().size() : 6;
            int defenderPrizesLeft = ctx.getDefenderPrizes() != null ? ctx.getDefenderPrizes().size() : 6;
            int attackerTaken = 6 - attackerPrizesLeft;
            int defenderTaken = 6 - defenderPrizesLeft;
            if (defenderTaken <= attackerTaken) {
                throw new IllegalArgumentException(
                        "Upside-Down Evolution: opponent has not taken more prizes");
            }
            Card evolution = ctx.getAttackerDeck().stream()
                    .filter(c -> c instanceof PokemonCard pc && "Inkay".equals(pc.getEvolvesFrom()))
                    .findFirst().orElse(null);
            if (evolution == null) {
                throw new IllegalArgumentException(
                        "Upside-Down Evolution: no card that evolves from Inkay in deck");
            }
            ctx.getAttackerDeck().remove(evolution);
            // Evolve holder into the found evolution card (mirrors TurnManager.evolve logic).
            PokemonCard evolutionCard = (PokemonCard) evolution;
            InPlayPokemon evolved = new InPlayPokemon(evolutionCard);
            evolved.setDamageTaken(holder.getDamageTaken());
            evolved.setEnergies(holder.getEnergies());
            evolved.setTool(holder.getTool());
            evolved.setUnderlyingCards(holder.getUnderlyingCards());
            evolved.getUnderlyingCards().add(holder.getCard());
            if (holder == ctx.getAttacker()) {
                ctx.setAttacker(evolved);
            } else {
                List<InPlayPokemon> bench = ctx.getAttackerBench();
                int idx = bench.indexOf(holder);
                if (idx >= 0) {
                    bench.set(idx, evolved);
                }
            }
        });

        // xy1-85/86 Aegislash — Stance Change (ACTIVATED): needs in-hand card swap — not yet in v1.
        register("xy1-85", 0, (ctx, holder, ability) -> {
            throw new IllegalArgumentException("Stance Change is not yet implemented in v1");
        });
        register("xy1-86", 0, (ctx, holder, ability) -> {
            throw new IllegalArgumentException("Stance Change is not yet implemented in v1");
        });

        // xy1-93 Aromatisse — Fairy Transfer (ACTIVATED): needs energy source/target picker — not yet in v1.
        register("xy1-93", 0, (ctx, holder, ability) -> {
            throw new IllegalArgumentException("Fairy Transfer is not yet implemented in v1");
        });

        // xy1-26 Delphox — Mystical Fire: draw until 6 cards in hand
        register("xy1-26", 0, (ctx, holder, ability) -> {
            while (ctx.getAttackerHand().size() < 6) {
                Card drawn = ctx.attackerDrawCard();
                if (drawn == null) {
                    break;
                }
            }
        });

        // xy1-41 Greninja — Water Shuriken: discard a Water Energy from hand, place 3 damage counters
        register("xy1-41", 0, (ctx, holder, ability) -> {
            Card waterEnergy = ctx.getAttackerHand().stream()
                    .filter(c -> c instanceof EnergyCard ec && ec.getEnergyType() == EnergyType.WATER)
                    .findFirst().orElse(null);
            if (waterEnergy == null) {
                throw new IllegalArgumentException("No Water Energy in hand for Water Shuriken");
            }
            ctx.getAttackerHand().remove(waterEnergy);
            ctx.getAttackerDiscard().add(waterEnergy);
            // place 3 damage counters (30 damage) on opponent's active
            InPlayPokemon defenderActive = ctx.getDefender();
            if (defenderActive != null) {
                defenderActive.setDamageTaken(defenderActive.getDamageTaken() + 30);
            }
        });

        // xy1-103 Swellow — Drive Off: opponent switches their active with a benched Pokemon
        // (simplified: force swap to first available bench slot — full target selection not yet supported)
        register("xy1-103", 0, (ctx, holder, ability) -> {
            InPlayPokemon firstBenched = ctx.getDefenderBench().stream()
                    .filter(p -> p != null)
                    .findFirst().orElse(null);
            if (firstBenched == null) {
                return; // no bench, no effect
            }
            int idx = ctx.getDefenderBench().indexOf(firstBenched);
            InPlayPokemon currentDefender = ctx.getDefender();
            ctx.setDefender(firstBenched);
            ctx.getDefenderBench().set(idx, currentDefender);
        });
    }

    private String buildKey(String cardId, int abilityIndex) {
        return cardId + ":" + abilityIndex;
    }
}
