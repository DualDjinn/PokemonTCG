package ar.edu.utn.frc.tup.piii.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import ar.edu.utn.frc.tup.piii.dtos.engine.MatchStateDTO;
import ar.edu.utn.frc.tup.piii.dtos.judge.JudgeQueryCategory;
import ar.edu.utn.frc.tup.piii.dtos.judge.JudgeQueryDTO;
import ar.edu.utn.frc.tup.piii.dtos.judge.JudgeResponseDTO;
import ar.edu.utn.frc.tup.piii.engine.GameEngine;
import ar.edu.utn.frc.tup.piii.judge.events.JudgeEvent;
import ar.edu.utn.frc.tup.piii.judge.resolvers.JudgeQueryResolver;
import ar.edu.utn.frc.tup.piii.judge.store.JudgeEventStore;
import ar.edu.utn.frc.tup.piii.models.Match;
import ar.edu.utn.frc.tup.piii.models.MatchStatus;
import ar.edu.utn.frc.tup.piii.repositories.MatchRepository;

class JudgeServiceTest {

    private MatchRepository matchRepository;
    private GameEngine gameEngine;
    private JudgeEventStore eventStore;
    private JudgeQueryResolver whyCantIResolver;
    private JudgeService service;

    private Match match;

    @BeforeEach
    void setUp() {
        matchRepository = mock(MatchRepository.class);
        gameEngine = mock(GameEngine.class);
        eventStore = mock(JudgeEventStore.class);

        whyCantIResolver = mock(JudgeQueryResolver.class);
        when(whyCantIResolver.supports()).thenReturn(JudgeQueryCategory.WHY_CANT_I);

        service = new JudgeService(matchRepository, gameEngine, eventStore, List.of(whyCantIResolver));
        service.buildResolverMap();

        match = new Match("m1", "p1", "deck1", "ABC123", null, null, false, true, null, false);
        match.setPlayerTwoId("p2");
        match.setEngineMatchId("engine-1");
        match.setStatus(MatchStatus.ACTIVE);
    }

    private JudgeQueryDTO validQuery() {
        JudgeQueryDTO query = new JudgeQueryDTO();
        query.setMatchId("m1");
        query.setPlayerId("p1");
        query.setCategory(JudgeQueryCategory.WHY_CANT_I);
        query.setLang("es");
        return query;
    }

    // ───────── resolve — happy path ─────────

    @Test
    void resolve_ValidQuery_DelegatesToResolver() {
        JudgeQueryDTO query = validQuery();
        MatchStateDTO state = new MatchStateDTO();
        JudgeResponseDTO expected = JudgeResponseDTO.builder()
                .category(JudgeQueryCategory.WHY_CANT_I)
                .summary("Cannot attack").build();

        when(matchRepository.findById("m1")).thenReturn(Optional.of(match));
        when(gameEngine.getMatchState("engine-1", "p1")).thenReturn(state);
        when(eventStore.getLog("engine-1")).thenReturn(List.of());
        when(whyCantIResolver.resolve(eq(query), eq(state), any())).thenReturn(expected);

        JudgeResponseDTO result = service.resolve(query);

        assertThat(result.getSummary()).isEqualTo("Cannot attack");
        assertThat(result.getCategory()).isEqualTo(JudgeQueryCategory.WHY_CANT_I);
    }

    // ───────── resolve — CURRENT_RESTRICTIONS fallback ─────────

    @Test
    void resolve_CurrentRestrictions_FallsBackToWhyCantI() {
        JudgeQueryDTO query = validQuery();
        query.setCategory(JudgeQueryCategory.CURRENT_RESTRICTIONS);

        MatchStateDTO state = new MatchStateDTO();
        JudgeResponseDTO expected = JudgeResponseDTO.builder()
                .category(JudgeQueryCategory.CURRENT_RESTRICTIONS)
                .summary("Restrictions").build();

        when(matchRepository.findById("m1")).thenReturn(Optional.of(match));
        when(gameEngine.getMatchState("engine-1", "p1")).thenReturn(state);
        when(eventStore.getLog("engine-1")).thenReturn(List.of());
        when(whyCantIResolver.resolve(eq(query), eq(state), any())).thenReturn(expected);

        JudgeResponseDTO result = service.resolve(query);
        assertThat(result.getSummary()).isEqualTo("Restrictions");
    }

    // ───────── validate — null query ─────────

    @Test
    void resolve_NullQuery_Throws400() {
        assertThatThrownBy(() -> service.resolve(null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Query must not be null");
    }

    @Test
    void resolve_NullMatchId_Throws400() {
        JudgeQueryDTO query = validQuery();
        query.setMatchId(null);

        assertThatThrownBy(() -> service.resolve(query))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("matchId is required");
    }

    @Test
    void resolve_BlankMatchId_Throws400() {
        JudgeQueryDTO query = validQuery();
        query.setMatchId("   ");

        assertThatThrownBy(() -> service.resolve(query))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("matchId is required");
    }

    @Test
    void resolve_NullPlayerId_Throws400() {
        JudgeQueryDTO query = validQuery();
        query.setPlayerId(null);

        assertThatThrownBy(() -> service.resolve(query))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("playerId is required");
    }

    @Test
    void resolve_BlankPlayerId_Throws400() {
        JudgeQueryDTO query = validQuery();
        query.setPlayerId("");

        assertThatThrownBy(() -> service.resolve(query))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("playerId is required");
    }

    @Test
    void resolve_NullCategory_Throws400() {
        JudgeQueryDTO query = validQuery();
        query.setCategory(null);

        assertThatThrownBy(() -> service.resolve(query))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("category is required");
    }

    // ───────── resolve — lang default ─────────

    @Test
    void resolve_NullLang_DefaultsToEs() {
        JudgeQueryDTO query = validQuery();
        query.setLang(null);

        MatchStateDTO state = new MatchStateDTO();
        JudgeResponseDTO expected = JudgeResponseDTO.builder().summary("ok").build();

        when(matchRepository.findById("m1")).thenReturn(Optional.of(match));
        when(gameEngine.getMatchState("engine-1", "p1")).thenReturn(state);
        when(eventStore.getLog("engine-1")).thenReturn(List.of());
        when(whyCantIResolver.resolve(any(), any(), any())).thenReturn(expected);

        service.resolve(query);

        assertThat(query.getLang()).isEqualTo("es");
    }

    // ───────── resolve — match not found ─────────

    @Test
    void resolve_MatchNotFound_Throws404() {
        JudgeQueryDTO query = validQuery();
        when(matchRepository.findById("m1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolve(query))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Match not found");
    }

    // ───────── resolve — player not in match ─────────

    @Test
    void resolve_PlayerNotInMatch_Throws403() {
        JudgeQueryDTO query = validQuery();
        query.setPlayerId("stranger");
        when(matchRepository.findById("m1")).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> service.resolve(query))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Player does not belong");
    }

    // ───────── resolve — match not started ─────────

    @Test
    void resolve_MatchNotStarted_Throws404() {
        JudgeQueryDTO query = validQuery();
        match.setEngineMatchId(null);
        when(matchRepository.findById("m1")).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> service.resolve(query))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Match has not started");
    }

    // ───────── resolve — unsupported category ─────────

    @Test
    void resolve_UnsupportedCategory_Throws400() {
        JudgeQueryDTO query = validQuery();
        query.setCategory(JudgeQueryCategory.EXPLAIN_DAMAGE);

        when(matchRepository.findById("m1")).thenReturn(Optional.of(match));
        when(gameEngine.getMatchState("engine-1", "p1")).thenReturn(new MatchStateDTO());
        when(eventStore.getLog("engine-1")).thenReturn(List.of());

        assertThatThrownBy(() -> service.resolve(query))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Unsupported query category");
    }

    // ───────── buildResolverMap — fallback ─────────

    @Test
    void buildResolverMap_WithoutCurrentRestrictions_FallsBackToWhyCantI() {
        JudgeService freshService = new JudgeService(
                matchRepository, gameEngine, eventStore, List.of(whyCantIResolver));
        freshService.buildResolverMap();

        JudgeQueryDTO query = validQuery();
        query.setCategory(JudgeQueryCategory.CURRENT_RESTRICTIONS);

        MatchStateDTO state = new MatchStateDTO();
        JudgeResponseDTO expected = JudgeResponseDTO.builder().summary("fallback").build();

        when(matchRepository.findById("m1")).thenReturn(Optional.of(match));
        when(gameEngine.getMatchState("engine-1", "p1")).thenReturn(state);
        when(eventStore.getLog("engine-1")).thenReturn(List.of());
        when(whyCantIResolver.resolve(any(), any(), any())).thenReturn(expected);

        JudgeResponseDTO result = freshService.resolve(query);
        assertThat(result.getSummary()).isEqualTo("fallback");
    }
}
