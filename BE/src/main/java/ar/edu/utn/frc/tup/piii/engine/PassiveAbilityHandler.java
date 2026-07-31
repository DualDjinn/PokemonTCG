package ar.edu.utn.frc.tup.piii.engine;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import ar.edu.utn.frc.tup.piii.models.Ability;
import ar.edu.utn.frc.tup.piii.models.AbilityType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;

/**
 * Applies {@link AbilityType#PASSIVE} abilities to a Pokémon while it is in
 * play. Wired into the attack-resolution pipeline so that PASSIVE effects
 * (damage boost, damage reduction, etc.) are honoured before final damage is
 * applied.
 *
 * <p>The handler honours {@link InPlayPokemon#isAbilitiesDisabled()} as the
 * single source of truth for ability suppression: when the flag is set
 * (Gastro Acid, or similar) every passive on the holder is skipped. This is
 * the only consumer of that flag in v1.
 */
@Component
public class PassiveAbilityHandler {

    /**
     * Applies offensive PASSIVE abilities of the {@code attacker} to the
     * current {@code ctx.pendingDamage}. No-op when the attacker has its
     * abilities disabled.
     *
     * @param ctx      current attack-resolution context
     * @param attacker the Pokémon whose passives are being evaluated
     */
    public void applyOffensivePassives(GameContext ctx, InPlayPokemon attacker) {
        Objects.requireNonNull(ctx, "ctx must not be null");
        Objects.requireNonNull(attacker, "attacker must not be null");
        dispatch(ctx, attacker);
    }

    /**
     * Applies defensive PASSIVE abilities of the {@code defender} to the
     * current {@code ctx.pendingDamage}. No-op when the defender has its
     * abilities disabled.
     *
     * @param ctx      current attack-resolution context
     * @param defender the Pokémon whose passives are being evaluated
     */
    public void applyDefensivePassives(GameContext ctx, InPlayPokemon defender) {
        Objects.requireNonNull(ctx, "ctx must not be null");
        Objects.requireNonNull(defender, "defender must not be null");
        dispatch(ctx, defender);
    }

    private void dispatch(GameContext ctx, InPlayPokemon holder) {
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
            if (ability == null || ability.getType() != AbilityType.PASSIVE) {
                continue;
            }
            if (ability.getEffect() == null) {
                continue;
            }
            ability.getEffect().apply(ctx, holder, ability);
        }
    }
}
