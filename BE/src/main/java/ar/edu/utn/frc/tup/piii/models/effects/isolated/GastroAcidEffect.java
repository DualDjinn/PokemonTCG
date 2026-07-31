package ar.edu.utn.frc.tup.piii.models.effects.isolated;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.GameContext;

/**
 * Arbok — Gastro Acid: the defending Pokémon has no Abilities until the end of the next turn.
 */
public class GastroAcidEffect implements AttackEffect {

    @Override
    public void apply(GameContext ctx) {
        ctx.getDefender().setAbilitiesDisabled(true);
    }
}
