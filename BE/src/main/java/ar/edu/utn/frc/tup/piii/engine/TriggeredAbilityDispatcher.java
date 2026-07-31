package ar.edu.utn.frc.tup.piii.engine;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import ar.edu.utn.frc.tup.piii.models.Ability;
import ar.edu.utn.frc.tup.piii.models.AbilityEffect;
import ar.edu.utn.frc.tup.piii.models.AbilityType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;

/**
 * Dispatches {@link AbilityType#TRIGGERED} abilities when the relevant game
 * event occurs.
 *
 * <p>Two dispatch points exist in v1:
 * <ul>
 *   <li>{@link #onDamaged} — called by {@code ApplyDamageHandler} after damage
 *       lands on the defender (e.g. Chesnaught's Spiky Shield).</li>
 *   <li>{@link #onKnockedOut} — called by {@code TurnManager} when a Pokémon
 *       is about to be sent to the discard (e.g. Voltorb's Destiny Burst).</li>
 * </ul>
 */
@Component
public class TriggeredAbilityDispatcher {

    /**
     * Fires all TRIGGERED abilities on {@code defender} that should react to
     * incoming damage.
     *
     * @param ctx      current game context
     * @param defender Pokémon that just received damage
     * @param attacker Pokémon that dealt the damage
     */
    public void onDamaged(GameContext ctx, InPlayPokemon defender, InPlayPokemon attacker) {
        Objects.requireNonNull(ctx, "ctx must not be null");
        Objects.requireNonNull(defender, "defender must not be null");
        dispatch(ctx, defender, new AbilityEffect.TriggerContext(attacker));
    }

    /**
     * Fires all TRIGGERED abilities on {@code knockedOut} that should react to
     * being knocked out.
     *
     * @param ctx       current game context
     * @param knockedOut Pokémon that was just knocked out
     * @param attacker  Pokémon responsible for the knockout
     */
    public void onKnockedOut(GameContext ctx, InPlayPokemon knockedOut, InPlayPokemon attacker) {
        Objects.requireNonNull(ctx, "ctx must not be null");
        Objects.requireNonNull(knockedOut, "knockedOut must not be null");
        dispatch(ctx, knockedOut, new AbilityEffect.TriggerContext(attacker));
    }

    private void dispatch(GameContext ctx, InPlayPokemon holder, AbilityEffect.TriggerContext trigger) {
        if (holder.isAbilitiesDisabled()) {
            return;
        }
        PokemonCard card = holder.getCard();
        if (card == null) {
            return;
        }
        List<Ability> abilities = card.getAbilities();
        if (abilities == null || abilities.isEmpty()) {
            return;
        }
        for (Ability ability : abilities) {
            if (ability == null || ability.getType() != AbilityType.TRIGGERED) {
                continue;
            }
            if (ability.getEffect() == null) {
                continue;
            }
            ability.getEffect().apply(ctx, holder, ability, trigger);
        }
    }
}
