package ar.edu.utn.frc.tup.piii.engine.bot;

/**
 * Difficulty levels for AI bots that can play against human users.
 *
 * <ul>
 *   <li>{@link #EASY} — picks a random legal action.</li>
 *   <li>{@link #MEDIUM} — greedy: picks the action with the highest immediate heuristic score.</li>
 *   <li>{@link #HARD} — minimax with alpha-beta pruning at limited depth.</li>
 * </ul>
 */
public enum BotDifficulty {
    EASY,
    MEDIUM,
    HARD;

    /**
     * Parses a difficulty name (case-insensitive). Defaults to {@link #EASY} for {@code null}
     * or unknown values so the match can still start.
     *
     * @param value raw difficulty string (e.g. {@code "easy"}, {@code "Hard"})
     * @return matching difficulty or {@link #EASY}
     */
    public static BotDifficulty fromString(String value) {
        if (value == null) {
            return EASY;
        }
        try {
            return BotDifficulty.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return EASY;
        }
    }

    /**
     * Returns the canonical bot player id associated to this difficulty.
     * The {@code DataSeeder} pre-creates one bot player per difficulty.
     *
     * @return canonical id like {@code "bot-easy"}
     */
    public String botPlayerId() {
        return "bot-" + name().toLowerCase();
    }
}
