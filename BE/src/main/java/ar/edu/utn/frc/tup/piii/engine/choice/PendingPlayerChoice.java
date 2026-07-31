package ar.edu.utn.frc.tup.piii.engine.choice;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import ar.edu.utn.frc.tup.piii.models.Card;

/**
 * A paused engine operation waiting for the player to pick or order cards.
 *
 * <p>Stored on {@link ar.edu.utn.frc.tup.piii.models.GameContext} while the
 * engine waits. The {@link #callback} is invoked with the picked/ordered
 * card identifiers when the {@code RESOLVE_PLAYER_CHOICE} action arrives.
 *
 * <p>For {@link ChoiceType#COIN_FLIP_INFO}, {@link #options} is empty and the
 * callback fires with an empty list — the FE just acknowledges the visual flip.
 * The actual coin result is encoded in {@link #coinFlipHeads}.
 */
public final class PendingPlayerChoice {

    private final String playerId;
    private final ChoiceType type;
    private final List<Card> options;
    private final int minPick;
    private final int maxPick;
    private final String prompt;
    private final Boolean coinFlipHeads;
    private final transient Consumer<List<String>> callback;

    public PendingPlayerChoice(String playerId,
                                ChoiceType type,
                                List<Card> options,
                                int minPick,
                                int maxPick,
                                String prompt,
                                Boolean coinFlipHeads,
                                Consumer<List<String>> callback) {
        this.playerId = Objects.requireNonNull(playerId, "playerId must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.options = options == null ? List.of() : List.copyOf(options);
        this.minPick = minPick;
        this.maxPick = maxPick;
        this.prompt = prompt;
        this.coinFlipHeads = coinFlipHeads;
        this.callback = Objects.requireNonNull(callback, "callback must not be null");
    }

    public String getPlayerId() { return playerId; }
    public ChoiceType getType() { return type; }
    public List<Card> getOptions() { return options; }
    public int getMinPick() { return minPick; }
    public int getMaxPick() { return maxPick; }
    public String getPrompt() { return prompt; }
    public Boolean getCoinFlipHeads() { return coinFlipHeads; }
    public Consumer<List<String>> getCallback() { return callback; }
}
