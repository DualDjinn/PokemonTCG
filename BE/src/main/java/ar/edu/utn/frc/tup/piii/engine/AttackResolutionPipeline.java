package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.engine.attackresolution.ApplyAttackEffectHandler;
import ar.edu.utn.frc.tup.piii.engine.attackresolution.ApplyDamageHandler;
import ar.edu.utn.frc.tup.piii.engine.attackresolution.ApplyDefenderProtectionHandler;
import ar.edu.utn.frc.tup.piii.engine.attackresolution.AttackResolutionContext;
import ar.edu.utn.frc.tup.piii.engine.attackresolution.AttackResolutionHandler;
import ar.edu.utn.frc.tup.piii.engine.attackresolution.CalculateDamageHandler;
import ar.edu.utn.frc.tup.piii.engine.attackresolution.PrepareAttackStateHandler;
import ar.edu.utn.frc.tup.piii.models.effects.CardEffectRegistry;
import ar.edu.utn.frc.tup.piii.models.Attack;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import java.util.Objects;

/**
 * Focused attack-resolution pipeline used by the turn manager.
 *
 * <p>The pipeline keeps the order explicit: initialize base damage, apply the
 * registered attack effect, calculate final damage and finally mutate the
 * defender. This prevents {@link GameEngine} from accumulating engine details.
 */
class AttackResolutionPipeline {

    private final DamageCalculator damageCalculator;
    private final CardEffectRegistry cardEffectRegistry;
    private final PassiveAbilityHandler passiveAbilityHandler;
    private final TriggeredAbilityDispatcher triggeredAbilityDispatcher;
    private final AttackResolutionHandler attackResolutionChain;

    AttackResolutionPipeline(DamageCalculator damageCalculator,
                             CardEffectRegistry cardEffectRegistry,
                             PassiveAbilityHandler passiveAbilityHandler,
                             TriggeredAbilityDispatcher triggeredAbilityDispatcher) {
        this.damageCalculator = damageCalculator;
        this.cardEffectRegistry = cardEffectRegistry;
        this.passiveAbilityHandler = passiveAbilityHandler;
        this.triggeredAbilityDispatcher = triggeredAbilityDispatcher;
        this.attackResolutionChain = buildResolutionChain();
    }

    /**
     * Resolves one attack against the current attacker/defender pair.
     *
     * @param context oriented match context
     * @param attack attack selected by the current player
     * @return final damage applied to the defender
     */
    int resolve(GameContext context, Attack attack) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(attack, "attack must not be null");

        InPlayPokemon attacker = requireCombatant(context.getAttacker(), "Attacker");
        InPlayPokemon defender = requireCombatant(context.getDefender(), "Defender");

        AttackResolutionContext resolutionContext = new AttackResolutionContext(
                context, attack, attacker, defender);
        attackResolutionChain.handle(resolutionContext);
        return resolutionContext.getFinalDamage();
    }

    private AttackResolutionHandler buildResolutionChain() {
        return new PrepareAttackStateHandler(
                new ApplyAttackEffectHandler(
                        cardEffectRegistry,
                        new CalculateDamageHandler(
                                damageCalculator,
                                passiveAbilityHandler,
                                new ApplyDefenderProtectionHandler(
                                        new ApplyDamageHandler(triggeredAbilityDispatcher, null)
                                )
                        )
                )
        );
    }

    private InPlayPokemon requireCombatant(InPlayPokemon pokemon, String role) {
        if (pokemon == null) {
            throw new IllegalStateException(role + " must not be null");
        }
        return pokemon;
    }
}
