package ar.edu.utn.frc.tup.piii.services;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.CardRarity;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.PackType;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.PokemonStage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link PackRandomizer}.
 *
 * <p>Usa {@link StubRandom} para inyectar valores deterministas y validar el
 * comportamiento del algoritmo de sorteo sin depender de aleatoriedad real.
 */
class PackRandomizerTest {

    // ---------------------------------------------------------------- helpers

    private PokemonCard card(String id, CardRarity rarity) {
        PokemonCard p = PokemonCard.builder()
                .id(id)
                .name(id)
                .hp(60)
                .stage(PokemonStage.BASIC)
                .isEx(false)
                .isMega(false)
                .pokemonType(EnergyType.GRASS)
                .attacks(List.of())
                .weaknesses(List.of())
                .resistances(List.of())
                .retreatCost(1)
                .evolvesFrom(null)
                .evolvesTo(List.of())
                .rules(List.of())
                .build();
        p.setRarity(rarity);
        return p;
    }

    private List<Card> standardPool() {
        return List.of(
                card("h1", CardRarity.COMMON),
                card("h2", CardRarity.COMMON),
                card("m1", CardRarity.UNCOMMON),
                card("m2", CardRarity.UNCOMMON),
                card("l1", CardRarity.RARE),
                card("l2", CardRarity.RARE_HOLO));
    }

    // ---------------------------------------------------------------- tests

    @Test
    void drawCardsReturnsExactlyThreeCards() {
        PackRandomizer randomizer = new PackRandomizer(new Random(42));
        List<Card> result = randomizer.drawCards(PackType.BRONZE, standardPool());
        assertThat(result).hasSize(PackType.CARDS_PER_PACK);
    }

    @Test
    void drawCardsAllowsRepeatedResultsAcrossSlots() {
        // Pool con UNA sola carta en HIGH y BRONZE prioriza HIGH → al sortear 3, todas
        // deben ser esa única carta.
        List<Card> tinyPool = List.of(card("only", CardRarity.COMMON));
        PackRandomizer randomizer = new PackRandomizer(new Random(0));
        List<Card> result = randomizer.drawCards(PackType.BRONZE, tinyPool);
        assertThat(result).hasSize(3);
        assertThat(result).allMatch(c -> "only".equals(c.getId()));
    }

    @Test
    void drawCardsRespectsHighBucketWhenRollIsLow() {
        // BRONZE: HIGH=0.80, MEDIUM=0.17, LOW=0.03
        // roll < 0.80 debe caer en HIGH. Forzamos roll=0.10 para los 3 slots.
        StubRandom rng = new StubRandom()
                .withDoubles(0.10, 0.10, 0.10)
                .withInts(0, 0, 0);
        PackRandomizer randomizer = new PackRandomizer(rng);
        List<Card> result = randomizer.drawCards(PackType.BRONZE, standardPool());
        assertThat(result).allMatch(c -> c.getRarity() == CardRarity.COMMON);
    }

    @Test
    void drawCardsRespectsLowBucketWhenRollIsHigh() {
        // GOLD: HIGH=0.45, MEDIUM=0.35 (cum 0.80), LOW=0.20 (cum 1.0)
        // roll=0.90 cae en LOW.
        StubRandom rng = new StubRandom()
                .withDoubles(0.90, 0.90, 0.90)
                .withInts(0, 0, 0);
        PackRandomizer randomizer = new PackRandomizer(rng);
        List<Card> result = randomizer.drawCards(PackType.GOLD, standardPool());
        assertThat(result).allMatch(c ->
                c.getRarity() == CardRarity.RARE || c.getRarity() == CardRarity.RARE_HOLO);
    }

    @Test
    void drawCardsFallsBackToAvailableBucketWhenSortedBucketEmpty() {
        // Pool solo con cartas LOW; cualquier roll que apunte a HIGH debe caer en LOW.
        List<Card> onlyLow = List.of(card("l1", CardRarity.RARE), card("l2", CardRarity.RARE_HOLO));
        StubRandom rng = new StubRandom()
                .withDoubles(0.10, 0.10, 0.10)  // apunta a HIGH (que está vacío)
                .withInts(0, 0, 0);
        PackRandomizer randomizer = new PackRandomizer(rng);
        List<Card> result = randomizer.drawCards(PackType.BRONZE, onlyLow);
        assertThat(result).hasSize(3);
        assertThat(result).allMatch(c ->
                c.getRarity() == CardRarity.RARE || c.getRarity() == CardRarity.RARE_HOLO);
    }

    @Test
    void drawCardsThrowsOnEmptyPool() {
        PackRandomizer randomizer = new PackRandomizer(new Random(0));
        assertThatThrownBy(() -> randomizer.drawCards(PackType.SILVER, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void drawCardsThrowsOnNullPool() {
        PackRandomizer randomizer = new PackRandomizer(new Random(0));
        assertThatThrownBy(() -> randomizer.drawCards(PackType.SILVER, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void defaultConstructorUsesNonDeterministicRandom() {
        // Solo verifica que no explote y que devuelva 3 cartas.
        PackRandomizer randomizer = new PackRandomizer();
        List<Card> result = randomizer.drawCards(PackType.SILVER, standardPool());
        assertThat(result).hasSize(3);
    }

    @Test
    void silverPackUsesItsOwnDistribution() {
        // SILVER: HIGH=0.60, MEDIUM=0.30 (cum 0.90), LOW=0.10
        // roll=0.70 cae en MEDIUM.
        StubRandom rng = new StubRandom()
                .withDoubles(0.70, 0.70, 0.70)
                .withInts(0, 0, 0);
        PackRandomizer randomizer = new PackRandomizer(rng);
        List<Card> result = randomizer.drawCards(PackType.SILVER, standardPool());
        assertThat(result).allMatch(c -> c.getRarity() == CardRarity.UNCOMMON);
    }

    // ============================== EDGE CASES ==============================

    @Test
    void drawCardsHandlesCardsWithNullRarity() {
        // Carta sin rareza: RarityBucket.fromRarity(null) → HIGH, así que cae en pool HIGH.
        PokemonCard nullRarityCard = PokemonCard.builder()
                .id("nullR").name("NoRarity").hp(60)
                .stage(PokemonStage.BASIC).isEx(false).isMega(false)
                .pokemonType(EnergyType.GRASS)
                .attacks(List.of()).weaknesses(List.of()).resistances(List.of())
                .retreatCost(1).evolvesFrom(null).evolvesTo(List.of()).rules(List.of())
                .build();
        // No seteamos rarity, queda null
        List<Card> pool = List.of(nullRarityCard);

        PackRandomizer randomizer = new PackRandomizer(new Random(0));
        List<Card> result = randomizer.drawCards(PackType.BRONZE, pool);

        assertThat(result).hasSize(3);
        assertThat(result).allMatch(c -> "nullR".equals(c.getId()));
    }

    @Test
    void drawCardsRollAtZeroBoundaryFallsInFirstBucket() {
        // roll=0.0 debe caer estrictamente en el primer bucket con probabilidad > 0.
        // Bronce: HIGH=0.80 (cum 0.80) → cualquier roll en [0, 0.8) cae en HIGH.
        StubRandom rng = new StubRandom().withDoubles(0.0, 0.0, 0.0).withInts(0, 0, 0);
        PackRandomizer randomizer = new PackRandomizer(rng);
        List<Card> result = randomizer.drawCards(PackType.BRONZE, standardPool());
        assertThat(result).allMatch(c -> c.getRarity() == CardRarity.COMMON);
    }

    @Test
    void drawCardsRollNearOneBoundaryFallsInLastBucket() {
        // roll=0.999 está en el límite superior; debe caer en LOW (último bucket).
        StubRandom rng = new StubRandom().withDoubles(0.999, 0.999, 0.999).withInts(0, 0, 0);
        PackRandomizer randomizer = new PackRandomizer(rng);
        List<Card> result = randomizer.drawCards(PackType.GOLD, standardPool());
        assertThat(result).allMatch(c ->
                c.getRarity() == CardRarity.RARE || c.getRarity() == CardRarity.RARE_HOLO);
    }

    @Test
    void drawCardsRollExactlyAtCumulativeBoundary() {
        // Bronce: HIGH=0.80, MEDIUM=0.17 (cum 0.97), LOW=0.03 (cum 1.0)
        // roll=0.80 es el borde superior de HIGH. Como la condición es roll < cumulative,
        // roll=0.80 NO entra en HIGH (cum=0.80 no satisface 0.80<0.80), pasa a MEDIUM.
        StubRandom rng = new StubRandom().withDoubles(0.80, 0.80, 0.80).withInts(0, 0, 0);
        PackRandomizer randomizer = new PackRandomizer(rng);
        List<Card> result = randomizer.drawCards(PackType.BRONZE, standardPool());
        assertThat(result).allMatch(c -> c.getRarity() == CardRarity.UNCOMMON);
    }

    @Test
    void drawCardsAllCardsAreFromInputPool() {
        // Invariante: el randomizer nunca devuelve cartas que no estén en el pool.
        List<Card> pool = standardPool();
        Set<String> poolIds = pool.stream().map(Card::getId).collect(Collectors.toSet());

        PackRandomizer randomizer = new PackRandomizer(new Random(42));
        for (int i = 0; i < 20; i++) {
            List<Card> result = randomizer.drawCards(PackType.SILVER, pool);
            for (Card c : result) {
                assertThat(poolIds).contains(c.getId());
            }
        }
    }

    @Test
    void drawCardsReturnsListInOrderOfDraw() {
        // Sortea slot 0, luego slot 1, luego slot 2. Los rolls se aplican secuencialmente.
        // Configuramos rolls distintos para cada slot para verificar orden.
        StubRandom rng = new StubRandom()
                .withDoubles(0.10, 0.70, 0.90)  // HIGH, MEDIUM, LOW (en GOLD)
                .withInts(0, 0, 0);
        PackRandomizer randomizer = new PackRandomizer(rng);
        List<Card> result = randomizer.drawCards(PackType.GOLD, standardPool());

        // GOLD: HIGH=0.45, MEDIUM=0.35 (cum 0.80), LOW=0.20
        assertThat(result.get(0).getRarity()).isEqualTo(CardRarity.COMMON);   // roll 0.10 → HIGH
        assertThat(result.get(1).getRarity()).isEqualTo(CardRarity.UNCOMMON); // roll 0.70 → MEDIUM (< 0.80)
        // roll 0.90 cae en LOW (Rare o Rare Holo)
        assertThat(result.get(2).getRarity())
                .isIn(CardRarity.RARE, CardRarity.RARE_HOLO);
    }

    @Test
    void drawCardsAllowsPoolOfSingleBucket() {
        // Pool con SOLO cartas comunes — todos los sorteos caen en HIGH (fallback).
        List<Card> onlyCommon = List.of(
                card("c1", CardRarity.COMMON),
                card("c2", CardRarity.COMMON));
        PackRandomizer randomizer = new PackRandomizer(new Random(7));
        List<Card> result = randomizer.drawCards(PackType.GOLD, onlyCommon);

        assertThat(result).hasSize(3);
        assertThat(result).allMatch(c -> c.getRarity() == CardRarity.COMMON);
    }

    // ---------------------------------------------------------------- stub RNG

    /**
     * {@link Random} estable que devuelve valores predefinidos en orden.
     * Cuando se agotan, vuelve al comportamiento default del {@link Random} padre.
     */
    private static class StubRandom extends Random {
        private final Deque<Double> doubles = new ArrayDeque<>();
        private final Deque<Integer> ints = new ArrayDeque<>();

        StubRandom() {
            super(0L);
        }

        StubRandom withDoubles(double... values) {
            for (double v : values) {
                doubles.add(v);
            }
            return this;
        }

        StubRandom withInts(int... values) {
            for (int v : values) {
                ints.add(v);
            }
            return this;
        }

        @Override
        public double nextDouble() {
            return doubles.isEmpty() ? super.nextDouble() : doubles.poll();
        }

        @Override
        public int nextInt(int bound) {
            return ints.isEmpty() ? super.nextInt(bound) : ints.poll();
        }
    }
}
