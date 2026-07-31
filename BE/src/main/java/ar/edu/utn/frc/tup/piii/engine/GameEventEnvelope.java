package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.engine.events.MatchGameEvent;

/**
 * STOMP transport wrapper for {@link MatchGameEvent}s.
 *
 * <p>Provides a {@code type} discriminator string so the Angular FE can route
 * the event to the correct handler without inspecting the payload shape.
 *
 * @param type    event type tag (e.g. {@code "COIN_FLIP"}, {@code "STATUS_APPLIED"})
 * @param payload the concrete event record
 */
public record GameEventEnvelope(String type, MatchGameEvent payload) {
}
