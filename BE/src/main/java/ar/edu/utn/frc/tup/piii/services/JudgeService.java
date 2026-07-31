package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.engine.MatchStateDTO;
import ar.edu.utn.frc.tup.piii.dtos.judge.JudgeQueryCategory;
import ar.edu.utn.frc.tup.piii.dtos.judge.JudgeQueryDTO;
import ar.edu.utn.frc.tup.piii.dtos.judge.JudgeResponseDTO;
import ar.edu.utn.frc.tup.piii.engine.GameEngine;
import ar.edu.utn.frc.tup.piii.judge.events.JudgeEvent;
import ar.edu.utn.frc.tup.piii.judge.resolvers.JudgeQueryResolver;
import ar.edu.utn.frc.tup.piii.judge.store.JudgeEventStore;
import ar.edu.utn.frc.tup.piii.models.Match;
import ar.edu.utn.frc.tup.piii.repositories.MatchRepository;
import jakarta.annotation.PostConstruct;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class JudgeService {

    private final MatchRepository matchRepository;
    private final GameEngine gameEngine;
    private final JudgeEventStore eventStore;
    private final List<JudgeQueryResolver> resolverList;

    private Map<JudgeQueryCategory, JudgeQueryResolver> resolverMap;

    public JudgeService(MatchRepository matchRepository,
                        GameEngine gameEngine,
                        JudgeEventStore eventStore,
                        List<JudgeQueryResolver> resolverList) {
        this.matchRepository = matchRepository;
        this.gameEngine = gameEngine;
        this.eventStore = eventStore;
        this.resolverList = resolverList;
    }

    @PostConstruct
    public void buildResolverMap() {
        resolverMap = new EnumMap<>(JudgeQueryCategory.class);
        for (JudgeQueryResolver resolver : resolverList) {
            resolverMap.put(resolver.supports(), resolver);
        }
        if (!resolverMap.containsKey(JudgeQueryCategory.CURRENT_RESTRICTIONS)) {
            JudgeQueryResolver feasibility = resolverMap.get(JudgeQueryCategory.WHY_CANT_I);
            if (feasibility != null) {
                resolverMap.put(JudgeQueryCategory.CURRENT_RESTRICTIONS, feasibility);
            }
        }
    }

    public JudgeResponseDTO resolve(JudgeQueryDTO query) {
        validateQuery(query);

        Match lobbyMatch = matchRepository.findById(query.getMatchId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Match not found: " + query.getMatchId()));

        if (!lobbyMatch.containsPlayer(query.getPlayerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Player does not belong to this match");
        }

        String engineMatchId = lobbyMatch.getEngineMatchId();
        if (engineMatchId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Match has not started yet — no engine state available");
        }

        MatchStateDTO matchState = gameEngine.getMatchState(engineMatchId, query.getPlayerId());
        List<JudgeEvent> log = eventStore.getLog(engineMatchId);

        JudgeQueryResolver resolver = resolverMap.get(query.getCategory());
        if (resolver == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported query category: " + query.getCategory());
        }

        return resolver.resolve(query, matchState, log);
    }

    private void validateQuery(JudgeQueryDTO query) {
        if (query == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Query must not be null");
        }
        if (query.getMatchId() == null || query.getMatchId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "matchId is required");
        }
        if (query.getPlayerId() == null || query.getPlayerId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "playerId is required");
        }
        if (query.getCategory() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "category is required");
        }
        if (query.getLang() == null) {
            query.setLang("es");
        }
    }

}
