package ar.edu.utn.frc.tup.piii.engine.bot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.frc.tup.piii.dtos.engine.GameAction;
import ar.edu.utn.frc.tup.piii.models.Attack;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.PokemonStage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GameStateEvaluatorTest {

    @Test
    void evaluateRewardsHavingFewerPrizesLeft() {
        GameContext lateGame = baseContext();
        // Bot has only 1 prize left, opponent still has 6 → strongly favours bot.
        lateGame.setAttackerPrizes(prizes(1));
        lateGame.setDefenderPrizes(prizes(6));

        GameContext earlyGame = baseContext();
        earlyGame.setAttackerPrizes(prizes(6));
        earlyGame.setDefenderPrizes(prizes(6));

        assertTrue(GameStateEvaluator.evaluate(lateGame) > GameStateEvaluator.evaluate(earlyGame),
                "Taking prize cards should improve the bot's score");
    }

    @Test
    void evaluateDeclaresWinWhenBotTookAllPrizes() {
        GameContext won = baseContext();
        won.setAttackerPrizes(prizes(0));
        won.setDefenderPrizes(prizes(6));

        int score = GameStateEvaluator.evaluate(won);
        // 6 prizes × 1000 weight + 10_000 win bonus = 16_000 baseline minimum
        assertTrue(score >= 16_000, "Winning bot must score above the prize+win threshold");
    }

    @Test
    void scoreActionPrefersAttackOverEndTurn() {
        PokemonCard activeCard = PokemonCard.builder()
                .hp(100)
                .stage(PokemonStage.BASIC)
                .attacks(List.of())
                .build();
        GameContext context = baseContext();
        context.setAttacker(new InPlayPokemon(activeCard));
        context.setDefender(new InPlayPokemon(activeCard));

        int endTurnScore = GameStateEvaluator.scoreAction(context,
                ar.edu.utn.frc.tup.piii.dtos.engine.GameAction.endTurn());
        int advanceScore = GameStateEvaluator.scoreAction(context,
                ar.edu.utn.frc.tup.piii.dtos.engine.GameAction.advanceToAttack());

        assertTrue(advanceScore > endTurnScore,
                "Advancing the phase should never be worse than passing the turn");
    }

    private GameContext baseContext() {
        GameContext context = new GameContext();
        context.setAttackerPrizes(prizes(6));
        context.setDefenderPrizes(prizes(6));
        return context;
    }

    private List<Card> prizes(int count) {
        List<Card> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(null); // contents are irrelevant for the evaluator, only size matters
        }
        return list;
    }

    /** Sanity test that the helper method actually returns the right size. */
    @Test
    void prizesHelperHonoursRequestedSize() {
        assertEquals(3, prizes(3).size());
    }

    // ───────────────── evaluate() — additional branches ───────────────────────

    @Test
    void havingMorePokemonOnBenchImprovesScore() {
        // Bench presence (BENCH_WEIGHT = 30) protects against KO, evaluator rewards it.
        GameContext withBench = baseContext();
        withBench.setAttackerBench(List.of(
                new InPlayPokemon(basicCard("Pikachu", 60)),
                new InPlayPokemon(basicCard("Squirtle", 60))));

        GameContext emptyBench = baseContext();
        emptyBench.setAttackerBench(List.of());

        assertTrue(GameStateEvaluator.evaluate(withBench) > GameStateEvaluator.evaluate(emptyBench));
    }

    @Test
    void missingActivePokemonPenalisesTheSideWithoutOne() {
        // NO_ACTIVE_PENALTY = 200. Without an active, bot's score must drop.
        GameContext withActive = baseContext();
        withActive.setAttacker(new InPlayPokemon(basicCard("Pikachu", 60)));

        GameContext noActive = baseContext();
        noActive.setAttacker(null);

        assertTrue(GameStateEvaluator.evaluate(withActive) > GameStateEvaluator.evaluate(noActive));
    }

    @Test
    void killingOpponentActivePokemonIsRewarded() {
        // Mirror of the previous test from the defender's side: when the opponent has
        // no active, the bot gets a positive swing.
        GameContext oppNoActive = baseContext();
        oppNoActive.setDefender(null);

        GameContext oppHasActive = baseContext();
        oppHasActive.setDefender(new InPlayPokemon(basicCard("Caterpie", 30)));

        assertTrue(GameStateEvaluator.evaluate(oppNoActive) > GameStateEvaluator.evaluate(oppHasActive));
    }

    @Test
    void attachedEnergiesContributeToBoardScore() {
        InPlayPokemon withFire = new InPlayPokemon(basicCard("Charmander", 60));
        withFire.getEnergies().add(EnergyCard.builder().energyType(EnergyType.FIRE).build());
        withFire.getEnergies().add(EnergyCard.builder().energyType(EnergyType.FIRE).build());

        GameContext withEnergy = baseContext();
        withEnergy.setAttacker(withFire);
        GameContext withoutEnergy = baseContext();
        withoutEnergy.setAttacker(new InPlayPokemon(basicCard("Charmander", 60)));

        assertTrue(GameStateEvaluator.evaluate(withEnergy) > GameStateEvaluator.evaluate(withoutEnergy));
    }

    @Test
    void handCardsContributeToScore() {
        GameContext fullHand = baseContext();
        fullHand.setAttackerHand(List.of(basicCard("a", 60), basicCard("b", 60), basicCard("c", 60)));

        GameContext emptyHand = baseContext();
        emptyHand.setAttackerHand(List.of());

        assertTrue(GameStateEvaluator.evaluate(fullHand) > GameStateEvaluator.evaluate(emptyHand));
    }

    @Test
    void opponentTakingAllPrizesScoresAsBotLoss() {
        // Mirror of the win bonus: when oppPrizesLeft == 0 the bot loses, score must crash.
        GameContext lost = baseContext();
        lost.setAttackerPrizes(prizes(6));
        lost.setDefenderPrizes(prizes(0));

        int score = GameStateEvaluator.evaluate(lost);
        // 0 prizes taken by bot, 6 by opponent → -6 * PRIZE_WEIGHT − WIN_BONUS = -16_000.
        assertTrue(score < 0 && Math.abs(score) >= 10_000,
                "Defender clearing all prizes must drive the score deeply negative; got " + score);
    }

    // ───────────────── scoreAction() — every branch ───────────────────────────

    @Test
    void scoreActionWithNullActionReturnsBaseEvaluation() {
        GameContext ctx = baseContext();
        int base = GameStateEvaluator.evaluate(ctx);
        assertEquals(base, GameStateEvaluator.scoreAction(ctx, null));
    }

    @Test
    void scoreActionWithNullTypeReturnsBaseEvaluation() {
        GameContext ctx = baseContext();
        int base = GameStateEvaluator.evaluate(ctx);
        GameAction action = new GameAction();   // type defaults to null
        assertEquals(base, GameStateEvaluator.scoreAction(ctx, action));
    }

    @Test
    void scoreAttackReturnsZeroDeltaWhenAttackerIsAbsent() {
        // Without attacker the score for an ATTACK action collapses to base, no bonus.
        GameContext ctx = baseContext();
        ctx.setDefender(new InPlayPokemon(basicCard("Magikarp", 30)));
        int base = GameStateEvaluator.evaluate(ctx);
        assertEquals(base, GameStateEvaluator.scoreAction(ctx, GameAction.attack("Whatever")));
    }

    @Test
    void scoreAttackReturnsZeroDeltaWhenAttackNameDoesNotMatchAnyAttack() {
        Attack ember = attack("Ember", 30);
        InPlayPokemon attacker = new InPlayPokemon(basicCardWithAttacks("Charmander", 60, List.of(ember)));
        GameContext ctx = baseContext();
        ctx.setAttacker(attacker);
        ctx.setDefender(new InPlayPokemon(basicCard("Magikarp", 30)));

        int base = GameStateEvaluator.evaluate(ctx);
        // "Flamethrower" is not on the card → no delta.
        assertEquals(base, GameStateEvaluator.scoreAction(ctx, GameAction.attack("Flamethrower")));
    }

    @Test
    void scoreAttackAddsKnockoutBonusWhenDamageExceedsDefenderHp() {
        Attack ember = attack("Ember", 30);
        InPlayPokemon attacker = new InPlayPokemon(basicCardWithAttacks("Charmander", 60, List.of(ember)));
        InPlayPokemon defender = new InPlayPokemon(basicCard("Magikarp", 30));
        GameContext ctx = baseContext();
        ctx.setAttacker(attacker);
        ctx.setDefender(defender);

        int withKo = GameStateEvaluator.scoreAction(ctx, GameAction.attack("Ember"));
        // Increase defender HP so the same attack no longer KOs and the bonus disappears.
        InPlayPokemon tanky = new InPlayPokemon(basicCard("Snorlax", 200));
        ctx.setDefender(tanky);
        int withoutKo = GameStateEvaluator.scoreAction(ctx, GameAction.attack("Ember"));

        assertTrue(withKo > withoutKo,
                "Knockout bonus (+PRIZE_WEIGHT) must outweigh the score of a non-lethal attack");
    }

    @Test
    void scorePlayBasicRewardsCardsThatLandActiveOrFillBench() {
        PokemonCard pikachu = PokemonCard.builder()
                .id("xy1-001").name("Pikachu").hp(60).stage(PokemonStage.BASIC).attacks(List.of()).build();
        GameContext ctx = baseContext();
        ctx.setAttacker(null);                 // no active → bigger bonus inside scorePlayBasic
        ctx.setAttackerHand(List.of(pikachu));

        int score = GameStateEvaluator.scoreAction(ctx, GameAction.playBasic("xy1-001"));
        int base = GameStateEvaluator.evaluate(ctx);

        assertTrue(score > base, "Playing a basic when there is no active must improve the score");
    }

    @Test
    void scorePlayBasicReturnsBaseWhenCardIdDoesNotExistInHand() {
        GameContext ctx = baseContext();
        ctx.setAttackerHand(List.of());
        int base = GameStateEvaluator.evaluate(ctx);
        assertEquals(base, GameStateEvaluator.scoreAction(ctx, GameAction.playBasic("missing-id")));
    }

    @Test
    void scoreAttachEnergyValuesActiveTargetMoreThanBench() {
        EnergyCard fire = EnergyCard.builder().id("e1").energyType(EnergyType.FIRE).build();
        InPlayPokemon active = new InPlayPokemon(basicCard("Charmander", 60));
        InPlayPokemon benched = new InPlayPokemon(basicCard("Squirtle", 60));
        GameContext ctx = baseContext();
        ctx.setAttacker(active);
        ctx.setAttackerBench(List.of(benched));
        ctx.setAttackerHand(List.of(fire));

        int toActive = GameStateEvaluator.scoreAction(ctx,
                GameAction.attachEnergy("e1", active.getInstanceId()));
        int toBench = GameStateEvaluator.scoreAction(ctx,
                GameAction.attachEnergy("e1", benched.getInstanceId()));

        assertTrue(toActive > toBench, "Attaching to active should outweigh attaching to bench");
    }

    @Test
    void scoreAttachEnergyReturnsBaseWhenCardIsNotEnergy() {
        PokemonCard nonEnergy = PokemonCard.builder()
                .id("xy1-002").name("Pikachu").hp(60).stage(PokemonStage.BASIC).attacks(List.of()).build();
        GameContext ctx = baseContext();
        ctx.setAttackerHand(List.of(nonEnergy));
        int base = GameStateEvaluator.evaluate(ctx);
        assertEquals(base, GameStateEvaluator.scoreAction(ctx,
                GameAction.attachEnergy("xy1-002", "target")));
    }

    @Test
    void scoreEvolveRewardsHigherHpEvolutions() {
        PokemonCard lowHp = PokemonCard.builder()
                .id("low").name("X").hp(60).stage(PokemonStage.STAGE_1).evolvesFrom("Y").attacks(List.of()).build();
        PokemonCard highHp = PokemonCard.builder()
                .id("hi").name("Z").hp(200).stage(PokemonStage.STAGE_2).evolvesFrom("Y").attacks(List.of()).build();
        GameContext ctx = baseContext();
        ctx.setAttackerHand(List.of(lowHp, highHp));

        int lowScore = GameStateEvaluator.scoreAction(ctx, GameAction.evolve("low", "target"));
        int highScore = GameStateEvaluator.scoreAction(ctx, GameAction.evolve("hi", "target"));

        assertTrue(highScore > lowScore, "Stage 2s with more HP should outrank weaker evolutions");
    }

    @Test
    void scoreEvolveReturnsBaseWhenCardIsNotInHand() {
        GameContext ctx = baseContext();
        ctx.setAttackerHand(List.of());
        int base = GameStateEvaluator.evaluate(ctx);
        assertEquals(base, GameStateEvaluator.scoreAction(ctx, GameAction.evolve("nope", "target")));
    }

    @Test
    void trainerCardsHaveAFixedPositiveDelta() {
        // PLAY_SUPPORTER/ITEM/STADIUM/ATTACH_TOOL all add a flat +10 bonus regardless of card.
        GameContext ctx = baseContext();
        int base = GameStateEvaluator.evaluate(ctx);

        assertEquals(base + 10, GameStateEvaluator.scoreAction(ctx, GameAction.playSupporter("x")));
        assertEquals(base + 10, GameStateEvaluator.scoreAction(ctx, GameAction.playItem("x")));
        assertEquals(base + 10, GameStateEvaluator.scoreAction(ctx, GameAction.playStadium("x")));
        assertEquals(base + 10, GameStateEvaluator.scoreAction(ctx, GameAction.attachPokemonTool("x", "y")));
    }

    @Test
    void endTurnIsPenalisedWhileAdvanceToAttackIsRewarded() {
        // We already test the relative order in the existing test; here we pin the
        // exact deltas because they encode the bot's bias toward action.
        GameContext ctx = baseContext();
        int base = GameStateEvaluator.evaluate(ctx);
        assertEquals(base + 1, GameStateEvaluator.scoreAction(ctx, GameAction.advanceToAttack()));
        assertEquals(base - 5, GameStateEvaluator.scoreAction(ctx, GameAction.endTurn()));
    }

    @Test
    void coinFlipChoicesAreNeutral() {
        // These actions appear during opening only — the heuristic intentionally treats
        // them as no-op (returns the base evaluation unchanged).
        GameContext ctx = baseContext();
        int base = GameStateEvaluator.evaluate(ctx);

        assertEquals(base, GameStateEvaluator.scoreAction(ctx, GameAction.chooseCoinSide("HEADS")));
        assertEquals(base, GameStateEvaluator.scoreAction(ctx, GameAction.chooseFirstTurn("p1")));
    }

    @Test
    void unsupportedActionTypeRaisesIllegalArgument() {
        // Any action whose type is not switched on must surface a clear error.
        // RESOLVE_PLAYER_CHOICE is currently unhandled by the evaluator.
        GameContext ctx = baseContext();
        assertThrows(IllegalArgumentException.class,
                () -> GameStateEvaluator.scoreAction(ctx,
                        GameAction.resolvePlayerChoice(List.of("any"))));
    }

    // ───────────────── helpers ────────────────────────────────────────────────

    private PokemonCard basicCard(String name, int hp) {
        return PokemonCard.builder()
                .name(name).hp(hp).stage(PokemonStage.BASIC).attacks(List.of()).build();
    }

    private PokemonCard basicCardWithAttacks(String name, int hp, List<Attack> attacks) {
        return PokemonCard.builder()
                .name(name).hp(hp).stage(PokemonStage.BASIC).attacks(attacks).build();
    }

    private Attack attack(String name, int damage) {
        return Attack.builder().name(name).baseDamage(damage)
                .energyCost(Map.of(EnergyType.COLORLESS, 1)).build();
    }
}
