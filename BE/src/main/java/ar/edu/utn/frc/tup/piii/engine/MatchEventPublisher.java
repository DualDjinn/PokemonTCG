package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.engine.events.MatchGameEvent;

/**
 * Observer contract for publishing match update events.
 *
 * <p>GameEngine will publish {@link MatchUpdatedEvent} instances through this
 * interface. Transport-layer implementations (e.g. STOMP/WebSocket publishers)
 * implement this interface to deliver per-player views to connected clients.
 *
 * <p>Design pattern: Observer — this is the observer interface. GameEngine is the
 * subject and depends only on this interface (no transport-specific imports).
 */
public interface MatchEventPublisher {

    /**
     * Deliver a match-updated event to interested observers (transport).
     *
     * @param event the match updated event containing per-player views
     */
    void onMatchUpdated(MatchUpdatedEvent event);

    /**
     * Deliver a mulligan notification event to connected clients.
     *
     * @param event the mulligan event containing revealed hand steps
     */
    void onMulligan(MulliganEvent event);

    /**
     * Deliver a granular in-game event to both players in a match.
     *
     * @param matchId  entity match identifier used for topic routing
     * @param p1Id     first player identifier
     * @param p2Id     second player identifier
     * @param event    the game event to deliver
     */
    void onMatchGameEvent(String matchId, String p1Id, String p2Id, MatchGameEvent event);
}
