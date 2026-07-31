package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.engine.events.MatchGameEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Test-only no-op implementation of {@link MatchEventPublisher}.
 *
 * <p>This component exists so the GameEngine can depend on {@link MatchEventPublisher}
 * while unit/integration tests run without any Spring STOMP/WebSocket infrastructure.
 * When the 'test' profile is active Spring will instantiate this bean and publishing
 * becomes a no-op.
 *
 * <p>Design pattern: Observer — test stub / null object implementation of the observer.
 */
@Component
@Profile("test")
public class NoOpMatchEventPublisher implements MatchEventPublisher {

    @Override
    public void onMatchUpdated(MatchUpdatedEvent event) {
        // Intentionally no-op for tests.
    }

    @Override
    public void onMulligan(MulliganEvent event) {
        // Intentionally no-op for tests.
    }

    @Override
    public void onMatchGameEvent(String matchId, String p1Id, String p2Id, MatchGameEvent event) {
        // Intentionally no-op for tests.
    }
}
