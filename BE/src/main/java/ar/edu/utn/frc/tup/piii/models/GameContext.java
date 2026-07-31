package ar.edu.utn.frc.tup.piii.models;

import ar.edu.utn.frc.tup.piii.engine.choice.PendingPlayerChoice;
import ar.edu.utn.frc.tup.piii.engine.events.CoinFlipGameEvent;
import ar.edu.utn.frc.tup.piii.engine.events.MatchGameEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.IntUnaryOperator;
import ar.edu.utn.frc.tup.piii.engine.log.MatchLogEntry;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * Mutable snapshot of all battle state accessible to {@link AttackEffect} implementations.
 *
 * <p>The engine's Chain-of-Responsibility pipeline passes a single {@code GameContext}
 * through each step. Effects read from it to make decisions and write to it to record
 * outcomes (damage adjustments, flags, card movements). No Spring or HTTP types appear here.
 */
@Getter
@Setter
public class GameContext {

    // ---- Active combatants ----

    /** The Pokémon currently attacking. */
    private InPlayPokemon attacker;

    /** The Pokémon currently defending (opponent's active). */
    private InPlayPokemon defender;

    // ---- Benches ----

    /** Attacker's bench (up to 5 Pokémon). */
    private List<InPlayPokemon> attackerBench;

    /** Defender's bench (up to 5 Pokémon). */
    private List<InPlayPokemon> defenderBench;

    // ---- Hands ----

    private List<Card> attackerHand;
    private List<Card> defenderHand;

    // ---- Decks (top = index 0) ----

    private List<Card> attackerDeck;
    private List<Card> defenderDeck;

    // ---- Discard piles ----

    private List<Card> attackerDiscard;
    private List<Card> defenderDiscard;

    // ---- Prize cards ----

    private List<Card> attackerPrizes;
    private List<Card> defenderPrizes;

    // ---- Turn-scoped flags (reset each turn) ----

    /** Attacker cannot play Supporter cards this turn (Bother). */
    private boolean attackerCantPlaySupporters;

    /** Current turn phase used by pure validators to gate legal actions. */
    private TurnPhase currentPhase;

    /** Whether the current turn is the opening turn of the match. */
    private boolean firstTurn;

    /** Number of manual energy attachments already used by the attacker this turn. */
    private int attackerEnergyAttachmentsThisTurn;

    /** Additional manual energy attachments granted by card effects this turn. */
    private int attackerAdditionalEnergyAttachments;

    /** Number of Supporter cards already played by the attacker this turn. */
    private int attackerSupportersPlayedThisTurn;

    /**
     * Pokémon card instances that entered play under the attacker this turn.
     *
     * <p>The validator uses object identity here so tests and the engine can track the exact
     * in-play card instance that is not yet allowed to evolve.
     */
    private List<PokemonCard> attackerPokemonPlayedThisTurn;

    /** Instance IDs of Pokémon that have already evolved this turn (one per Pokémon). */
    private Set<String> attackerEvolvedThisTurn = new HashSet<>();

    /** Accumulated event log for the whole match (shared by both players). */
    private List<MatchLogEntry> matchLog = new ArrayList<>();

    /** 1-based turn counter, incremented at the start of each turn. */
    private int turnNumber;
    /** Instance IDs of Pokémon that retreated this turn; blocked from being promoted again same turn. */
    private Set<String> attackerRetreatedThisTurn = new HashSet<>();

    /** Keys of abilities already activated this turn, form {@code "instanceId:abilityName"}. */
    private Set<String> abilitiesUsedThisTurn = new HashSet<>();

    /**
     * Currently active player-choice prompt, or {@code null} if none. While set, the engine
     * rejects all other actions from the choosing player except {@code RESOLVE_PLAYER_CHOICE}.
     * Carries a transient callback consumed when the choice resolves.
     */
    private transient PendingPlayerChoice pendingPlayerChoice;

    /**
     * Player ID of the side currently acting as the attacker in this oriented context.
     * Populated by {@code TurnManager} before each action so trainer/ability effects can
     * raise player-choice prompts addressed to the correct client.
     */
    private transient String attackerPlayerId;

    // ---- Stadium zone (shared between both players) ----

    /** The Stadium card currently in play, or {@code null} if none. */
    private StadiumCard activeStadium;

    /** Player ID of whoever played the active stadium (for discard routing on replacement). */
    private String stadiumOwnerId;

    // ---- Last played trainer card (for opponent visibility) ----

    /** The most recently played trainer card, cleared after being read in getMatchState. */
    private transient Card lastPlayedCard;

    /** Player ID who played the most recent trainer card. */
    private transient String lastPlayedCardPlayerId;

    // ---- Attack resolution chain state ----

    /** Base or modified damage value flowing through the resolution chain. */
    private int pendingDamage;

    /**
     * When {@code true}, the current attack ignores the defender's Weakness multiplier
     * (set by IgnoreWeaknessAndResistanceEffect / IgnoreResistanceEffect).
     */
    private boolean ignoreWeakness;

    /**
     * When {@code true}, the current attack ignores the defender's Resistance reduction
     * (set by IgnoreResistanceEffect / IgnoreWeaknessAndResistanceEffect).
     */
    private boolean ignoreResistance;

    /**
     * Additional deterministic post-processing steps applied by the damage calculator
     * after Weakness and Resistance, in registration order.
     */
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private List<IntUnaryOperator> damageCalculationModifiers;

    /** Whether prize cards have been placed for both sides. */
    private boolean prizesDealt;

    // ---- Mulligan state ----

    /** How many mulligan iterations have occurred across both players. */
    private int mulliganIteration;

    /** Mulligan count for the attacker-side player. */
    private int attackerMulliganCount;

    /** Mulligan count for the defender-side player. */
    private int defenderMulliganCount;

    // ---- Randomness source (injectable for testing) ----

    private Random random;

    /**
     * Pokémon waiting to be placed on the bench after retreat, or {@code null}.
     * Used when the bench is full at retreat time — the Pokémon is stored here,
     * then placed in the slot vacated by {@link GameEngine#promoteFromBench}.
     */
    private InPlayPokemon pendingBenchPlacement;

    // ---- Pending game events (drained by GameEngine after each action) ----

    private final List<MatchGameEvent> pendingEvents = new ArrayList<>();

    public GameContext() {
        this.attackerBench = new ArrayList<>(Collections.nCopies(5, null));
        this.defenderBench = new ArrayList<>(Collections.nCopies(5, null));
        this.attackerHand = new ArrayList<>();
        this.defenderHand = new ArrayList<>();
        this.attackerDeck = new ArrayList<>();
        this.defenderDeck = new ArrayList<>();
        this.attackerDiscard = new ArrayList<>();
        this.defenderDiscard = new ArrayList<>();
        this.attackerPrizes = new ArrayList<>();
        this.defenderPrizes = new ArrayList<>();
        this.currentPhase = TurnPhase.MAIN;
        this.attackerPokemonPlayedThisTurn = new ArrayList<>();
        this.prizesDealt = false;
        this.mulliganIteration = 0;
        this.attackerMulliganCount = 0;
        this.defenderMulliganCount = 0;
        this.damageCalculationModifiers = new ArrayList<>();
        this.firstTurn = true;
        this.turnNumber = 1;
        this.random = new Random();
    }

    /**
     * Registers an additional damage modifier to be applied after Weakness and Resistance.
     *
     * @param modifier unary operator transforming the current damage into the next value
     */
    /**
     * Appends an entry to the shared match event log.
     *
     * @param playerId    acting player id
     * @param actionType  short action code
     * @param description human-readable description in Spanish
     */
    public void addLogEntry(String playerId, String actionType, String description) {
        if (matchLog == null) {
            matchLog = new ArrayList<>();
        }
        matchLog.add(new MatchLogEntry(
                turnNumber,
                playerId,
                actionType,
                description,
                System.currentTimeMillis()
        ));
    }

    public void addDamageCalculationModifier(IntUnaryOperator modifier) {
        damageCalculationModifiers.add(Objects.requireNonNull(modifier,
                "damageCalculationModifier must not be null"));
    }

    /** Removes all registered post-processing damage modifiers. */
    public void clearDamageCalculationModifiers() {
        damageCalculationModifiers.clear();
    }

    /**
     * Returns the registered post-processing damage modifiers in application order.
     *
     * @return immutable view of the current damage modifiers
     */
    public List<IntUnaryOperator> getDamageCalculationModifiers() {
        return Collections.unmodifiableList(damageCalculationModifiers);
    }

    /**
     * Replaces the registered post-processing damage modifiers.
     *
     * @param damageCalculationModifiers modifiers to apply after Weakness/Resistance;
     *                                  {@code null} resets the list to empty
     */
    public void setDamageCalculationModifiers(List<IntUnaryOperator> damageCalculationModifiers) {
        if (damageCalculationModifiers == null) {
            this.damageCalculationModifiers = new ArrayList<>();
            return;
        }

        List<IntUnaryOperator> validatedModifiers = new ArrayList<>(damageCalculationModifiers.size());
        for (IntUnaryOperator modifier : damageCalculationModifiers) {
            validatedModifiers.add(Objects.requireNonNull(modifier,
                    "damageCalculationModifier must not be null"));
        }
        this.damageCalculationModifiers = validatedModifiers;
    }

    // ---- Utility: coin flips ----

    /** Flips a single coin. Returns {@code true} for heads. */
    public boolean flipCoin() {
        return random.nextBoolean();
    }

    /**
     * Flips {@code n} coins and returns the number of heads. Each flip emits a
     * {@link CoinFlipGameEvent} so the FE animates them one by one.
     */
    public int flipCoins(int n) {
        int heads = 0;
        for (int i = 0; i < n; i++) {
            if (flipCoin("attack")) {
                heads++;
            }
        }
        return heads;
    }

    /**
     * Flips coins until tails. Returns the number of heads obtained before the first tails.
     * Each flip emits a {@link CoinFlipGameEvent}.
     */
    public int flipUntilTails() {
        int heads = 0;
        while (flipCoin("attack")) {
            heads++;
        }
        return heads;
    }

    /**
     * Flips a coin, records a {@link CoinFlipGameEvent}, and returns the result.
     *
     * @param reason label describing why the flip occurred (e.g. "burn", "sleep")
     * @return {@code true} for heads
     */
    public boolean flipCoin(String reason) {
        boolean heads = flipCoin();
        pendingEvents.add(new CoinFlipGameEvent(heads, reason));
        return heads;
    }

    /**
     * Appends an arbitrary game event to the pending list.
     *
     * @param event event to record; must not be {@code null}
     */
    public void recordEvent(MatchGameEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        pendingEvents.add(event);
    }

    /**
     * Removes and returns all accumulated events since the last drain.
     *
     * @return snapshot of events; the internal list is cleared
     */
    public List<MatchGameEvent> drainPendingEvents() {
        List<MatchGameEvent> snapshot = new ArrayList<>(pendingEvents);
        pendingEvents.clear();
        return snapshot;
    }

    // ---- Utility: damage counter helpers ----

    /** Returns the number of damage counters on the attacker (10 HP lost = 1 counter). */
    public int attackerDamageCounters() {
        return attacker.getDamageTaken() / 10;
    }

    /** Returns the number of damage counters on the defender. */
    public int defenderDamageCounters() {
        return defender.getDamageTaken() / 10;
    }

    // ---- Utility: card movement helpers ----

    /**
     * Searches the attacker's deck for the first card matching the given predicate,
     * removes it from the deck, and returns it. Returns {@code null} if not found.
     */
    public Card searchAttackerDeck(java.util.function.Predicate<Card> predicate) {
        for (int i = 0; i < attackerDeck.size(); i++) {
            if (predicate.test(attackerDeck.get(i))) {
                return attackerDeck.remove(i);
            }
        }
        return null;
    }

    /**
     * Searches the defender's deck for the first card matching the given predicate,
     * removes it from the deck, and returns it. Returns {@code null} if not found.
     */
    public Card searchDefenderDeck(java.util.function.Predicate<Card> predicate) {
        for (int i = 0; i < defenderDeck.size(); i++) {
            if (predicate.test(defenderDeck.get(i))) {
                return defenderDeck.remove(i);
            }
        }
        return null;
    }

    /** Draws the top card of the attacker's deck into their hand. Returns the drawn card. */
    public Card attackerDrawCard() {
        if (attackerDeck.isEmpty()) {
            return null;
        }
        Card drawn = attackerDeck.remove(0);
        attackerHand.add(drawn);
        return drawn;
    }

    /** Discards the top card of the attacker's deck. Returns the discarded card. */
    public Card attackerDiscardTopCard() {
        if (attackerDeck.isEmpty()) {
            return null;
        }
        Card top = attackerDeck.remove(0);
        attackerDiscard.add(top);
        return top;
    }

    /** Draws the top card of the defender's deck into their hand. Returns the drawn card. */
    public Card defenderDrawCard() {
        if (defenderDeck.isEmpty()) {
            return null;
        }
        Card drawn = defenderDeck.remove(0);
        defenderHand.add(drawn);
        return drawn;
    }

    /** Discards the top card of the defender's deck. Returns the discarded card. */
    public Card defenderDiscardTopCard() {
        if (defenderDeck.isEmpty()) {
            return null;
        }
        Card top = defenderDeck.remove(0);
        defenderDiscard.add(top);
        return top;
    }

    /** Returns the top card of the defender's deck without removing it. */
    public Card peekDefenderDeck() {
        return defenderDeck.isEmpty() ? null : defenderDeck.get(0);
    }

    /** Returns the top card of the attacker's deck without removing it. */
    public Card peekAttackerDeck() {
        return attackerDeck.isEmpty() ? null : attackerDeck.get(0);
    }

    /** Shuffles the attacker's deck in place. */
    public void shuffleAttackerDeck() {
        java.util.Collections.shuffle(attackerDeck, random);
    }

    /** Shuffles the defender's deck in place. */
    public void shuffleDefenderDeck() {
        java.util.Collections.shuffle(defenderDeck, random);
    }

    /** Applies {@code damage} directly to the attacker (recoil / self-damage). */
    public void dealDamageToAttacker(int damage) {
        attacker.setDamageTaken(attacker.getDamageTaken() + damage);
    }

    /** Applies {@code damage} to a specific benched Pokémon (no W/R). */
    public void dealDamageToBenched(InPlayPokemon benched, int damage) {
        benched.setDamageTaken(benched.getDamageTaken() + damage);
    }
}
