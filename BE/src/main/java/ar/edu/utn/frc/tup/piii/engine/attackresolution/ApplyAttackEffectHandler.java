package ar.edu.utn.frc.tup.piii.engine.attackresolution;

import ar.edu.utn.frc.tup.piii.models.effects.CardEffectRegistry;
import ar.edu.utn.frc.tup.piii.models.Attack;
import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves and applies the selected attack's effect strategy.
 *
 * <p>Inline effects declared directly on the {@link Attack} take precedence over registry lookups.
 * When the defender prevents all incoming effects, this handler becomes a no-op.
 */
public final class ApplyAttackEffectHandler extends AbstractAttackResolutionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApplyAttackEffectHandler.class);

    private final CardEffectRegistry cardEffectRegistry;

    /**
     * Creates the handler.
     *
     * @param cardEffectRegistry strategy registry used for card-id/attack-index lookups
     * @param next next step in the chain, or {@code null} for the last handler
     */
    public ApplyAttackEffectHandler(CardEffectRegistry cardEffectRegistry, AttackResolutionHandler next) {
        super(next);
        this.cardEffectRegistry = Objects.requireNonNull(cardEffectRegistry,
                "cardEffectRegistry must not be null");
    }

    @Override
    protected void doHandle(AttackResolutionContext context) {
        if (context.getDefender().isPreventAllEffects()) {
            return;
        }

        AttackEffect effect = resolveEffect(context);
        if (effect != null) {
            effect.apply(context.getGameContext());
        } else {
            PokemonCard card = context.getAttacker().getCard();
            if (card != null) {
                int idx = card.getAttacks() != null ? card.getAttacks().indexOf(context.getAttack()) : -1;
                log.debug("No effect registered for {}:{} ({})", card.getId(), idx, context.getAttack().getName());
            }
        }
    }

    private AttackEffect resolveEffect(AttackResolutionContext context) {
        if (context.getAttack().getEffect() != null) {
            return context.getAttack().getEffect();
        }

        PokemonCard attackerCard = requireAttackerCard(context);
        List<Attack> attacks = attackerCard.getAttacks();
        if (attacks == null) {
            return null;
        }
        int attackIndex = attacks.indexOf(context.getAttack());
        if (attackIndex < 0) {
            return null;
        }
        return cardEffectRegistry.getAttackEffect(attackerCard.getId(), attackIndex);
    }

    private PokemonCard requireAttackerCard(AttackResolutionContext context) {
        PokemonCard attackerCard = context.getAttacker().getCard();
        if (attackerCard == null) {
            throw new IllegalStateException("Attacker card must not be null");
        }
        return attackerCard;
    }
}

