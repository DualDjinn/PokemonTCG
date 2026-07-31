package ar.edu.utn.frc.tup.piii.engine.attackresolution;

import ar.edu.utn.frc.tup.piii.engine.DamageCalculator;
import ar.edu.utn.frc.tup.piii.engine.PassiveAbilityHandler;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import java.util.Objects;

/**
 * Delegates Weakness, Resistance and modifier calculation to
 * {@link DamageCalculator} and folds in the
 * {@link PassiveAbilityHandler} hooks for offensive and defensive passive
 * abilities.
 *
 * <p>Resolution order inside this handler:
 * <ol>
 *     <li>Offensive PASSIVE abilities of the attacker (added to
 *     {@code ctx.pendingDamage} before base damage is read).</li>
 *     <li>Standard XY1 pipeline (base → Weakness → Resistance → extra
 *     modifiers) executed by {@link DamageCalculator}.</li>
 *     <li>Defensive PASSIVE abilities of the defender (subtract from the
 *     post-pipeline damage, floored at {@code 0}).</li>
 * </ol>
 */
public final class CalculateDamageHandler extends AbstractAttackResolutionHandler {

    private final DamageCalculator damageCalculator;
    private final PassiveAbilityHandler passiveAbilityHandler;

    /**
     * Creates the handler.
     *
     * @param damageCalculator       deterministic damage calculator
     * @param passiveAbilityHandler  handler that applies PASSIVE abilities
     * @param next                   next step in the chain, or {@code null} for
     *                               the last handler
     */
    public CalculateDamageHandler(DamageCalculator damageCalculator,
                                  PassiveAbilityHandler passiveAbilityHandler,
                                  AttackResolutionHandler next) {
        super(next);
        this.damageCalculator = Objects.requireNonNull(damageCalculator, "damageCalculator must not be null");
        this.passiveAbilityHandler = Objects.requireNonNull(passiveAbilityHandler,
                "passiveAbilityHandler must not be null");
    }

    @Override
    protected void doHandle(AttackResolutionContext context) {
        GameContext gameContext = context.getGameContext();
        passiveAbilityHandler.applyOffensivePassives(gameContext, context.getAttacker());
        int calculatedDamage = damageCalculator.calculateDamage(
                context.getAttacker(),
                context.getDefender(),
                gameContext
        );
        // Muscle Band (xy1-121): +20 damage from the attacker's tool, applied AFTER
        // weakness/resistance because the rulebook clause "before applying Weakness
        // and Resistance" only governs scope, not order — the tool boost stacks on
        // the calculator output without being doubled by Weakness.
        if (hasTool(context.getAttacker(), "xy1-121")) {
            calculatedDamage += 20;
        }
        gameContext.setPendingDamage(calculatedDamage);
        passiveAbilityHandler.applyDefensivePassives(gameContext, context.getDefender());
        // Hard Charm (xy1-119): -20 incoming damage from the defender's tool.
        if (hasTool(context.getDefender(), "xy1-119")) {
            gameContext.setPendingDamage(gameContext.getPendingDamage() - 20);
        }
        int finalDamage = Math.max(0, gameContext.getPendingDamage());
        context.setCalculatedDamage(finalDamage);
        context.setFinalDamage(finalDamage);
    }

    private static boolean hasTool(ar.edu.utn.frc.tup.piii.models.InPlayPokemon p, String cardId) {
        return p != null && p.getTool() != null && cardId.equals(p.getTool().getId());
    }
}

