package ar.edu.utn.frc.tup.piii.models.effects;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.StatusCondition;
import ar.edu.utn.frc.tup.piii.models.effects.common.AttachFromDiscardEffect;
import ar.edu.utn.frc.tup.piii.models.effects.common.CoinFlipDiscardOpponentEnergyEffect;
import ar.edu.utn.frc.tup.piii.models.effects.common.DiscardSelfEnergyEffect;
import ar.edu.utn.frc.tup.piii.models.effects.common.DrawCardsEffect;
import ar.edu.utn.frc.tup.piii.models.effects.common.HealBenchedEffect;
import ar.edu.utn.frc.tup.piii.models.effects.common.IgnoreResistanceEffect;
import ar.edu.utn.frc.tup.piii.models.effects.common.IgnoreWeaknessAndResistanceEffect;
import ar.edu.utn.frc.tup.piii.models.effects.common.PreventRetreatEffect;
import ar.edu.utn.frc.tup.piii.models.effects.common.SearchDeckAttachEnergyEffect;
import ar.edu.utn.frc.tup.piii.models.effects.common.SearchDeckPokemonToHandEffect;
import ar.edu.utn.frc.tup.piii.models.effects.common.SearchDeckSupporterToHandEffect;
import ar.edu.utn.frc.tup.piii.models.effects.common.SpreadBenchDamageEffect;
import ar.edu.utn.frc.tup.piii.models.effects.common.SwitchOpponentEffect;
import ar.edu.utn.frc.tup.piii.models.effects.common.SwitchSelfEffect;
import ar.edu.utn.frc.tup.piii.models.effects.dsl.Conditions;
import ar.edu.utn.frc.tup.piii.models.effects.dsl.Counts;
import ar.edu.utn.frc.tup.piii.models.effects.dsl.Effects;
import ar.edu.utn.frc.tup.piii.models.effects.isolated.AstonishEffect;
import ar.edu.utn.frc.tup.piii.models.effects.isolated.BotherEffect;
import ar.edu.utn.frc.tup.piii.models.effects.isolated.ClairvoyantEyeEffect;
import ar.edu.utn.frc.tup.piii.models.effects.isolated.ClampCrushEffect;
import ar.edu.utn.frc.tup.piii.models.effects.isolated.DevastatingWindEffect;
import ar.edu.utn.frc.tup.piii.models.effects.isolated.DigOutEffect;
import ar.edu.utn.frc.tup.piii.models.effects.isolated.EerieVoiceEffect;
import ar.edu.utn.frc.tup.piii.models.effects.isolated.ElectronCrushEffect;
import ar.edu.utn.frc.tup.piii.models.effects.isolated.EnergyGlideEffect;
import ar.edu.utn.frc.tup.piii.models.effects.isolated.EnergySalonEffect;
import ar.edu.utn.frc.tup.piii.models.effects.isolated.FlashNeedleEffect;
import ar.edu.utn.frc.tup.piii.models.effects.isolated.GastroAcidEffect;
import ar.edu.utn.frc.tup.piii.models.effects.isolated.GeomancyEffect;
import ar.edu.utn.frc.tup.piii.models.effects.isolated.HardenEffect;
import ar.edu.utn.frc.tup.piii.models.effects.isolated.HeartfeltSongEffect;
import ar.edu.utn.frc.tup.piii.models.effects.isolated.HocusPinkusEffect;
import ar.edu.utn.frc.tup.piii.models.effects.isolated.JoustEffect;
import ar.edu.utn.frc.tup.piii.models.effects.isolated.MadMountainEffect;
import ar.edu.utn.frc.tup.piii.models.effects.isolated.MagmaMantleEffect;
import ar.edu.utn.frc.tup.piii.models.effects.isolated.MentalPanicEffect;
import ar.edu.utn.frc.tup.piii.models.effects.isolated.MentalTrashEffect;
import ar.edu.utn.frc.tup.piii.models.effects.isolated.MetalWallopEffect;
import ar.edu.utn.frc.tup.piii.models.effects.isolated.MineEffect;
import ar.edu.utn.frc.tup.piii.models.effects.isolated.MoonblastEffect;
import ar.edu.utn.frc.tup.piii.models.effects.isolated.NastyPlotEffect;
import ar.edu.utn.frc.tup.piii.models.effects.isolated.NightClawEffect;
import ar.edu.utn.frc.tup.piii.models.effects.isolated.PickupEffect;
import ar.edu.utn.frc.tup.piii.models.effects.isolated.RecoverEffect;
import ar.edu.utn.frc.tup.piii.models.effects.isolated.RecycleEffect;
import ar.edu.utn.frc.tup.piii.models.effects.isolated.SeafaringEffect;
import ar.edu.utn.frc.tup.piii.models.effects.isolated.SpiritScreamEffect;
import ar.edu.utn.frc.tup.piii.models.effects.isolated.SweetScentEffect;
import ar.edu.utn.frc.tup.piii.models.effects.isolated.TrickyStepsEffect;
import ar.edu.utn.frc.tup.piii.models.effects.isolated.YCycloneEffect;

/**
 * Registry that resolves the {@link AttackEffect} strategy for a given card and attack index.
 *
 * <p>Mappings are keyed by the composite {@code "cardId:attackIndex"} so that a single
 * card with multiple attacks can register a different effect per attack slot.
 *
 * <p>Each attack maps to the effect that reproduces its rulebook behaviour: a composed
 * {@link Effects} DSL recipe where the vocabulary covers it, or a reusable
 * {@code effects.common} strategy otherwise.
 */
@Component
public class CardEffectRegistry {

    private final Map<String, AttackEffect> registry = new HashMap<>();

    public CardEffectRegistry() {
        registerCommonEffects();
    }

    /**
     * Returns the {@link AttackEffect} associated with a specific attack on a specific card.
     * Returns {@code null} when no effect has been registered yet for that combination.
     *
     * @param cardId      the card identifier (e.g., {@code "xy1-1"})
     * @param attackIndex zero-based index of the attack within the card's attack list
     * @return the registered {@link AttackEffect}, or {@code null} if none is registered
     */
    public AttackEffect getAttackEffect(String cardId, int attackIndex) {
        return registry.get(buildKey(cardId, attackIndex));
    }

    /**
     * Returns the set of {@code "cardId:attackIndex"} keys currently registered.
     *
     * <p>Test/debug accessor only — not part of the runtime contract. Used by
     * {@code CardEffectRegistryBaselineTest} to assert refactor parity.
     *
     * @return an unmodifiable view of the registered keys
     */
    Set<String> registeredKeys() {
        return Collections.unmodifiableSet(registry.keySet());
    }

    /**
     * Registers each implemented attack against the effect that reproduces its behaviour —
     * either a composed {@link Effects} DSL recipe or a reusable {@code effects.common} strategy.
     */
    private void registerCommonEffects() {
        // ── Status conditions ────────────────────────────────────────────────
        // Venusaur-EX — Poison Powder: opponent is now Poisoned.
        register("xy1-1", 0, Effects.applyStatus(StatusCondition.POISONED));
        // Beedrill — Poison Jab: opponent is now Poisoned.
        register("xy1-5", 0, Effects.applyStatus(StatusCondition.POISONED));

        // ── Self-healing ─────────────────────────────────────────────────────
        // Venusaur-EX — Jungle Hammer: heal 30 from self.
        register("xy1-1", 1, Effects.heal(30));
        // Pansage — Leech Seed: heal 10 from self.
        register("xy1-10", 1, Effects.heal(10));
        // Chesnaught — Touchdown: heal 20 from self.
        register("xy1-14", 0, Effects.heal(20));

        // ── Deck search ──────────────────────────────────────────────────────
        // Illumise — Pheromotion: search deck for a Grass Pokémon, put it in hand.
        register("xy1-9", 0, new SearchDeckPokemonToHandEffect(EnergyType.GRASS));

        // ── Deck reorder ─────────────────────────────────────────────────────
        // Braixen — Clairvoyant Eye (xy1-25:0) is registered via ClairvoyantEyeEffect in
        // registerPhaseBExistingIsolated(); that registration is the active one.

        // ── Coin-flip bonus damage ───────────────────────────────────────────
        // Illumise — Quick Attack: flip a coin; if heads, +20 damage.
        register("xy1-9", 1, Effects.flip().onHeads(Effects.addDamage(20)));

        // ── Coin-flip gates (formerly one-off isolated effects) ──────────────
        // Conkeldurr — Dynamic Punch: heads → +40 damage and Confuse the defender.
        register("xy1-67", 1, Effects.flip()
                .onHeads(Effects.addDamage(40), Effects.applyStatus(StatusCondition.CONFUSED)));
        // Malamar — Distortion Beam: heads → Asleep, tails → Confused.
        register("xy1-76", 1, Effects.flip()
                .onHeads(Effects.applyStatus(StatusCondition.ASLEEP))
                .onTails(Effects.applyStatus(StatusCondition.CONFUSED)));
        // Blastoise-EX — Splash Bomb: tails → 30 damage to itself (two printings).
        register("xy1-29", 1, Effects.flip().onTails(Effects.selfDamage(30)));
        register("xy1-142", 1, Effects.flip().onTails(Effects.selfDamage(30)));

        // ── Flip-until-tails scaling (formerly one-off isolated effects) ─────
        // Whirlipede — Continuous Tumble: 30× — base damage times the heads count.
        register("xy1-52", 0, Effects.multiplyDamageBy(Counts.coinHeadsUntilTails()));
        // Corsola — Spiny Rush: +20 damage for each heads before tails.
        register("xy1-36", 1, Effects.addDamagePer(Counts.coinHeadsUntilTails(), 20));

        // ── Bucket A: DSL-shaped one-offs (replaced by recipes) ──────────────
        // M Venusaur-EX — Crisis Vine: defender is Paralyzed AND Poisoned (stacked).
        register("xy1-2", 0, Effects.seq(
                Effects.clearDefenderStatuses(),
                Effects.addStatus(StatusCondition.PARALYZED),
                Effects.addStatus(StatusCondition.POISONED)));
        // Weedle — Leaf Munch: +20 damage if defender is a Grass Pokémon.
        register("xy1-3", 0, Effects.when(Conditions.defenderType(EnergyType.GRASS))
                .then(Effects.addDamage(20)));
        // Vivillon — Colorful Wind: +30 damage per distinct basic Energy type attached.
        register("xy1-17", 1, Effects.addDamagePer(Counts.attackerDistinctEnergyTypes(), 30));
        // Corsola — Refresh: heal 30 from self and remove all Special Conditions.
        register("xy1-36", 0, Effects.seq(
                Effects.heal(30),
                Effects.clearAttackerStatuses()));
        // Raichu — Circle Circuit: base damage × attacker's bench count.
        register("xy1-43", 0, Effects.multiplyDamageBy(Counts.attackerBenchCount()));
        // Dugtrio — Earthquake: 10 damage to each of attacker's benched Pokémon.
        register("xy1-59", 0, Effects.damageEachAttackerBench(10));
        // Rhyperior — Rock Black: base × (coin heads, one coin per attached Fighting Energy).
        register("xy1-62", 0, Effects.multiplyDamageBy(
                Counts.coinHeadsOf(Counts.attackerEnergy(EnergyType.FIGHTING))));
        // Solrock — Cosmic Spin: +30 damage if Lunatone (xy1-44) is on attacker's bench.
        register("xy1-64", 0, Effects.when(Conditions.attackerBenchHasCardId("xy1-44"))
                .then(Effects.addDamage(30)));
        // Conkeldurr — Wake-Up Slap: +60 damage and clear statuses if defender has any.
        register("xy1-67", 0, Effects.when(Conditions.defenderHasAnyStatus())
                .then(Effects.seq(Effects.addDamage(60), Effects.clearDefenderStatuses())));
        // Inkay — Confusion Wave: both Active Pokémon are now Confused.
        register("xy1-74", 0, Effects.seq(
                Effects.applyStatus(StatusCondition.CONFUSED),
                Effects.applyStatusToAttacker(StatusCondition.CONFUSED)));
        // Yveltal-EX — Evil Ball: +20 damage per energy attached to either Active.
        register("xy1-79", 0, Effects.addDamagePer(Counts.bothActiveEnergyTotal(), 20));
        // Skarmory-EX — Tailspin Piledriver: +40 damage if defender already has damage.
        register("xy1-80", 1, Effects.when(Conditions.defenderDamaged())
                .then(Effects.addDamage(40)));
        // Wigglytuff — Balloon Barrage: base × total energy attached.
        register("xy1-90", 0, Effects.multiplyDamageBy(Counts.attackerEnergy(null)));
        // Tauros — Seething Anger: base × (coin heads, one coin per damage counter on self).
        register("xy1-100", 1, Effects.multiplyDamageBy(
                Counts.coinHeadsOf(Counts.attackerDamageCounters())));
        // Skitty — Heal Bell: heal 10 from each of attacker's Pokémon (active + bench).
        register("xy1-104", 0, Effects.healAllAttackerPokemon(10));
        // Stoutland — Bite Off: +60 damage if defender's card is a Pokémon-EX.
        register("xy1-110", 0, Effects.when(Conditions.defenderIsEx())
                .then(Effects.addDamage(60)));
        // Yveltal-EX (second printing) — Evil Ball: same as xy1-79[0].
        register("xy1-144", 0, Effects.addDamagePer(Counts.bothActiveEnergyTotal(), 20));
        // Skarmory-EX (second printing) — Tailspin Piledriver: same as xy1-80[1].
        register("xy1-145", 1, Effects.when(Conditions.defenderDamaged())
                .then(Effects.addDamage(40)));

        // ── Phase B: routing of existing implementations ─────────────────────
        registerPhaseBStatusApplies();
        registerPhaseBSelfStateChanges();
        registerPhaseBCoinFlipPrimitives();
        registerPhaseBMultiCoinDamage();
        registerPhaseBEnergyAndDamageCounterBonuses();
        registerPhaseBSpreadBenchAndEnergyManipulation();
        registerPhaseBExistingIsolated();
        registerPhaseCMigratedFromIsolated();
    }

    /** Plain "defender is now <status>" attacks. */
    private void registerPhaseBStatusApplies() {
        register("xy1-23", 0, Effects.applyStatus(StatusCondition.ASLEEP));        // Yawn
        register("xy1-48", 1, Effects.applyStatus(StatusCondition.POISONED));      // Poison Jab (Drapion)
        register("xy1-50", 1, Effects.applyStatus(StatusCondition.CONFUSED));      // Psybeam
        register("xy1-51", 0, Effects.applyStatus(StatusCondition.POISONED));      // Poison Sting
        register("xy1-56", 0, Effects.applyStatus(StatusCondition.CONFUSED));      // Confuse Ray
        register("xy1-82", 0, Effects.applyStatus(StatusCondition.CONFUSED));      // Metal Sound
        register("xy1-8",  1, Effects.applyStatus(StatusCondition.CONFUSED));      // Signal Beam
        register("xy1-141", 0, Effects.applyStatus(StatusCondition.POISONED));     // Poison Powder (2nd printing)
    }

    /** Self heal / recoil — single-line atoms. */
    private void registerPhaseBSelfStateChanges() {
        register("xy1-141", 1, Effects.heal(30));        // Jungle Hammer (2nd printing)
        register("xy1-95",  0, Effects.heal(30));        // Draining Kiss
        register("xy1-13",  1, Effects.selfDamage(10));  // Wood Hammer
        register("xy1-33",  0, Effects.selfDamage(10));  // Reckless Charge
        register("xy1-90",  1, Effects.selfDamage(10));  // Double-Edge
        register("xy1-100", 0, Effects.selfDamage(10));  // Take Down
    }

    /** Single coin flip: status on heads, bonus damage on heads, or cancel on tails. */
    private void registerPhaseBCoinFlipPrimitives() {
        // Coin-flip status (Paralyze on heads).
        register("xy1-16",  1, Effects.flip().onHeads(Effects.applyStatus(StatusCondition.PARALYZED)));   // Stun Spore
        register("xy1-40",  0, Effects.flip().onHeads(Effects.applyStatus(StatusCondition.PARALYZED)));   // Lick
        register("xy1-42",  0, Effects.flip().onHeads(Effects.applyStatus(StatusCondition.PARALYZED)));   // Nuzzle
        register("xy1-88",  0, Effects.flip().onHeads(Effects.applyStatus(StatusCondition.PARALYZED)));   // Body Slam
        register("xy1-101", 0, Effects.flip().onHeads(Effects.applyStatus(StatusCondition.PARALYZED)));   // Glare
        register("xy1-105", 1, Effects.flip().onHeads(Effects.applyStatus(StatusCondition.PARALYZED)));   // Fake Out
        // Coin-flip bonus damage.
        register("xy1-37",  1, Effects.flip().onHeads(Effects.addDamage(20)));   // Water Splash
        register("xy1-42",  1, Effects.flip().onHeads(Effects.addDamage(10)));   // Quick Attack
        register("xy1-65",  0, Effects.flip().onHeads(Effects.addDamage(20)));   // Pummel (Mienfoo)
        register("xy1-66",  0, Effects.flip().onHeads(Effects.addDamage(20)));   // Pummel (Mienshao)
        register("xy1-102", 0, Effects.flip().onHeads(Effects.addDamage(30)));   // Aerial Ace
        register("xy1-109", 1, Effects.flip().onHeads(Effects.addDamage(20)));   // Jump On
        // Coin-flip cancel on tails.
        register("xy1-106", 0, Effects.flip().onTails(Effects.cancelDamage()));  // Hyper Fang
        // Bounce: heads → switch self with a benched Pokémon.
        register("xy1-39", 0, Effects.flip().onHeads(new SwitchSelfEffect()));   // Bounce
    }

    /** Multi-coin × damage and per-heads bonus scaling. */
    private void registerPhaseBMultiCoinDamage() {
        register("xy1-12",  0, Effects.multiplyDamageBy(Counts.coinHeads(4)));   // Pin Missile
        register("xy1-32",  1, Effects.multiplyDamageBy(Counts.coinHeads(5)));   // Spike Cannon
        register("xy1-84",  0, Effects.multiplyDamageBy(Counts.coinHeads(2)));   // Dual Blades
        register("xy1-98",  0, Effects.multiplyDamageBy(Counts.coinHeads(2)));   // Double Hit
        register("xy1-107", 0, Effects.multiplyDamageBy(Counts.coinHeads(2)));   // Double Headbutt
        register("xy1-53",  0, Effects.addDamagePer(Counts.coinHeads(2), 20));   // Random Peck
        register("xy1-91",  1, Effects.addDamagePer(Counts.coinHeads(2), 20));   // Slap Down
        register("xy1-99",  1, Effects.addDamagePer(Counts.coinHeads(2), 20));   // Endeavor
    }

    /** +N per attached energy / per damage counter / energy-presence gate. */
    private void registerPhaseBEnergyAndDamageCounterBonuses() {
        register("xy1-26", 0, Effects.addDamagePer(Counts.attackerEnergy(EnergyType.FIRE), 20));   // Blaze Ball
        register("xy1-35", 1, Effects.addDamagePer(Counts.attackerEnergy(EnergyType.WATER), 20));  // Hydro Pump
        register("xy1-99",  0, Effects.addDamagePer(Counts.attackerDamageCounters(), 10));         // Rage
        register("xy1-101", 1, Effects.addDamagePer(Counts.defenderDamageCounters(), 10));         // Second Bite
        register("xy1-34", 1, Effects.when(Conditions.attackerHasEnergyOf(EnergyType.PSYCHIC))
                .then(Effects.addDamage(30)));   // Core Splash
    }

    /** Bench-spread damage, bench-heal, energy moves, draws, switches, retreat lock. */
    private void registerPhaseBSpreadBenchAndEnergyManipulation() {
        register("xy1-7",   1, new SpreadBenchDamageEffect(10, 1));   // Mach Punch
        register("xy1-30",  0, new SpreadBenchDamageEffect(30, 2));   // Hydro Bombard
        register("xy1-55",  0, new SpreadBenchDamageEffect(20, 2));   // Tree Slam
        register("xy1-97",  0, new SpreadBenchDamageEffect(30, 1));   // Break Through
        register("xy1-110", 1, new SpreadBenchDamageEffect(20, 1));   // Wild Barking
        register("xy1-146", 0, new SpreadBenchDamageEffect(30, 1));   // Break Through (2nd printing)
        register("xy1-91",  0, new HealBenchedEffect(60));            // Massage
        register("xy1-63",  0, new DrawCardsEffect(2));               // Double Draw
        register("xy1-68",  0, new DrawCardsEffect(1));               // Filch
        register("xy1-113", 0, new DrawCardsEffect(1));               // Me First
        register("xy1-20",  0, new DiscardSelfEnergyEffect(null, false));                 // Flamethrower (Charizard)
        register("xy1-22",  1, new DiscardSelfEnergyEffect(null, false));                 // Fireworks
        register("xy1-23",  1, new DiscardSelfEnergyEffect(EnergyType.FIRE, false));      // Flamethrower (Pyroar)
        register("xy1-25",  1, new DiscardSelfEnergyEffect(EnergyType.FIRE, false));      // Fire Tail Slap
        register("xy1-28",  1, new DiscardSelfEnergyEffect(EnergyType.FIRE, true));       // Flare Blitz
        register("xy1-43",  1, new DiscardSelfEnergyEffect(null, true));                  // Thunderbolt
        register("xy1-96",  1, new DiscardSelfEnergyEffect(null, false));                 // Rainbow Spear
        register("xy1-45",  0, new CoinFlipDiscardOpponentEnergyEffect());                // Eerie Impulse
        register("xy1-68",  1, new CoinFlipDiscardOpponentEnergyEffect());                // Rip Claw
        register("xy1-70",  0, new CoinFlipDiscardOpponentEnergyEffect());                // Crunch
        register("xy1-81",  0, new CoinFlipDiscardOpponentEnergyEffect());                // Cut Down
        register("xy1-114", 0, new CoinFlipDiscardOpponentEnergyEffect());                // Energy Cutoff
        register("xy1-8",   0, new SwitchOpponentEffect());                               // Luring Glow
        register("xy1-71",  1, new SwitchOpponentEffect());                               // Knock Back
        register("xy1-27",  0, new SearchDeckAttachEnergyEffect(EnergyType.FIRE, true));  // Flame Charge
        register("xy1-19",  0, new SearchDeckSupporterToHandEffect(2));                   // Lead (Cinccino)
        register("xy1-18",  0, Effects.flip().onHeads(new SearchDeckSupporterToHandEffect(1)));  // Lead (Minccino)
        register("xy1-78",  0, new AttachFromDiscardEffect(EnergyType.DARKNESS, true));   // Oblivion Wing
        register("xy1-41",  0, new IgnoreWeaknessAndResistanceEffect());                  // Mist Slash
        register("xy1-59",  1, new IgnoreResistanceEffect());                             // Rock Tumble
        register("xy1-75",  1, new IgnoreResistanceEffect());                             // Puncture (Granbull)
        register("xy1-77",  1, new IgnoreResistanceEffect());                             // Puncture (Sableye)
        register("xy1-85",  0, new IgnoreResistanceEffect());                             // Buster Swing
        register("xy1-73",  0, new PreventRetreatEffect());                               // Corner
    }

    /** Cards whose behaviour is encoded in an existing isolated/* class — register as-is. */
    private void registerPhaseBExistingIsolated() {
        register("xy1-4",   0, new HardenEffect());
        register("xy1-5",   1, new FlashNeedleEffect());
        register("xy1-21",  0, new MagmaMantleEffect());
        register("xy1-25",  0, new ClairvoyantEyeEffect());
        register("xy1-28",  0, new DevastatingWindEffect());
        register("xy1-32",  0, new ClampCrushEffect());
        register("xy1-34",  0, new RecoverEffect());
        register("xy1-35",  0, new SeafaringEffect());
        register("xy1-38",  0, new RecycleEffect());
        register("xy1-46",  0, new EnergyGlideEffect());
        register("xy1-46",  1, new ElectronCrushEffect());
        register("xy1-48",  0, new GastroAcidEffect());
        register("xy1-50",  0, new TrickyStepsEffect());
        register("xy1-54",  0, new AstonishEffect());
        register("xy1-57",  0, new EerieVoiceEffect());
        register("xy1-57",  1, new SpiritScreamEffect());
        register("xy1-58",  0, new MineEffect());
        register("xy1-60",  0, new DigOutEffect());
        register("xy1-61",  1, new MadMountainEffect());
        register("xy1-63",  1, new MoonblastEffect());
        register("xy1-71",  0, new BotherEffect());
        register("xy1-72",  1, new NastyPlotEffect());
        register("xy1-73",  1, new NightClawEffect());
        register("xy1-76",  0, new MentalTrashEffect());
        register("xy1-77",  0, new MentalPanicEffect());
        register("xy1-79",  1, new YCycloneEffect());
        register("xy1-80",  0, new JoustEffect());
        register("xy1-82",  1, new MetalWallopEffect());
        register("xy1-87",  1, new HeartfeltSongEffect());
        register("xy1-89",  1, new HocusPinkusEffect());
        register("xy1-92",  0, new SweetScentEffect());
        register("xy1-96",  0, new GeomancyEffect());
        register("xy1-105", 0, new EnergySalonEffect());
        register("xy1-112", 0, new PickupEffect());
        register("xy1-143", 0, new EnergyGlideEffect());
        register("xy1-143", 1, new ElectronCrushEffect());
        register("xy1-144", 1, new YCycloneEffect());
        register("xy1-145", 0, new JoustEffect());
    }

    /**
     * Phase C — cards whose dedicated {@code isolated/*} class has been folded back into
     * the DSL. Each entry here previously lived in {@link #registerPhaseBExistingIsolated()}
     * as a {@code new XxxEffect()} reference; the recipe below reproduces that behaviour
     * using only DSL atoms and combinators.
     */
    private void registerPhaseCMigratedFromIsolated() {
        // Quilladin — Scrunch: flip; if heads, prevent all incoming damage next turn.
        register("xy1-13", 0, Effects.flip().onHeads(Effects.attackerPreventAllIncomingDamage()));
        // Gurdurr — Hammer Arm: discard the top card of the opponent's deck.
        register("xy1-66", 1, Effects.discardDefenderDeckTop());
        // Xerneas-EX — X Blast: this Pokémon cannot use X Blast during its next turn.
        register("xy1-97", 1, Effects.attackerCantUseAttackName("X Blast"));
        // Bibarel — Hypno Headbutt: +30 damage and the attacker is now Asleep.
        register("xy1-107", 1, Effects.seq(
                Effects.addDamage(30),
                Effects.applyStatusToAttacker(StatusCondition.ASLEEP)));
        // Xerneas-EX (2nd printing) — X Blast: same as xy1-97[1].
        register("xy1-146", 1, Effects.attackerCantUseAttackName("X Blast"));

        // ── Round 2 ──────────────────────────────────────────────────────────
        // Vivillon — Conversion Powder: defender becomes Poisoned (the registered variant).
        register("xy1-17", 0, Effects.applyStatus(StatusCondition.POISONED));
        // Gogoat — Charge Dash: +20 damage and 20 self-damage.
        register("xy1-19", 1, Effects.seq(Effects.addDamage(20), Effects.selfDamage(20)));
        // Blastoise-EX — Rapid Spin: switch self with bench[0], then opponent switches.
        register("xy1-29", 0, Effects.seq(new SwitchSelfEffect(), new SwitchOpponentEffect()));
        // Scolipede — Poison Ring: Poisoned + cannot retreat next turn.
        register("xy1-53", 1, Effects.seq(
                Effects.applyStatus(StatusCondition.POISONED),
                new PreventRetreatEffect()));
        // Rhyperior — Rock Wrecker: ignore Weakness/Resistance + attacker cannot attack next turn.
        register("xy1-62", 1, Effects.seq(
                new IgnoreWeaknessAndResistanceEffect(),
                Effects.attackerCantAttack()));
        // Yveltal — Darkness Blade: flip; if tails attacker cannot attack next turn.
        register("xy1-78", 1, Effects.flip().onTails(Effects.attackerCantAttack()));
        // Aegislash — King's Shield: prevent all incoming damage + cannot use King's Shield next turn.
        register("xy1-86", 0, Effects.seq(
                Effects.attackerPreventAllIncomingDamage(),
                Effects.attackerCantUseAttackName("King's Shield")));
        // Wigglytuff — Gather Energy: search any basic Energy and attach to attacker.
        register("xy1-89", 0, new SearchDeckAttachEnergyEffect(null, true));
        // Bunnelby — Dig: flip; if heads, prevent all effects on attacker next turn.
        register("xy1-111", 0, Effects.flip().onHeads(Effects.attackerPreventAllEffects()));
        // Diggersby — Dig (2nd printing): same as xy1-111.
        register("xy1-112", 1, Effects.flip().onHeads(Effects.attackerPreventAllEffects()));
        // Blastoise-EX (2nd printing) — Rapid Spin: same as xy1-29.
        register("xy1-142", 0, Effects.seq(new SwitchSelfEffect(), new SwitchOpponentEffect()));
    }

    private void register(String cardId, int attackIndex, AttackEffect effect) {
        registry.put(buildKey(cardId, attackIndex), effect);
    }

    private String buildKey(String cardId, int attackIndex) {
        return cardId + ":" + attackIndex;
    }
}
