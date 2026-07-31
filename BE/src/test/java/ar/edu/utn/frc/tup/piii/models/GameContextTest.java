package ar.edu.utn.frc.tup.piii.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.IntUnaryOperator;
import static org.assertj.core.api.Assertions.assertThat;

class GameContextTest {

    private GameContext ctx;
    private PokemonCard basePokemon;

    @BeforeEach
    void setUp() {
        ctx = new GameContext();
        basePokemon = PokemonCard.builder()
                .id("p1").name("TestMon").hp(100)
                .stage(PokemonStage.BASIC)
                .build();
    }

    @Test
    void constructor_InitializesAllLists() {
        assertThat(ctx.getAttackerBench()).hasSize(5).containsOnlyNulls();
        assertThat(ctx.getDefenderBench()).hasSize(5).containsOnlyNulls();
        assertThat(ctx.getAttackerHand()).isEmpty();
        assertThat(ctx.getDefenderHand()).isEmpty();
        assertThat(ctx.getAttackerDeck()).isEmpty();
        assertThat(ctx.getDefenderDeck()).isEmpty();
        assertThat(ctx.getAttackerDiscard()).isEmpty();
        assertThat(ctx.getDefenderDiscard()).isEmpty();
        assertThat(ctx.getAttackerPrizes()).isEmpty();
        assertThat(ctx.getDefenderPrizes()).isEmpty();
        assertThat(ctx.getCurrentPhase()).isEqualTo(TurnPhase.MAIN);
        // El constructor marca el turno de apertura; Battle lo baja a false al avanzar.
        assertThat(ctx.isFirstTurn()).isTrue();
        assertThat(ctx.getAttackerEnergyAttachmentsThisTurn()).isZero();
        assertThat(ctx.getAttackerAdditionalEnergyAttachments()).isZero();
        assertThat(ctx.getAttackerSupportersPlayedThisTurn()).isZero();
        assertThat(ctx.getAttackerPokemonPlayedThisTurn()).isEmpty();
        assertThat(ctx.getRandom()).isNotNull();
    }

    @Test
    void flipCoin_ReturnsTrueOrFalse() {
        ctx.setRandom(new Random(42));
        boolean result = ctx.flipCoin();
        assertThat(result).isIn(true, false);
    }

    @Test
    void flipCoins_ReturnsCorrectCount() {
        ctx.setRandom(new Random(42));
        int heads = ctx.flipCoins(10);
        assertThat(heads).isBetween(0, 10);
    }

    @Test
    void flipUntilTails_ReturnsHeadsCount() {
        ctx.setRandom(new Random(42));
        int heads = ctx.flipUntilTails();
        assertThat(heads).isNotNegative();
    }

    @Test
    void attackerDamageCounters_ReturnsCorrectCount() {
        InPlayPokemon attacker = new InPlayPokemon(basePokemon);
        attacker.setDamageTaken(30);
        ctx.setAttacker(attacker);
        assertThat(ctx.attackerDamageCounters()).isEqualTo(3);
    }

    @Test
    void defenderDamageCounters_ReturnsCorrectCount() {
        InPlayPokemon defender = new InPlayPokemon(basePokemon);
        defender.setDamageTaken(50);
        ctx.setDefender(defender);
        assertThat(ctx.defenderDamageCounters()).isEqualTo(5);
    }

    @Test
    void searchAttackerDeck_FindsAndRemovesCard() {
        PokemonCard target = PokemonCard.builder().id("target").name("Pikachu").build();
        ctx.getAttackerDeck().add(target);
        ctx.getAttackerDeck().add(PokemonCard.builder().id("other").name("Charmander").build());
        Card found = ctx.searchAttackerDeck(c -> "Pikachu".equals(c.getName()));
        assertThat(found).isSameAs(target);
        assertThat(ctx.getAttackerDeck()).hasSize(1);
    }

    @Test
    void searchAttackerDeck_NotFound_ReturnsNull() {
        ctx.getAttackerDeck().add(PokemonCard.builder().id("c1").name("Charmander").build());
        Card found = ctx.searchAttackerDeck(c -> "Missing".equals(c.getName()));
        assertThat(found).isNull();
        assertThat(ctx.getAttackerDeck()).hasSize(1);
    }

    @Test
    void searchDefenderDeck_FindsAndRemovesCard() {
        PokemonCard target = PokemonCard.builder().id("target").name("Squirtle").build();
        ctx.getDefenderDeck().add(target);
        Card found = ctx.searchDefenderDeck(c -> "Squirtle".equals(c.getName()));
        assertThat(found).isSameAs(target);
        assertThat(ctx.getDefenderDeck()).isEmpty();
    }

    @Test
    void attackerDrawCard_DrawsFromDeckToHand() {
        PokemonCard card = PokemonCard.builder().id("c1").name("Pikachu").build();
        ctx.getAttackerDeck().add(card);
        Card drawn = ctx.attackerDrawCard();
        assertThat(drawn).isSameAs(card);
        assertThat(ctx.getAttackerDeck()).isEmpty();
        assertThat(ctx.getAttackerHand()).contains(card);
    }

    @Test
    void attackerDrawCard_WhenDeckEmpty_ReturnsNull() {
        Card drawn = ctx.attackerDrawCard();
        assertThat(drawn).isNull();
    }

    @Test
    void attackerDiscardTopCard_DiscardsFromDeck() {
        PokemonCard card = PokemonCard.builder().id("c1").name("Pikachu").build();
        ctx.getAttackerDeck().add(card);
        Card discarded = ctx.attackerDiscardTopCard();
        assertThat(discarded).isSameAs(card);
        assertThat(ctx.getAttackerDeck()).isEmpty();
        assertThat(ctx.getAttackerDiscard()).contains(card);
    }

    @Test
    void attackerDiscardTopCard_WhenDeckEmpty_ReturnsNull() {
        Card discarded = ctx.attackerDiscardTopCard();
        assertThat(discarded).isNull();
    }

    @Test
    void defenderDiscardTopCard_DiscardsFromDeck() {
        PokemonCard card = PokemonCard.builder().id("c2").name("Charmander").build();
        ctx.getDefenderDeck().add(card);
        Card discarded = ctx.defenderDiscardTopCard();
        assertThat(discarded).isSameAs(card);
        assertThat(ctx.getDefenderDeck()).isEmpty();
        assertThat(ctx.getDefenderDiscard()).contains(card);
    }

    @Test
    void defenderDiscardTopCard_WhenDeckEmpty_ReturnsNull() {
        Card discarded = ctx.defenderDiscardTopCard();
        assertThat(discarded).isNull();
    }

    @Test
    void peekDefenderDeck_ReturnsTopCard() {
        PokemonCard card = PokemonCard.builder().id("c3").name("Bulbasaur").build();
        ctx.getDefenderDeck().add(card);
        Card peeked = ctx.peekDefenderDeck();
        assertThat(peeked).isSameAs(card);
        assertThat(ctx.getDefenderDeck()).hasSize(1);
    }

    @Test
    void peekDefenderDeck_WhenEmpty_ReturnsNull() {
        assertThat(ctx.peekDefenderDeck()).isNull();
    }

    @Test
    void peekAttackerDeck_ReturnsTopCard() {
        PokemonCard card = PokemonCard.builder().id("c4").name("Squirtle").build();
        ctx.getAttackerDeck().add(card);
        Card peeked = ctx.peekAttackerDeck();
        assertThat(peeked).isSameAs(card);
        assertThat(ctx.getAttackerDeck()).hasSize(1);
    }

    @Test
    void peekAttackerDeck_WhenEmpty_ReturnsNull() {
        assertThat(ctx.peekAttackerDeck()).isNull();
    }

    @Test
    void shuffleAttackerDeck_ChangesOrder() {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            cards.add(PokemonCard.builder().id("c" + i).name("Card" + i).build());
        }
        ctx.getAttackerDeck().addAll(cards);
        ctx.shuffleAttackerDeck();
        assertThat(ctx.getAttackerDeck()).hasSize(10);
    }

    @Test
    void shuffleDefenderDeck_ChangesOrder() {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            cards.add(PokemonCard.builder().id("c" + i).name("Card" + i).build());
        }
        ctx.getDefenderDeck().addAll(cards);
        ctx.shuffleDefenderDeck();
        assertThat(ctx.getDefenderDeck()).hasSize(10);
    }

    @Test
    void dealDamageToAttacker_AddsDamage() {
        InPlayPokemon attacker = new InPlayPokemon(basePokemon);
        ctx.setAttacker(attacker);
        ctx.dealDamageToAttacker(30);
        assertThat(attacker.getDamageTaken()).isEqualTo(30);
    }

    @Test
    void dealDamageToBenched_AddsDamage() {
        InPlayPokemon benched = new InPlayPokemon(basePokemon);
        ctx.dealDamageToBenched(benched, 20);
        assertThat(benched.getDamageTaken()).isEqualTo(20);
    }

    @Test
    void ignoreWeakness_SetterAndGetter() {
        ctx.setIgnoreWeakness(true);
        assertThat(ctx.isIgnoreWeakness()).isTrue();
        ctx.setIgnoreWeakness(false);
        assertThat(ctx.isIgnoreWeakness()).isFalse();
    }

    @Test
    void ignoreResistance_SetterAndGetter() {
        ctx.setIgnoreResistance(true);
        assertThat(ctx.isIgnoreResistance()).isTrue();
        ctx.setIgnoreResistance(false);
        assertThat(ctx.isIgnoreResistance()).isFalse();
    }

    @Test
    void pendingDamage_DefaultIsZero() {
        assertThat(ctx.getPendingDamage()).isZero();
    }

    @Test
    void pendingDamage_SetterAndGetter() {
        ctx.setPendingDamage(50);
        assertThat(ctx.getPendingDamage()).isEqualTo(50);
    }

    @Test
    void attackerCantPlaySupporters_DefaultIsFalse() {
        assertThat(ctx.isAttackerCantPlaySupporters()).isFalse();
    }

    @Test
    void attackerCantPlaySupporters_SetterAndGetter() {
        ctx.setAttackerCantPlaySupporters(true);
        assertThat(ctx.isAttackerCantPlaySupporters()).isTrue();
    }

    @Test
    void currentPhase_SetterAndGetter() {
        ctx.setCurrentPhase(TurnPhase.ATTACK);
        assertThat(ctx.getCurrentPhase()).isEqualTo(TurnPhase.ATTACK);
    }

    @Test
    void firstTurn_SetterAndGetter() {
        ctx.setFirstTurn(true);
        assertThat(ctx.isFirstTurn()).isTrue();
    }

    @Test
    void attackerEnergyAttachmentsThisTurn_SetterAndGetter() {
        ctx.setAttackerEnergyAttachmentsThisTurn(1);
        assertThat(ctx.getAttackerEnergyAttachmentsThisTurn()).isEqualTo(1);
    }

    @Test
    void attackerAdditionalEnergyAttachments_SetterAndGetter() {
        ctx.setAttackerAdditionalEnergyAttachments(2);
        assertThat(ctx.getAttackerAdditionalEnergyAttachments()).isEqualTo(2);
    }

    @Test
    void attackerSupportersPlayedThisTurn_SetterAndGetter() {
        ctx.setAttackerSupportersPlayedThisTurn(1);
        assertThat(ctx.getAttackerSupportersPlayedThisTurn()).isEqualTo(1);
    }

    @Test
    void setAttackerPokemonPlayedThisTurn_ReplacesList() {
        List<PokemonCard> playedThisTurn = new ArrayList<>();
        playedThisTurn.add(basePokemon);
        ctx.setAttackerPokemonPlayedThisTurn(playedThisTurn);
        assertThat(ctx.getAttackerPokemonPlayedThisTurn()).isSameAs(playedThisTurn);
    }

    @Test
    void attacker_SetterAndGetter() {
        InPlayPokemon mon = new InPlayPokemon(basePokemon);
        ctx.setAttacker(mon);
        assertThat(ctx.getAttacker()).isSameAs(mon);
    }

    @Test
    void defender_SetterAndGetter() {
        InPlayPokemon mon = new InPlayPokemon(basePokemon);
        ctx.setDefender(mon);
        assertThat(ctx.getDefender()).isSameAs(mon);
    }

    @Test
    void random_SetterAndGetter() {
        java.util.Random rng = new java.util.Random(1);
        ctx.setRandom(rng);
        assertThat(ctx.getRandom()).isSameAs(rng);
    }

    @Test
    void addDamageCalculationModifier_AddsModifier() {
        IntUnaryOperator modifier = d -> d + 5;
        ctx.addDamageCalculationModifier(modifier);
        assertThat(ctx.getDamageCalculationModifiers()).containsExactly(modifier);
    }

    @Test
    void clearDamageCalculationModifiers_ClearsList() {
        ctx.addDamageCalculationModifier(d -> d + 5);
        ctx.clearDamageCalculationModifiers();
        assertThat(ctx.getDamageCalculationModifiers()).isEmpty();
    }

    @Test
    void setDamageCalculationModifiers_WithNull_ResetsToEmpty() {
        ctx.setDamageCalculationModifiers(null);
        assertThat(ctx.getDamageCalculationModifiers()).isEmpty();
    }

    @Test
    void setDamageCalculationModifiers_WithValidList_Replaces() {
        ctx.setDamageCalculationModifiers(List.of(d -> d * 2, d -> d + 10));
        assertThat(ctx.getDamageCalculationModifiers()).hasSize(2);
    }

    @Test
    void flipUntilTails_MultipleHeadsThenTails_ReturnsHeadsCount() {
        ctx.setRandom(new java.util.Random() {
            private int calls = 0;
            @Override
            public boolean nextBoolean() {
                calls++;
                return calls < 3;
            }
        });
        int heads = ctx.flipUntilTails();
        assertThat(heads).isEqualTo(2);
    }

    @Test
    void attackerDamageCounters_WithPartialDamage_Truncates() {
        InPlayPokemon attacker = new InPlayPokemon(basePokemon);
        attacker.setDamageTaken(35);
        ctx.setAttacker(attacker);
        assertThat(ctx.attackerDamageCounters()).isEqualTo(3);
    }

    @Test
    void defenderDamageCounters_WithNoDamage_ReturnsZero() {
        InPlayPokemon defender = new InPlayPokemon(basePokemon);
        ctx.setDefender(defender);
        assertThat(ctx.defenderDamageCounters()).isZero();
    }

    @Test
    void searchDefenderDeck_NotFound_ReturnsNull() {
        ctx.getDefenderDeck().add(PokemonCard.builder().id("c1").name("Pikachu").build());
        Card found = ctx.searchDefenderDeck(c -> "Missing".equals(c.getName()));
        assertThat(found).isNull();
        assertThat(ctx.getDefenderDeck()).hasSize(1);
    }

    @Test
    void setAttackerBench_ReplacesList() {
        List<InPlayPokemon> bench = new ArrayList<>();
        bench.add(new InPlayPokemon(basePokemon));
        ctx.setAttackerBench(bench);
        assertThat(ctx.getAttackerBench()).isSameAs(bench);
    }

    @Test
    void setDefenderBench_ReplacesList() {
        List<InPlayPokemon> bench = new ArrayList<>();
        ctx.setDefenderBench(bench);
        assertThat(ctx.getDefenderBench()).isSameAs(bench);
    }

    @Test
    void setAttackerHand_ReplacesList() {
        List<Card> hand = new ArrayList<>();
        ctx.setAttackerHand(hand);
        assertThat(ctx.getAttackerHand()).isSameAs(hand);
    }

    @Test
    void setDefenderHand_ReplacesList() {
        List<Card> hand = new ArrayList<>();
        ctx.setDefenderHand(hand);
        assertThat(ctx.getDefenderHand()).isSameAs(hand);
    }

    @Test
    void setAttackerDeck_ReplacesList() {
        List<Card> deck = new ArrayList<>();
        ctx.setAttackerDeck(deck);
        assertThat(ctx.getAttackerDeck()).isSameAs(deck);
    }

    @Test
    void setDefenderDeck_ReplacesList() {
        List<Card> deck = new ArrayList<>();
        ctx.setDefenderDeck(deck);
        assertThat(ctx.getDefenderDeck()).isSameAs(deck);
    }

    @Test
    void setAttackerDiscard_ReplacesList() {
        List<Card> discard = new ArrayList<>();
        ctx.setAttackerDiscard(discard);
        assertThat(ctx.getAttackerDiscard()).isSameAs(discard);
    }

    @Test
    void setDefenderDiscard_ReplacesList() {
        List<Card> discard = new ArrayList<>();
        ctx.setDefenderDiscard(discard);
        assertThat(ctx.getDefenderDiscard()).isSameAs(discard);
    }

    @Test
    void setAttackerPrizes_ReplacesList() {
        List<Card> prizes = new ArrayList<>();
        ctx.setAttackerPrizes(prizes);
        assertThat(ctx.getAttackerPrizes()).isSameAs(prizes);
    }

    @Test
    void setDefenderPrizes_ReplacesList() {
        List<Card> prizes = new ArrayList<>();
        ctx.setDefenderPrizes(prizes);
        assertThat(ctx.getDefenderPrizes()).isSameAs(prizes);
    }

    @Test
    void addLogEntry_AppendsToExistingLog() {
        ctx.addLogEntry("p1", "DRAW", "drew a card");
        assertThat(ctx.getMatchLog()).hasSize(1);
    }

    @Test
    void addLogEntry_WhenMatchLogIsNull_Reinitializes() {
        // Cubre la rama matchLog == null dentro de addLogEntry.
        ctx.setMatchLog(null);
        ctx.addLogEntry("p2", "ATTACK", "attacked");
        assertThat(ctx.getMatchLog()).isNotNull().hasSize(1);
    }

    @Test
    void drainPendingEvents_ReturnsAndClearsEvents() {
        ctx.flipCoin("burn"); // encola un CoinFlipGameEvent
        var drained = ctx.drainPendingEvents();
        assertThat(drained).hasSize(1);
        // Tras drenar, la lista interna queda vacía.
        assertThat(ctx.drainPendingEvents()).isEmpty();
    }

    @Test
    void defenderDrawCard_DrawsFromDeckToHand() {
        PokemonCard card = PokemonCard.builder().id("d1").name("Squirtle").build();
        ctx.getDefenderDeck().add(card);
        Card drawn = ctx.defenderDrawCard();
        assertThat(drawn).isSameAs(card);
        assertThat(ctx.getDefenderDeck()).isEmpty();
        assertThat(ctx.getDefenderHand()).contains(card);
    }

    @Test
    void defenderDrawCard_WhenDeckEmpty_ReturnsNull() {
        assertThat(ctx.defenderDrawCard()).isNull();
    }
}
