package ar.edu.utn.frc.tup.piii.models.effects.trainer;

import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.TrainerEffect;

import java.util.List;

/**
 * Super Potion (xy1-128): Heal 60 damage from 1 of your Pokémon.
 * If you do, discard an Energy attached to that Pokémon.
 *
 * <p>The automated engine heals the most damaged Pokémon that has
 * at least one Energy attached.
 */
public class SuperPotionEffect implements TrainerEffect {

    private static final int HEAL_AMOUNT = 60;

    @Override
    public void apply(GameContext ctx) {
        InPlayPokemon target = findBestTarget(ctx);
        if (target == null) {
            return;
        }

        EnergyCard discarded = target.removeOneEnergy(null);
        if (discarded != null) {
            ctx.getAttackerDiscard().add(discarded);
        }

        int healed = Math.min(HEAL_AMOUNT, target.getDamageTaken());
        target.setDamageTaken(target.getDamageTaken() - healed);
    }

    private InPlayPokemon findBestTarget(GameContext ctx) {
        InPlayPokemon best = null;
        int mostDamage = 0;

        InPlayPokemon active = ctx.getAttacker();
        if (active != null && active.getDamageTaken() > 0 && !active.getEnergies().isEmpty()) {
            best = active;
            mostDamage = active.getDamageTaken();
        }

        List<InPlayPokemon> bench = ctx.getAttackerBench();
        for (InPlayPokemon benched : bench) {
            if (benched != null && benched.getDamageTaken() > mostDamage
                    && !benched.getEnergies().isEmpty()) {
                best = benched;
                mostDamage = benched.getDamageTaken();
            }
        }
        return best;
    }
}
