package ar.edu.utn.frc.tup.piii.configs;

import ar.edu.utn.frc.tup.piii.engine.GameEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically enforces the 40-second setup countdown: any player who has not
 * confirmed their active/bench setup when their deadline expires is auto-completed
 * by the engine (first Basic Pokémon as active, no bench, then ready).
 *
 * <p>Because the deadline lives in the engine, a page reload (F5) cannot reset it
 * nor dodge the auto-assignment.
 */
@Component
public class SetupTimeoutScheduler {

    private static final Logger log = LoggerFactory.getLogger(SetupTimeoutScheduler.class);

    private final GameEngine gameEngine;

    public SetupTimeoutScheduler(GameEngine gameEngine) {
        this.gameEngine = gameEngine;
    }

    @Scheduled(fixedRate = 1000)
    public void enforceSetupTimeouts() {
        try {
            gameEngine.autoCompleteExpiredSetups();
        } catch (Exception e) {
            log.warn("Setup timeout scan failed: {}", e.getMessage());
        }
    }
}