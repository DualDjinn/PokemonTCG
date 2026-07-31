package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.models.Player;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Repository-style service that stores in-memory match aggregates.
 *
 * <p>The engine facade orchestrates match creation and updates, while this class
 * keeps the lookup concern isolated so multiplayer storage can later be replaced
 * without changing controller-facing APIs.
 */
@Service
public class BattleRegistry {

    private final Map<String, Battle> matches = new ConcurrentHashMap<>();

    /**
     * Creates a new empty match aggregate.
     *
     * @param playerOne first player
     * @param playerTwo second player
     * @return new in-memory match aggregate
     */
    public Battle create(Player playerOne, Player playerTwo) {
        return new Battle(UUID.randomUUID().toString(), playerOne, playerTwo);
    }

    /**
     * Retrieves one match or fails with a consistent domain message.
     *
     * @param matchId match identifier
     * @return stored match aggregate
     * @throws IllegalArgumentException if the identifier is blank or the match does not exist
     */
    public Battle getBattleOrThrow(String matchId) {
        if (matchId == null || matchId.isBlank()) {
            throw new IllegalArgumentException("Battle id must not be blank");
        }

        Battle match = matches.get(matchId);
        if (match == null) {
            throw new IllegalArgumentException("Battle not found: " + matchId);
        }
        return match;
    }

    /**
     * Stores the current version of a match aggregate.
     *
     * @param match match to persist
     */
    public void save(Battle match) {
        Objects.requireNonNull(match, "match must not be null");
        matches.put(match.getId(), match);
    }

    /**
     * Removes a match aggregate from the in-memory store.
     *
     * @param matchId match identifier to remove
     */
    public void remove(String matchId) {
        if (matchId != null) {
            matches.remove(matchId);
        }
    }

    public List<Battle> getAll() {
        return new ArrayList<>(matches.values());
    }
}
