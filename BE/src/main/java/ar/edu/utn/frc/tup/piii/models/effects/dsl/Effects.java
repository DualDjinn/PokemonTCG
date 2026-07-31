package ar.edu.utn.frc.tup.piii.models.effects.dsl;

import ar.edu.utn.frc.tup.piii.engine.events.StatusAppliedGameEvent;
import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.StatusCondition;
import ar.edu.utn.frc.tup.piii.models.effects.common.CompositeAttackEffect;

/**
 * Fluent factory for composing atomic {@link AttackEffect}s.
 *
 * <p>This is the entry point of the attack-effect DSL. Each method returns a small,
 * reusable {@link AttackEffect}; the combinators ({@link #flip()}, {@link #when(Condition)},
 * {@link #seq(AttackEffect...)}) assemble them into the composite behaviour of one attack.
 * Card recipes are expected to {@code import static} these methods so an attack reads as,
 * e.g., {@code flip().onHeads(addDamage(40), applyStatus(CONFUSED))}.
 *
 * <p>Composition is delegated to the existing {@link CompositeAttackEffect} (the Composite
 * pattern); this class is only the ergonomic Builder layer on top of it.
 */
public final class Effects {

    private Effects() {
        // Static factory; not instantiable.
    }

    /**
     * Adds a fixed amount to the pending damage.
     *
     * @param amount damage to add
     * @return an effect that raises {@code pendingDamage} by {@code amount}
     */
    public static AttackEffect addDamage(int amount) {
        return ctx -> ctx.setPendingDamage(ctx.getPendingDamage() + amount);
    }

    /**
     * Adds {@code perUnit × source.count(ctx)} to the pending damage.
     *
     * <p>Unifies every "+N per coin head / per energy / per damage counter / per benched
     * Pokémon" attack behind one method.
     *
     * @param source  the quantity to scale by
     * @param perUnit damage added per counted unit
     * @return an effect that raises {@code pendingDamage} by {@code perUnit × count}
     */
    public static AttackEffect addDamagePer(CountSource source, int perUnit) {
        return ctx -> ctx.setPendingDamage(ctx.getPendingDamage() + perUnit * source.count(ctx));
    }

    /**
     * Multiplies the pending damage by {@code source.count(ctx)}.
     *
     * <p>Unifies every "base × number of heads / × benched Pokémon" attack.
     *
     * @param source the multiplier quantity
     * @return an effect that multiplies {@code pendingDamage} by {@code count}
     */
    public static AttackEffect multiplyDamageBy(CountSource source) {
        return ctx -> ctx.setPendingDamage(ctx.getPendingDamage() * source.count(ctx));
    }

    /**
     * Deals fixed recoil damage to the attacking Pokémon (no Weakness/Resistance).
     *
     * @param amount recoil damage
     * @return an effect that damages the attacker
     */
    public static AttackEffect selfDamage(int amount) {
        return ctx -> ctx.dealDamageToAttacker(amount);
    }

    /**
     * Heals a fixed amount of damage from the attacking Pokémon, floored at zero.
     *
     * @param amount damage to heal
     * @return an effect that lowers the attacker's {@code damageTaken}
     */
    public static AttackEffect heal(int amount) {
        return ctx -> {
            int current = ctx.getAttacker().getDamageTaken();
            ctx.getAttacker().setDamageTaken(Math.max(0, current - amount));
        };
    }

    /**
     * Applies a Special Condition to the defending Pokémon.
     *
     * @param condition the status to apply
     * @return an effect that sets the defender's status
     */
    public static AttackEffect applyStatus(StatusCondition condition) {
        return ctx -> {
            ctx.getDefender().applyStatus(condition);
            String name = ctx.getDefender().getCard() != null ? ctx.getDefender().getCard().getName() : "?";
            ctx.recordEvent(new StatusAppliedGameEvent(name, condition.name()));
        };
    }

    /**
     * Sets pending damage to zero. Used for "this attack does nothing" outcomes such as
     * Hyper Fang on tails.
     *
     * @return an effect that nullifies the attack's damage for this resolution
     */
    public static AttackEffect cancelDamage() {
        return ctx -> ctx.setPendingDamage(0);
    }

    /**
     * Adds a Special Condition to the defending Pokémon without clearing existing ones.
     *
     * <p>Use this when an attack explicitly stacks conditions (e.g., Crisis Vine —
     * Paralyzed and Poisoned simultaneously). For the replace-existing semantics,
     * use {@link #applyStatus(StatusCondition)} instead.
     *
     * @param condition the status to add
     * @return an effect that appends the status on the defender
     */
    public static AttackEffect addStatus(StatusCondition condition) {
        return ctx -> {
            ctx.getDefender().addStatus(condition);
            String name = ctx.getDefender().getCard() != null ? ctx.getDefender().getCard().getName() : "?";
            ctx.recordEvent(new StatusAppliedGameEvent(name, condition.name()));
        };
    }

    /**
     * Applies a Special Condition to the attacking Pokémon itself.
     *
     * @param condition the status to apply
     * @return an effect that sets the attacker's status
     */
    public static AttackEffect applyStatusToAttacker(StatusCondition condition) {
        return ctx -> ctx.getAttacker().applyStatus(condition);
    }

    /**
     * Removes all Special Conditions from the attacking Pokémon.
     *
     * @return an effect that clears the attacker's statuses
     */
    public static AttackEffect clearAttackerStatuses() {
        return ctx -> ctx.getAttacker().clearStatuses();
    }

    /**
     * Removes all Special Conditions from the defending Pokémon.
     *
     * @return an effect that clears the defender's statuses
     */
    public static AttackEffect clearDefenderStatuses() {
        return ctx -> ctx.getDefender().clearStatuses();
    }

    /**
     * Heals {@code amount} damage from every Pokémon the attacker controls (active + bench),
     * floored at zero per Pokémon.
     *
     * @param amount damage to heal from each of the attacker's Pokémon
     * @return an effect that heals across the attacker's whole side
     */
    public static AttackEffect healAllAttackerPokemon(int amount) {
        return ctx -> {
            heal(amount).apply(ctx);
            ctx.getAttackerBench().stream()
                    .filter(java.util.Objects::nonNull)
                    .forEach(p -> p.setDamageTaken(Math.max(0, p.getDamageTaken() - amount)));
        };
    }

    /**
     * Deals {@code amount} damage to each Pokémon on the attacker's own bench
     * (no Weakness/Resistance applied).
     *
     * @param amount damage to put on each of the attacker's benched Pokémon
     * @return an effect that damages every attacker bench slot
     */
    public static AttackEffect damageEachAttackerBench(int amount) {
        return ctx -> ctx.getAttackerBench().stream()
                .filter(java.util.Objects::nonNull)
                .forEach(p -> p.setDamageTaken(p.getDamageTaken() + amount));
    }

    /**
     * Locks the attacking Pokémon out of reusing the named attack on its next turn.
     *
     * <p>Used by self-cooldown attacks (e.g. X Blast, King's Shield) where the rulebook
     * forbids the same attack being declared on the following turn.
     *
     * @param attackName the printed attack name to block — must match the card's attack name exactly
     * @return an effect that sets {@code cantUseAttackName} on the attacker
     */
    public static AttackEffect attackerCantUseAttackName(String attackName) {
        return ctx -> ctx.getAttacker().setCantUseAttackName(attackName);
    }

    /**
     * Prevents all damage done to the attacking Pokémon by attacks during the opponent's
     * next turn.
     *
     * <p>Used by shield-style effects (e.g. King's Shield outright, Scrunch on heads).
     * Effects other than damage are unaffected.
     *
     * @return an effect that sets {@code preventAllIncomingDamage} on the attacker
     */
    public static AttackEffect attackerPreventAllIncomingDamage() {
        return ctx -> ctx.getAttacker().setPreventAllIncomingDamage(true);
    }

    /**
     * Discards the top card of the defender's deck.
     *
     * <p>Used by "mill 1" attacks (e.g. Hammer Arm) and as a building block for
     * coin-gated variants. Safely no-ops when the defender's deck is empty.
     *
     * @return an effect that moves the defender's top deck card to their discard pile
     */
    public static AttackEffect discardDefenderDeckTop() {
        return ctx -> ctx.defenderDiscardTopCard();
    }

    /**
     * Locks the attacking Pokémon out of using any attack on its next turn.
     *
     * <p>Used by self-disabling recoils (e.g. Rock Wrecker outright, Darkness Blade on tails).
     * Differs from {@link #attackerCantUseAttackName(String)} which only blocks one named attack.
     *
     * @return an effect that sets {@code cantAttack} on the attacker
     */
    public static AttackEffect attackerCantAttack() {
        return ctx -> ctx.getAttacker().setCantAttack(true);
    }

    /**
     * Prevents all effects of attacks — including damage — done to the attacking Pokémon
     * during the opponent's next turn.
     *
     * <p>Stronger than {@link #attackerPreventAllIncomingDamage()}: that one blocks damage only,
     * while this one also blocks side effects (status conditions, energy discard, etc.).
     * Used by Dig (on heads) and Flash Needle (when all three coins land heads).
     *
     * @return an effect that sets {@code preventAllEffects} on the attacker
     */
    public static AttackEffect attackerPreventAllEffects() {
        return ctx -> ctx.getAttacker().setPreventAllEffects(true);
    }

    /**
     * Combines effects into a single sequential {@link AttackEffect} (the Composite).
     *
     * @param effects effects to run in order
     * @return a composite effect; an empty argument list yields a no-op
     */
    public static AttackEffect seq(AttackEffect... effects) {
        return new CompositeAttackEffect(effects);
    }

    /**
     * Begins a single coin-flip branch: {@code flip().onHeads(...).onTails(...)}.
     *
     * @return a fresh {@link FlipBuilder}
     */
    public static FlipBuilder flip() {
        return new FlipBuilder();
    }

    /**
     * Begins a conditional branch: {@code when(cond).then(...).otherwise(...)}.
     *
     * @param condition the predicate gating the {@code then} branch
     * @return a fresh {@link ConditionBuilder}
     */
    public static ConditionBuilder when(Condition condition) {
        return new ConditionBuilder(condition);
    }
}
