package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.dtos.engine.MatchStateDTO.CardViewDTO;
import java.util.List;
import java.util.Objects;

public record MulliganEvent(
        String matchId,
        List<MulliganStep> steps
) {
    public MulliganEvent {
        Objects.requireNonNull(matchId, "matchId must not be null");
        Objects.requireNonNull(steps, "steps must not be null");
    }

    public record MulliganStep(
            String playerId,
            List<CardViewDTO> hand,
            int mulliganCount
    ) {
        public MulliganStep {
            Objects.requireNonNull(playerId, "playerId must not be null");
            Objects.requireNonNull(hand, "hand must not be null");
        }
    }
}
