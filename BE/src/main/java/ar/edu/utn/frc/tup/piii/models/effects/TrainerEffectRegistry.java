package ar.edu.utn.frc.tup.piii.models.effects;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import ar.edu.utn.frc.tup.piii.models.TrainerEffect;
import ar.edu.utn.frc.tup.piii.models.effects.trainer.CassiusEffect;
import ar.edu.utn.frc.tup.piii.models.effects.trainer.EvosodaEffect;
import ar.edu.utn.frc.tup.piii.models.effects.trainer.FairyGardenEffect;
import ar.edu.utn.frc.tup.piii.models.effects.trainer.GreatBallEffect;
import ar.edu.utn.frc.tup.piii.models.effects.trainer.HardCharmEffect;
import ar.edu.utn.frc.tup.piii.models.effects.trainer.MaxReviveEffect;
import ar.edu.utn.frc.tup.piii.models.effects.trainer.MuscleBandEffect;
import ar.edu.utn.frc.tup.piii.models.effects.trainer.ProfessorSycamoreEffect;
import ar.edu.utn.frc.tup.piii.models.effects.trainer.ProfessorsLetterEffect;
import ar.edu.utn.frc.tup.piii.models.effects.trainer.RedCardEffect;
import ar.edu.utn.frc.tup.piii.models.effects.trainer.RollerSkatesEffect;
import ar.edu.utn.frc.tup.piii.models.effects.trainer.ShadowCircleEffect;
import ar.edu.utn.frc.tup.piii.models.effects.trainer.ShaunaEffect;
import ar.edu.utn.frc.tup.piii.models.effects.trainer.SuperPotionEffect;
import ar.edu.utn.frc.tup.piii.models.effects.trainer.TeamFlareGruntEffect;

/**
 * Registry that resolves the {@link TrainerEffect} strategy for a given Trainer card.
 *
 * <p>Mappings are keyed by card ID (e.g. {@code "xy1-122"} for Professor Sycamore).
 * Each Trainer card maps to the effect that reproduces its rulebook behaviour.
 *
 * <p>Analogous to {@link CardEffectRegistry} for attack effects.
 */
@Component
public class TrainerEffectRegistry {

    private final Map<String, TrainerEffect> registry = new HashMap<>();

    public TrainerEffectRegistry() {
        registerAllEffects();
    }

    /**
     * Returns the {@link TrainerEffect} associated with a specific Trainer card.
     * Returns {@code null} when no effect has been registered for that card.
     *
     * @param cardId the card identifier (e.g. {@code "xy1-122"})
     * @return the registered {@link TrainerEffect}, or {@code null} if none
     */
    public TrainerEffect getTrainerEffect(String cardId) {
        return registry.get(cardId);
    }

    /**
     * Returns the set of card IDs currently registered.
     *
     * @return an unmodifiable view of the registered keys
     */
    public Set<String> registeredKeys() {
        return Collections.unmodifiableSet(registry.keySet());
    }

    private void registerAllEffects() {
        // ── Supporters ──────────────────────────────────────────────────────
        register("xy1-115", new CassiusEffect());
        register("xy1-122", new ProfessorSycamoreEffect());
        register("xy1-127", new ShaunaEffect());
        register("xy1-129", new TeamFlareGruntEffect());

        // ── Items ────────────────────────────────────────────────────────────
        register("xy1-116", new EvosodaEffect());
        register("xy1-118", new GreatBallEffect());
        register("xy1-120", new MaxReviveEffect());
        register("xy1-123", new ProfessorsLetterEffect());
        register("xy1-124", new RedCardEffect());
        register("xy1-125", new RollerSkatesEffect());
        register("xy1-128", new SuperPotionEffect());

        // ── Stadiums ─────────────────────────────────────────────────────────
        register("xy1-117", new FairyGardenEffect());
        register("xy1-126", new ShadowCircleEffect());

        // ── Pokémon Tools ────────────────────────────────────────────────────
        register("xy1-119", new HardCharmEffect());
        register("xy1-121", new MuscleBandEffect());
    }

    private void register(String cardId, TrainerEffect effect) {
        registry.put(cardId, effect);
    }
}
