package ar.edu.utn.frc.tup.piii.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import ar.edu.utn.frc.tup.piii.dtos.engine.CreateMatchResponse;
import ar.edu.utn.frc.tup.piii.dtos.engine.GameAction;
import ar.edu.utn.frc.tup.piii.dtos.engine.MatchStateDTO;
import ar.edu.utn.frc.tup.piii.engine.GameEngine;
import ar.edu.utn.frc.tup.piii.engine.snapshot.GameSnapshot;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.Deck;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.Match;
import ar.edu.utn.frc.tup.piii.models.MatchStatus;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.PokemonStage;
import ar.edu.utn.frc.tup.piii.repositories.MatchRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerRepository;

/**
 * Unit tests for {@link MatchService}, mocking the repositories and the
 * {@link GameEngine} facade so the lobby lifecycle logic is exercised in isolation.
 */
class MatchServiceTest {

    private static final int VALID_DECK_SIZE = 60;
    private static final int COPIES_PER_CARD = 4;

    private MatchRepository matchRepository;
    private PlayerRepository playerRepository;
    private GameEngine gameEngine;
    private SimpMessagingTemplate messagingTemplate;
    private CardBackService cardBackService;
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private CardRegistryService cardRegistryService;
    private MatchService matchService;

    @BeforeEach
    void setUp() {
        matchRepository = mock(MatchRepository.class);
        playerRepository = mock(PlayerRepository.class);
        gameEngine = mock(GameEngine.class);
        messagingTemplate = mock(SimpMessagingTemplate.class);
        cardBackService = mock(CardBackService.class);
        objectMapper = mock(com.fasterxml.jackson.databind.ObjectMapper.class);
        cardRegistryService = mock(CardRegistryService.class);
        matchService = new MatchService(matchRepository, playerRepository, gameEngine, messagingTemplate,
                cardBackService, objectMapper, cardRegistryService);

        when(matchRepository.save(any(Match.class))).thenAnswer(inv -> inv.getArgument(0));
        when(matchRepository.findByJoinCode(any())).thenReturn(Optional.empty());

        // Several lobby methods register an after-commit synchronization; activate it so
        // registerSynchronization() works in these plain unit tests (no Spring transaction).
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    /** Fires all registered after-commit hooks, simulating a successful transaction commit. */
    private void fireAfterCommit() {
        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
            sync.afterCommit();
        }
    }

    // ───────────────────────── createMatch ─────────────────────────

    @Test
    void createMatch_Success_CreatesWaitingMatchWithJoinCode() {
        Player host = playerWithDeck("host-1", "deck-1", validDeck("deck-1"));
        when(playerRepository.findByIdWithDecks("host-1")).thenReturn(Optional.of(host));

        Match match = matchService.createMatch("host-1", "deck-1");

        assertThat(match.getId()).isNotBlank();
        assertThat(match.getPlayerOneId()).isEqualTo("host-1");
        assertThat(match.getDeckOneId()).isEqualTo("deck-1");
        assertThat(match.getStatus()).isEqualTo(MatchStatus.WAITING);
        assertThat(match.getJoinCode()).isNotBlank();
        verify(matchRepository).save(match);
    }

    @Test
    void createMatch_NullDeckId_SkipsOwnershipCheckAndCreates() {
        Player host = playerWithDeck("host-1", "deck-1", validDeck("deck-1"));
        when(playerRepository.findByIdWithDecks("host-1")).thenReturn(Optional.of(host));

        assertThatThrownBy(() -> matchService.createMatch("host-1", null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("deck is required");
    }

    @Test
    void createMatch_BlankDeckId_SkipsOwnershipCheckAndCreates() {
        Player host = playerWithDeck("host-1", "deck-1", validDeck("deck-1"));
        when(playerRepository.findByIdWithDecks("host-1")).thenReturn(Optional.of(host));

        assertThatThrownBy(() -> matchService.createMatch("host-1", "   "))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("deck is required");
    }

    @Test
    void createMatch_HostNotFound_Throws404() {
        when(playerRepository.findByIdWithDecks("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.createMatch("ghost", "deck-1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Player not found");
        verify(matchRepository, never()).save(any());
    }

    @Test
    void createMatch_DeckNotOwned_Throws404() {
        Player host = playerWithDeck("host-1", "deck-1", validDeck("deck-1"));
        when(playerRepository.findByIdWithDecks("host-1")).thenReturn(Optional.of(host));

        assertThatThrownBy(() -> matchService.createMatch("host-1", "deck-unknown"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Deck not found");
    }

    @Test
    void createMatch_NullDecksList_Throws404WhenDeckRequested() {
        Player host = new Player();
        host.setId("host-1");
        host.setDecks(null);
        when(playerRepository.findByIdWithDecks("host-1")).thenReturn(Optional.of(host));

        assertThatThrownBy(() -> matchService.createMatch("host-1", "deck-1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Deck not found");
    }

    // ───────────────────────── joinMatch ─────────────────────────

    @Test
    void joinMatch_Success_AddsPlayerTwoAndKeepsWaiting() {
        Match lobby = new Match("match-1", "host-1", "deck-host", "ABC123");
        when(matchRepository.findById("match-1")).thenReturn(Optional.of(lobby));

        Player host = playerWithDeck("host-1", "deck-host", validDeck("deck-host"));
        Player joiner = playerWithDeck("joiner-1", "deck-join", validDeck("deck-join"));
        when(playerRepository.findById("joiner-1")).thenReturn(Optional.of(joiner));
        when(playerRepository.findById("host-1")).thenReturn(Optional.of(host));

        Match result = matchService.joinMatch("match-1", "joiner-1", "deck-join");

        assertThat(result.getStatus()).isEqualTo(MatchStatus.WAITING);
        assertThat(result.getPlayerTwoId()).isEqualTo("joiner-1");
        assertThat(result.getDeckTwoId()).isEqualTo("deck-join");
        assertThat(result.getEngineMatchId()).isNull();
        verify(gameEngine, never()).createMatch(any(), any(), any(), any());
        verify(matchRepository).save(result);
    }

    @Test
    void joinMatch_MatchNotFound_Throws404() {
        when(matchRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.joinMatch("missing", "joiner-1", "deck-join"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Match not found");
    }

    @Test
    void joinMatch_NotWaiting_Throws409() {
        Match lobby = new Match("match-1", "host-1", "deck-host", "ABC123");
        lobby.setStatus(MatchStatus.ACTIVE);
        when(matchRepository.findById("match-1")).thenReturn(Optional.of(lobby));

        assertThatThrownBy(() -> matchService.joinMatch("match-1", "joiner-1", "deck-join"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not open to join");
        verify(gameEngine, never()).createMatch(any(), any(), any(), any());
    }

    @Test
    void joinMatch_HostJoinsOwnMatch_Throws409() {
        Match lobby = new Match("match-1", "host-1", "deck-host", "ABC123");
        when(matchRepository.findById("match-1")).thenReturn(Optional.of(lobby));

        assertThatThrownBy(() -> matchService.joinMatch("match-1", "host-1", "deck-host"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Host cannot join");
        verify(gameEngine, never()).createMatch(any(), any(), any(), any());
    }

    @Test
    void joinMatch_JoinerNotFound_Throws404() {
        Match lobby = new Match("match-1", "host-1", "deck-host", "ABC123");
        when(matchRepository.findById("match-1")).thenReturn(Optional.of(lobby));
        when(playerRepository.findById("joiner-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.joinMatch("match-1", "joiner-1", "deck-join"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Player not found");
    }

    @Test
    void joinMatch_DeckNotOwned_Throws404() {
        Match lobby = new Match("match-1", "host-1", "deck-host", "ABC123");
        when(matchRepository.findById("match-1")).thenReturn(Optional.of(lobby));

        Player host = playerWithDeck("host-1", "deck-host", validDeck("deck-host"));
        Player joiner = playerWithDeck("joiner-1", "deck-join", validDeck("deck-join"));
        when(playerRepository.findById("joiner-1")).thenReturn(Optional.of(joiner));
        when(playerRepository.findById("host-1")).thenReturn(Optional.of(host));

        assertThatThrownBy(() -> matchService.joinMatch("match-1", "joiner-1", "deck-not-mine"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Deck not found");
        verify(gameEngine, never()).createMatch(any(), any(), any(), any());
    }

    @Test
    void joinMatch_InvalidDeck_Throws400AndDoesNotStartEngine() {
        Match lobby = new Match("match-1", "host-1", "deck-host", "ABC123");
        when(matchRepository.findById("match-1")).thenReturn(Optional.of(lobby));

        Player host = playerWithDeck("host-1", "deck-host", validDeck("deck-host"));
        Deck shortDeck = new Deck("deck-join", "Too Small", new ArrayList<>(List.of(basic("c1", 0))));
        Player joiner = playerWithDeck("joiner-1", "deck-join", shortDeck);
        when(playerRepository.findById("joiner-1")).thenReturn(Optional.of(joiner));
        when(playerRepository.findById("host-1")).thenReturn(Optional.of(host));

        assertThatThrownBy(() -> matchService.joinMatch("match-1", "joiner-1", "deck-join"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not valid");
        verify(gameEngine, never()).createMatch(any(), any(), any(), any());
    }

    // ───────────────────────── getMatchState ─────────────────────────

    @Test
    void getMatchState_Success_DelegatesAndKeepsSetupWhenStillWaiting() {
        Match lobby = startedLobby(MatchStatus.SETUP, "engine-99");
        when(matchRepository.findById("match-1")).thenReturn(Optional.of(lobby));
        MatchStateDTO engineState = state(false, false, null);
        when(gameEngine.getMatchState("engine-99", "host-1")).thenReturn(engineState);

        MatchStateDTO result = matchService.getMatchState("match-1", "host-1");

        assertThat(result).isSameAs(engineState);
        assertThat(lobby.getStatus()).isEqualTo(MatchStatus.SETUP);
        verify(matchRepository, never()).save(any());
    }

    @Test
    void getMatchState_WhenEngineReportsPlaying_SyncsToActive() {
        Match lobby = startedLobby(MatchStatus.SETUP, "engine-99");
        when(matchRepository.findById("match-1")).thenReturn(Optional.of(lobby));
        when(gameEngine.getMatchState("engine-99", "host-1"))
                .thenReturn(state(false, true, null));

        matchService.getMatchState("match-1", "host-1");

        assertThat(lobby.getStatus()).isEqualTo(MatchStatus.ACTIVE);
        verify(matchRepository).save(lobby);
    }

    @Test
    void getMatchState_WhenEngineReportsFinished_SyncsWinnerAndStatus() {
        Match lobby = startedLobby(MatchStatus.ACTIVE, "engine-99");
        when(matchRepository.findById("match-1")).thenReturn(Optional.of(lobby));
        when(gameEngine.getMatchState("engine-99", "host-1"))
                .thenReturn(state(true, false, "host-1"));

        matchService.getMatchState("match-1", "host-1");

        assertThat(lobby.getStatus()).isEqualTo(MatchStatus.FINISHED);
        assertThat(lobby.getWinnerId()).isEqualTo("host-1");
        verify(matchRepository).save(lobby);
    }

    @Test
    void getMatchState_BlankMatchId_Throws404() {
        assertThatThrownBy(() -> matchService.getMatchState("  ", "host-1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Match not found");
    }

    @Test
    void getMatchState_NullMatchId_Throws404() {
        assertThatThrownBy(() -> matchService.getMatchState(null, "host-1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Match not found");
    }

    @Test
    void getMatchState_AlreadyFinished_EngineFinished_SavesWithoutStatusChange() {
        Match lobby = startedLobby(MatchStatus.FINISHED, "engine-99");
        when(matchRepository.findById("match-1")).thenReturn(Optional.of(lobby));
        when(gameEngine.getMatchState("engine-99", "host-1"))
                .thenReturn(state(true, false, "host-1"));

        matchService.getMatchState("match-1", "host-1");

        assertThat(lobby.getStatus()).isEqualTo(MatchStatus.FINISHED);
        assertThat(lobby.getWinnerId()).isEqualTo("host-1");
        verify(matchRepository).save(lobby);
    }

    @Test
    void getMatchState_NotMember_Throws403() {
        Match lobby = startedLobby(MatchStatus.ACTIVE, "engine-99");
        when(matchRepository.findById("match-1")).thenReturn(Optional.of(lobby));

        assertThatThrownBy(() -> matchService.getMatchState("match-1", "stranger"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void getMatchState_NotStarted_Throws404() {
        Match lobby = new Match("match-1", "host-1", "deck-host", "ABC123");
        lobby.setPlayerTwoId("joiner-1");
        when(matchRepository.findById("match-1")).thenReturn(Optional.of(lobby));

        assertThatThrownBy(() -> matchService.getMatchState("match-1", "host-1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("has not started");
    }

    // ───────────────────────── submitAction ─────────────────────────

    @Test
    void submitAction_Success_DelegatesToEngineAndSyncsStatus() {
        Match lobby = startedLobby(MatchStatus.SETUP, "engine-99");
        when(matchRepository.findById("match-1")).thenReturn(Optional.of(lobby));
        when(gameEngine.getMatchState("engine-99", "host-1"))
                .thenReturn(state(false, true, null));
        GameAction action = GameAction.endTurn();

        matchService.submitAction("match-1", "host-1", action);

        verify(gameEngine).submitAction(eq("engine-99"), eq("host-1"), eq(action));
        assertThat(lobby.getStatus()).isEqualTo(MatchStatus.ACTIVE);
    }

    @Test
    void submitAction_NotMember_Throws403() {
        Match lobby = startedLobby(MatchStatus.ACTIVE, "engine-99");
        when(matchRepository.findById("match-1")).thenReturn(Optional.of(lobby));

        assertThatThrownBy(() -> matchService.submitAction("match-1", "stranger", GameAction.endTurn()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("does not belong");
        verify(gameEngine, never()).submitAction(any(), any(), any());
    }

    @Test
    void submitAction_NotStarted_Throws404() {
        Match lobby = new Match("match-1", "host-1", "deck-host", "ABC123");
        lobby.setPlayerTwoId("joiner-1");
        when(matchRepository.findById("match-1")).thenReturn(Optional.of(lobby));

        assertThatThrownBy(() -> matchService.submitAction("match-1", "host-1", GameAction.endTurn()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("has not started");
        verify(gameEngine, never()).submitAction(any(), any(), any());
    }

    // ───────────────────────── joinByCode ─────────────────────────

    @Test
    void joinByCode_Success() {
        Match lobby = new Match("m-1", "host-1", "deck-host", "ABC123");
        when(matchRepository.findByJoinCode("ABC123")).thenReturn(Optional.of(lobby));
        Player joiner = playerWithDeck("joiner-1", "deck-join", validDeck("deck-join"));
        when(playerRepository.findById("joiner-1")).thenReturn(Optional.of(joiner));

        CreateMatchResponse response = matchService.joinByCode("abc123", "joiner-1", "deck-join");

        assertThat(response.getJoinCode()).isEqualTo("ABC123");
        assertThat(lobby.getPlayerTwoId()).isEqualTo("joiner-1");
        verify(matchRepository).save(lobby);
    }

    @Test
    void joinByCode_NotFound_Throws404() {
        when(matchRepository.findByJoinCode("MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.joinByCode("MISSING", "joiner-1", "deck-join"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Room not found");
    }

    @Test
    void joinByCode_NotWaiting_Throws409() {
        Match lobby = new Match("m-1", "host-1", "deck-host", "ABC123");
        lobby.setStatus(MatchStatus.ACTIVE);
        when(matchRepository.findByJoinCode("ABC123")).thenReturn(Optional.of(lobby));

        assertThatThrownBy(() -> matchService.joinByCode("ABC123", "joiner-1", "deck-join"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not open");
    }

    @Test
    void joinByCode_HostJoinsOwn_Throws409() {
        Match lobby = new Match("m-1", "host-1", "deck-host", "ABC123");
        when(matchRepository.findByJoinCode("ABC123")).thenReturn(Optional.of(lobby));

        assertThatThrownBy(() -> matchService.joinByCode("ABC123", "host-1", "deck-join"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Host cannot join");
    }

    @Test
    void joinByCode_RoomFull_Throws409() {
        Match lobby = new Match("m-1", "host-1", "deck-host", "ABC123");
        lobby.setPlayerTwoId("someone");
        when(matchRepository.findByJoinCode("ABC123")).thenReturn(Optional.of(lobby));

        assertThatThrownBy(() -> matchService.joinByCode("ABC123", "joiner-1", "deck-join"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already full");
    }

    // ───────────────────────── setReady ─────────────────────────

    @Test
    void setReady_HostReady() {
        Match lobby = startedLobby(MatchStatus.WAITING, null);
        lobby.setHostReady(false);
        lobby.setGuestReady(false);
        when(matchRepository.findById("match-1")).thenReturn(Optional.of(lobby));

        CreateMatchResponse response = matchService.setReady("match-1", "host-1", true);

        assertThat(response.isHostReady()).isTrue();
        verify(matchRepository).save(lobby);
    }

    @Test
    void setReady_GuestReady() {
        Match lobby = startedLobby(MatchStatus.WAITING, null);
        lobby.setHostReady(false);
        lobby.setGuestReady(false);
        when(matchRepository.findById("match-1")).thenReturn(Optional.of(lobby));

        CreateMatchResponse response = matchService.setReady("match-1", "joiner-1", true);

        assertThat(response.isGuestReady()).isTrue();
        verify(matchRepository).save(lobby);
    }

    @Test
    void setReady_NotMember_Throws403() {
        Match lobby = startedLobby(MatchStatus.WAITING, null);
        when(matchRepository.findById("match-1")).thenReturn(Optional.of(lobby));

        assertThatThrownBy(() -> matchService.setReady("match-1", "stranger", true))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("does not belong");
    }

    // ───────────────────────── startMatch ─────────────────────────

    @Test
    void startMatch_Success() {
        Match lobby = new Match("m-1", "host-1", "deck-host", "ABC123");
        lobby.setPlayerTwoId("joiner-1");
        lobby.setDeckTwoId("deck-join");
        lobby.setHostReady(true);
        lobby.setGuestReady(true);
        when(matchRepository.findById("m-1")).thenReturn(Optional.of(lobby));

        Player host = playerWithDeck("host-1", "deck-host", validDeck("deck-host"));
        Player guest = playerWithDeck("joiner-1", "deck-join", validDeck("deck-join"));
        when(playerRepository.findByIdWithDecks("host-1")).thenReturn(Optional.of(host));
        when(playerRepository.findByIdWithDecks("joiner-1")).thenReturn(Optional.of(guest));
        when(gameEngine.createMatch(host, guest, host.getDecks().get(0), guest.getDecks().get(0)))
                .thenReturn("engine-99");

        CreateMatchResponse response = matchService.startMatch("m-1", "host-1");

        assertThat(response.getStatus()).isEqualTo(MatchStatus.SETUP);
        verify(gameEngine).createMatch(host, guest, host.getDecks().get(0), guest.getDecks().get(0));
        verify(matchRepository).save(lobby);
    }

    @Test
    void startMatch_NotHost_Throws403() {
        Match lobby = new Match("m-1", "host-1", "deck-host", "ABC123");
        when(matchRepository.findById("m-1")).thenReturn(Optional.of(lobby));

        assertThatThrownBy(() -> matchService.startMatch("m-1", "joiner-1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only the host");
    }

    @Test
    void startMatch_NoOpponent_Throws409() {
        Match lobby = new Match("m-1", "host-1", "deck-host", "ABC123");
        lobby.setPlayerTwoId(null);
        when(matchRepository.findById("m-1")).thenReturn(Optional.of(lobby));

        assertThatThrownBy(() -> matchService.startMatch("m-1", "host-1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("without an opponent");
    }

    @Test
    void startMatch_NotBothReady_Throws409() {
        Match lobby = new Match("m-1", "host-1", "deck-host", "ABC123");
        lobby.setPlayerTwoId("joiner-1");
        lobby.setHostReady(true);
        lobby.setGuestReady(false);
        when(matchRepository.findById("m-1")).thenReturn(Optional.of(lobby));

        assertThatThrownBy(() -> matchService.startMatch("m-1", "host-1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("must be ready");
    }

    // ───────────────────────── cancelMatch ─────────────────────────

    @Test
    void cancelMatch_HostCancels_DeletesMatch() {
        Match lobby = new Match("m-1", "host-1", "deck-host", "ABC123");
        when(matchRepository.findById("m-1")).thenReturn(Optional.of(lobby));

        matchService.cancelMatch("m-1", "host-1");

        verify(matchRepository).delete(lobby);
    }

    @Test
    void cancelMatch_GuestLeaves_ClearsSlot() {
        Match lobby = new Match("m-1", "host-1", "deck-host", "ABC123");
        lobby.setPlayerTwoId("joiner-1");
        lobby.setDeckTwoId("deck-join");
        lobby.setGuestReady(true);
        when(matchRepository.findById("m-1")).thenReturn(Optional.of(lobby));

        matchService.cancelMatch("m-1", "joiner-1");

        assertThat(lobby.getPlayerTwoId()).isNull();
        assertThat(lobby.getDeckTwoId()).isNull();
        assertThat(lobby.isGuestReady()).isFalse();
        verify(matchRepository).save(lobby);
    }

    @Test
    void cancelMatch_NotMember_Throws403() {
        Match lobby = new Match("m-1", "host-1", "deck-host", "ABC123");
        when(matchRepository.findById("m-1")).thenReturn(Optional.of(lobby));

        assertThatThrownBy(() -> matchService.cancelMatch("m-1", "stranger"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("does not belong");
    }

    // ───────────────────────── listLobby ─────────────────────────

    @Test
    void listLobby_ReturnsVisibleWaitingMatches() {
        Match visible = new Match("m-1", "host-1", "deck-host", "ABC123");
        visible.setShowInList(true);
        Match hidden = new Match("m-2", "host-2", "deck-host2", "XYZ789");
        hidden.setShowInList(false);
        when(matchRepository.findByStatusInOrderByCreatedAtDesc(any())).thenReturn(List.of(visible, hidden));

        List<CreateMatchResponse> result = matchService.listLobby();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getJoinCode()).isEqualTo("ABC123");
    }

    @Test
    void listLobby_Empty_ReturnsEmptyList() {
        when(matchRepository.findByStatusInOrderByCreatedAtDesc(any())).thenReturn(List.of());

        List<CreateMatchResponse> result = matchService.listLobby();

        assertThat(result).isEmpty();
    }

    // ───────────────────────── engine delegations ─────────────────────────

    private Match startedAndStubbed(MatchStatus status, String engineId) {
        Match lobby = startedLobby(status, engineId);
        when(matchRepository.findById("match-1")).thenReturn(Optional.of(lobby));
        return lobby;
    }

    private void stubEngineState(String engineId) {
        when(gameEngine.getMatchState(engineId, "host-1")).thenReturn(state(false, false, null));
    }

    @Test
    void requestDeal_delegatesToEngineAndSyncs() {
        startedAndStubbed(MatchStatus.SETUP, "engine-99");
        stubEngineState("engine-99");
        matchService.requestDeal("match-1", "host-1");
        verify(gameEngine).dealCards("engine-99");
    }

    @Test
    void selectActive_delegatesToEngine() {
        startedAndStubbed(MatchStatus.SETUP, "engine-99");
        matchService.selectActive("match-1", "host-1", "card-1");
        verify(gameEngine).selectActivePokemon("engine-99", "host-1", "card-1");
    }

    @Test
    void placeBench_delegatesToEngine() {
        startedAndStubbed(MatchStatus.SETUP, "engine-99");
        matchService.placeBench("match-1", "host-1", "card-1");
        verify(gameEngine).placeBenchPokemon("engine-99", "host-1", "card-1");
    }

    @Test
    void confirmSetup_delegatesAndSyncs() {
        startedAndStubbed(MatchStatus.SETUP, "engine-99");
        stubEngineState("engine-99");
        matchService.confirmSetup("match-1", "host-1");
        verify(gameEngine).confirmSetupReady("engine-99", "host-1");
    }

    @Test
    void processSetup_withBench_placesEachAndConfirms() {
        startedAndStubbed(MatchStatus.SETUP, "engine-99");
        stubEngineState("engine-99");
        matchService.processSetup("match-1", "host-1", "active-1", List.of("b1", "b2"));
        verify(gameEngine).selectActivePokemon("engine-99", "host-1", "active-1");
        verify(gameEngine).placeBenchPokemon("engine-99", "host-1", "b1");
        verify(gameEngine).placeBenchPokemon("engine-99", "host-1", "b2");
        verify(gameEngine).confirmSetupReady("engine-99", "host-1");
    }

    @Test
    void processSetup_nullBench_skipsBenchPlacement() {
        startedAndStubbed(MatchStatus.SETUP, "engine-99");
        stubEngineState("engine-99");
        matchService.processSetup("match-1", "host-1", "active-1", null);
        verify(gameEngine).selectActivePokemon("engine-99", "host-1", "active-1");
        verify(gameEngine, never()).placeBenchPokemon(any(), any(), any());
        verify(gameEngine).confirmSetupReady("engine-99", "host-1");
    }

    @Test
    void resolveMulliganDraw_delegatesAndSyncs() {
        startedAndStubbed(MatchStatus.SETUP, "engine-99");
        stubEngineState("engine-99");
        matchService.resolveMulliganDraw("match-1", "host-1", 2);
        verify(gameEngine).resolveMulliganDraw("engine-99", "host-1", 2);
    }

    @Test
    void chooseCoinSide_delegatesAndReturnsState() {
        startedAndStubbed(MatchStatus.SETUP, "engine-99");
        MatchStateDTO st = state(false, false, null);
        when(gameEngine.getMatchState("engine-99", "host-1")).thenReturn(st);
        MatchStateDTO result = matchService.chooseCoinSide("match-1", "host-1", "heads");
        assertThat(result).isSameAs(st);
        verify(gameEngine).chooseCoinSide("engine-99", "host-1", "heads");
    }

    @Test
    void selectPrize_delegatesAndSyncs() {
        startedAndStubbed(MatchStatus.ACTIVE, "engine-99");
        stubEngineState("engine-99");
        matchService.selectPrize("match-1", "host-1", 0);
        verify(gameEngine).selectPrize("engine-99", "host-1", 0);
    }

    @Test
    void promoteFromBench_delegatesAndSyncs() {
        startedAndStubbed(MatchStatus.ACTIVE, "engine-99");
        stubEngineState("engine-99");
        matchService.promoteFromBench("match-1", "host-1", 1);
        verify(gameEngine).promoteFromBench("engine-99", "host-1", 1);
    }

    @Test
    void chooseFirstTurn_delegatesAndReturnsState() {
        startedAndStubbed(MatchStatus.SETUP, "engine-99");
        MatchStateDTO st = state(false, false, null);
        when(gameEngine.getMatchState("engine-99", "host-1")).thenReturn(st);
        MatchStateDTO result = matchService.chooseFirstTurn("match-1", "host-1", "host-1");
        assertThat(result).isSameAs(st);
        verify(gameEngine).chooseFirstTurn("engine-99", "host-1", "host-1");
    }

    @Test
    void requestDeal_notMember_throws403() {
        startedAndStubbed(MatchStatus.SETUP, "engine-99");
        assertThatThrownBy(() -> matchService.requestDeal("match-1", "stranger"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("does not belong");
    }

    // ───────────────────────── surrender ─────────────────────────

    @Test
    void surrender_finishesMatchAndNotifiesOpponent() {
        Match lobby = startedAndStubbed(MatchStatus.ACTIVE, "engine-99");
        matchService.surrender("match-1", "host-1");

        assertThat(lobby.getStatus()).isEqualTo(MatchStatus.FINISHED);
        assertThat(lobby.getWinnerId()).isEqualTo("joiner-1");
        verify(gameEngine).removeMatch("engine-99");
        verify(messagingTemplate, times(3)).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void surrender_guestSurrenders_hostWins() {
        Match lobby = startedAndStubbed(MatchStatus.ACTIVE, "engine-99");
        matchService.surrender("match-1", "joiner-1");
        assertThat(lobby.getWinnerId()).isEqualTo("host-1");
    }

    // ───────────────────────── reconnectPlayer ─────────────────────────

    @Test
    void reconnectPlayer_engineExists_returnsEngineState() {
        startedAndStubbed(MatchStatus.ACTIVE, "engine-99");
        when(gameEngine.getMatchContext("engine-99")).thenReturn(mock(GameContext.class));
        MatchStateDTO st = state(false, false, null);
        when(gameEngine.getMatchState("engine-99", "host-1")).thenReturn(st);

        MatchStateDTO result = matchService.reconnectPlayer("match-1", "host-1");

        assertThat(result).isSameAs(st);
    }

    @Test
    void reconnectPlayer_clearsDisconnectFlagForReconnectingPlayer() {
        Match lobby = startedAndStubbed(MatchStatus.ACTIVE, "engine-99");
        lobby.setDisconnectedPlayerId("host-1");
        when(gameEngine.getMatchContext("engine-99")).thenReturn(mock(GameContext.class));
        when(gameEngine.getMatchState("engine-99", "host-1")).thenReturn(state(false, false, null));

        matchService.reconnectPlayer("match-1", "host-1");

        assertThat(lobby.getDisconnectedPlayerId()).isNull();
        verify(matchRepository).save(lobby);
    }

    @Test
    void reconnectPlayer_noEngineNoSnapshot_throws500() {
        Match lobby = startedAndStubbed(MatchStatus.ACTIVE, null);
        lobby.setStateJson(null);

        assertThatThrownBy(() -> matchService.reconnectPlayer("match-1", "host-1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Could not restore");
    }

    // ───────────────────────── getPendingMatches ─────────────────────────

    @Test
    void getPendingMatches_returnsDedupedMatches() {
        Match m1 = new Match("m-1", "p", "deck", "AAA111");
        Match m2 = new Match("m-2", "other", "deck", "BBB222");
        m2.setPlayerTwoId("p");
        when(matchRepository.findByPlayerOneIdAndStatusIn(eq("p"), any())).thenReturn(List.of(m1));
        when(matchRepository.findByPlayerTwoIdAndStatusIn(eq("p"), any())).thenReturn(List.of(m1, m2));

        List<CreateMatchResponse> result = matchService.getPendingMatches("p");

        assertThat(result).hasSize(2);
    }

    // ───────────────────────── countdown ─────────────────────────

    @Test
    void startCountdown_host_publishesEvent() {
        startedAndStubbed(MatchStatus.SETUP, "engine-99");
        matchService.startCountdown("match-1", "host-1", "SETUP", 30);
        verify(messagingTemplate).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void startCountdown_notHost_throws403() {
        startedAndStubbed(MatchStatus.SETUP, "engine-99");
        assertThatThrownBy(() -> matchService.startCountdown("match-1", "joiner-1", "SETUP", 30))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only the host");
    }

    @Test
    void cancelCountdown_host_publishesEvent() {
        startedAndStubbed(MatchStatus.SETUP, "engine-99");
        matchService.cancelCountdown("match-1", "host-1");
        verify(messagingTemplate).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void cancelCountdown_notHost_throws403() {
        startedAndStubbed(MatchStatus.SETUP, "engine-99");
        assertThatThrownBy(() -> matchService.cancelCountdown("match-1", "joiner-1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only the host");
    }

    // ───────────────────────── hover notifications ─────────────────────────

    @Test
    void notifyPokemonHover_member_sendsToOpponent() {
        startedAndStubbed(MatchStatus.ACTIVE, "engine-99");
        matchService.notifyPokemonHover("match-1", "host-1");
        verify(messagingTemplate).convertAndSend(eq("/topic/matches/match-1/state/joiner-1"), any(Object.class));
    }

    @Test
    void notifyPokemonUnhover_member_sendsToOpponent() {
        startedAndStubbed(MatchStatus.ACTIVE, "engine-99");
        matchService.notifyPokemonUnhover("match-1", "host-1");
        verify(messagingTemplate).convertAndSend(eq("/topic/matches/match-1/state/joiner-1"), any(Object.class));
    }

    // ───────────────────────── getLobbyState ─────────────────────────

    @Test
    void getLobbyState_host_updatesActivityAndSaves() {
        Match lobby = startedAndStubbed(MatchStatus.WAITING, null);

        CreateMatchResponse result = matchService.getLobbyState("match-1", "host-1");

        assertThat(result.getMatchId()).isEqualTo("match-1");
        assertThat(lobby.getLastActivityAt()).isNotNull();
        verify(matchRepository).save(lobby);
    }

    @Test
    void getLobbyState_guest_doesNotSave() {
        startedAndStubbed(MatchStatus.WAITING, null);
        matchService.getLobbyState("match-1", "joiner-1");
        verify(matchRepository, never()).save(any());
    }

    // ───────────────────────── cancelMatch (active) ─────────────────────────

    @Test
    void cancelMatch_activeMatch_finishesAndNotifiesBothPlayers() {
        Match lobby = startedAndStubbed(MatchStatus.ACTIVE, "engine-99");

        matchService.cancelMatch("match-1", "host-1");

        assertThat(lobby.getStatus()).isEqualTo(MatchStatus.FINISHED);
        assertThat(lobby.getWinnerId()).isEqualTo("joiner-1");
        verify(gameEngine).removeMatch("engine-99");
        verify(messagingTemplate, times(2)).convertAndSend(anyString(), any(Object.class));
    }

    // ───────────────────────── password branches ─────────────────────────

    @Test
    void joinMatch_passwordRequired_throws428() {
        Match lobby = new Match("match-1", "host-1", "deck-host", "ABC123");
        lobby.setPassword("secret");
        when(matchRepository.findById("match-1")).thenReturn(Optional.of(lobby));

        assertThatThrownBy(() -> matchService.joinMatch("match-1", "joiner-1", "deck-join", null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Password required");
    }

    @Test
    void joinMatch_wrongPassword_throws403() {
        Match lobby = new Match("match-1", "host-1", "deck-host", "ABC123");
        lobby.setPassword("secret");
        when(matchRepository.findById("match-1")).thenReturn(Optional.of(lobby));

        assertThatThrownBy(() -> matchService.joinMatch("match-1", "joiner-1", "deck-join", "wrong"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Incorrect password");
    }

    @Test
    void joinMatch_correctPassword_joins() {
        Match lobby = new Match("match-1", "host-1", "deck-host", "ABC123");
        lobby.setPassword("secret");
        when(matchRepository.findById("match-1")).thenReturn(Optional.of(lobby));
        Player joiner = playerWithDeck("joiner-1", "deck-join", validDeck("deck-join"));
        when(playerRepository.findById("joiner-1")).thenReturn(Optional.of(joiner));

        Match result = matchService.joinMatch("match-1", "joiner-1", "deck-join", "secret");

        assertThat(result.getPlayerTwoId()).isEqualTo("joiner-1");
    }

    @Test
    void joinByCode_passwordRequired_throws428() {
        Match lobby = new Match("m-1", "host-1", "deck-host", "ABC123");
        lobby.setPassword("secret");
        when(matchRepository.findByJoinCode("ABC123")).thenReturn(Optional.of(lobby));

        assertThatThrownBy(() -> matchService.joinByCode("ABC123", "joiner-1", "deck-join", null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Password required");
    }

    @Test
    void joinByCode_wrongPassword_throws403() {
        Match lobby = new Match("m-1", "host-1", "deck-host", "ABC123");
        lobby.setPassword("secret");
        when(matchRepository.findByJoinCode("ABC123")).thenReturn(Optional.of(lobby));

        assertThatThrownBy(() -> matchService.joinByCode("ABC123", "joiner-1", "deck-join", "nope"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Incorrect password");
    }

    // ───────────────────────── after-commit lobby event ─────────────────────────

    @Test
    void setReady_firesLobbyEventAfterCommit() {
        startedAndStubbed(MatchStatus.WAITING, null);

        matchService.setReady("match-1", "host-1", true);
        fireAfterCommit();

        verify(messagingTemplate).convertAndSend(anyString(), any(Object.class));
    }

    // ───────────────────────── createBotMatch ─────────────────────────

    @Test
    void createBotMatch_success_startsMatchAgainstBot() {
        Map<String, Match> store = new HashMap<>();
        when(matchRepository.save(any(Match.class))).thenAnswer(inv -> {
            Match m = inv.getArgument(0);
            store.put(m.getId(), m);
            return m;
        });
        when(matchRepository.findById(anyString()))
                .thenAnswer(inv -> Optional.ofNullable(store.get(inv.getArgument(0))));

        Player host = playerWithDeck("host-1", "deck-host", validDeck("deck-host"));
        when(playerRepository.findByIdWithDecks("host-1")).thenReturn(Optional.of(host));

        Player bot = playerWithDeck("bot-easy", "bot-deck", validDeck("bot-deck"));
        bot.setActiveDeckId("bot-deck");
        when(playerRepository.findByIdWithDecks("bot-easy")).thenReturn(Optional.of(bot));
        when(playerRepository.findById("bot-easy")).thenReturn(Optional.of(bot));

        when(gameEngine.createMatch(any(), any(), any(), any())).thenReturn("engine-bot");

        CreateMatchResponse response = matchService.createBotMatch("host-1", "deck-host", "EASY");

        assertThat(response.getStatus()).isEqualTo(MatchStatus.SETUP);
        verify(gameEngine).createMatch(any(), any(), any(), any());
    }

    @Test
    void createBotMatch_botHasNoActiveDeck_throws500() {
        Player bot = new Player();
        bot.setId("bot-easy");
        bot.setActiveDeckId(null);
        bot.setDecks(new ArrayList<>());
        when(playerRepository.findByIdWithDecks("bot-easy")).thenReturn(Optional.of(bot));

        assertThatThrownBy(() -> matchService.createBotMatch("host-1", "deck-host", "EASY"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("no active deck");
    }

    // ───────────────────────── cleanup schedulers ─────────────────────────

    @Test
    void cleanupOrphanRooms_deletesExpiredStaleAndFinishesStaleActive() {
        Match expired = new Match("m-exp", "host-1", "deck", "AAA111");
        expired.setCreatedAt(LocalDateTime.now().minusHours(1));

        Match staleNoActivity = new Match("m-stale", "host-2", "deck", "BBB222");
        staleNoActivity.setCreatedAt(LocalDateTime.now());
        staleNoActivity.setLastActivityAt(LocalDateTime.now().minusMinutes(10));

        when(matchRepository.findByStatusOrderByCreatedAtDesc(MatchStatus.WAITING))
                .thenReturn(List.of(expired, staleNoActivity));
        when(playerRepository.findById("host-2")).thenReturn(Optional.of(new Player()));

        Match staleActive = startedLobby(MatchStatus.ACTIVE, "engine-1");
        staleActive.setLastActivityAt(LocalDateTime.now().minusHours(1));
        when(matchRepository.findByStatusInOrderByCreatedAtDesc(any())).thenReturn(List.of(staleActive));

        matchService.cleanupOrphanRooms();

        verify(matchRepository).delete(expired);
        verify(matchRepository).delete(staleNoActivity);
        verify(gameEngine).removeMatch("engine-1");
        assertThat(staleActive.getStatus()).isEqualTo(MatchStatus.FINISHED);
    }

    @Test
    void cleanupOrphanRooms_deletesWaitingRoomWhenHostMissing() {
        Match orphan = new Match("m-orphan", "ghost-host", "deck", "CCC333");
        orphan.setCreatedAt(LocalDateTime.now());
        orphan.setLastActivityAt(LocalDateTime.now());
        when(matchRepository.findByStatusOrderByCreatedAtDesc(MatchStatus.WAITING))
                .thenReturn(List.of(orphan));
        when(playerRepository.findById("ghost-host")).thenReturn(Optional.empty());
        when(matchRepository.findByStatusInOrderByCreatedAtDesc(any())).thenReturn(List.of());

        matchService.cleanupOrphanRooms();

        verify(matchRepository).delete(orphan);
    }

    @Test
    void cleanupOrphanBattles_removesEnginesForMissingOrFinishedMatches() {
        Map<String, String> battleMap = new HashMap<>();
        battleMap.put("engine-a", "match-a");
        battleMap.put("engine-b", "match-b");
        when(gameEngine.getBattleEntityMap()).thenReturn(battleMap);
        when(matchRepository.existsById("match-a")).thenReturn(false);
        when(matchRepository.existsById("match-b")).thenReturn(true);
        Match finished = new Match("match-b", "host", "deck", "CODE12");
        finished.setStatus(MatchStatus.FINISHED);
        when(matchRepository.findById("match-b")).thenReturn(Optional.of(finished));

        matchService.cleanupOrphanBattles();

        verify(gameEngine).removeMatch("engine-a");
        verify(gameEngine).removeMatch("engine-b");
    }

    @Test
    void onApplicationReady_runsLobbyCleanup() {
        when(matchRepository.findByStatusOrderByCreatedAtDesc(MatchStatus.WAITING)).thenReturn(List.of());
        when(matchRepository.findByStatusInOrderByCreatedAtDesc(any())).thenReturn(List.of());

        matchService.onApplicationReady();

        verify(matchRepository).findByStatusOrderByCreatedAtDesc(MatchStatus.WAITING);
    }

    // ───────────────────────── extra branch coverage ─────────────────────────

    @Test
    void createMatch_invalidDeck_throws400() {
        Player host = new Player();
        host.setId("host-1");
        host.setDecks(new ArrayList<>(List.of(
                new Deck("deck-1", "Too Small", new ArrayList<>(List.of(basic("c1", 0)))))));
        when(playerRepository.findByIdWithDecks("host-1")).thenReturn(Optional.of(host));

        assertThatThrownBy(() -> matchService.createMatch("host-1", "deck-1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not valid");
    }

    @Test
    void joinMatch_roomFull_throws409() {
        Match lobby = new Match("match-1", "host-1", "deck-host", "ABC123");
        lobby.setPlayerTwoId("existing");
        when(matchRepository.findById("match-1")).thenReturn(Optional.of(lobby));

        assertThatThrownBy(() -> matchService.joinMatch("match-1", "joiner-1", "deck-join"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already full");
    }

    @Test
    void joinByCode_joinerNotFound_throws404() {
        Match lobby = new Match("m-1", "host-1", "deck-host", "ABC123");
        when(matchRepository.findByJoinCode("ABC123")).thenReturn(Optional.of(lobby));
        when(playerRepository.findById("joiner-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.joinByCode("ABC123", "joiner-1", "deck-join"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Player not found");
    }

    @Test
    void joinByCode_deckNotOwned_throws404() {
        Match lobby = new Match("m-1", "host-1", "deck-host", "ABC123");
        when(matchRepository.findByJoinCode("ABC123")).thenReturn(Optional.of(lobby));
        Player joiner = playerWithDeck("joiner-1", "deck-join", validDeck("deck-join"));
        when(playerRepository.findById("joiner-1")).thenReturn(Optional.of(joiner));

        assertThatThrownBy(() -> matchService.joinByCode("ABC123", "joiner-1", "deck-other"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Deck not found");
    }

    @Test
    void joinByCode_invalidDeck_throws400() {
        Match lobby = new Match("m-1", "host-1", "deck-host", "ABC123");
        when(matchRepository.findByJoinCode("ABC123")).thenReturn(Optional.of(lobby));
        Deck shortDeck = new Deck("deck-join", "Too Small", new ArrayList<>(List.of(basic("c1", 0))));
        Player joiner = playerWithDeck("joiner-1", "deck-join", shortDeck);
        when(playerRepository.findById("joiner-1")).thenReturn(Optional.of(joiner));

        assertThatThrownBy(() -> matchService.joinByCode("ABC123", "joiner-1", "deck-join"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not valid");
    }

    @Test
    void joinByCode_correctPassword_joinsAndReportsHasPassword() {
        Match lobby = new Match("m-1", "host-1", "deck-host", "ABC123");
        lobby.setPassword("secret");
        when(matchRepository.findByJoinCode("ABC123")).thenReturn(Optional.of(lobby));
        Player joiner = playerWithDeck("joiner-1", "deck-join", validDeck("deck-join"));
        when(playerRepository.findById("joiner-1")).thenReturn(Optional.of(joiner));

        CreateMatchResponse response = matchService.joinByCode("ABC123", "joiner-1", "deck-join", "secret");

        assertThat(lobby.getPlayerTwoId()).isEqualTo("joiner-1");
        assertThat(response.isHasPassword()).isTrue();
    }

    @Test
    void startMatch_hostNotReady_throws409() {
        Match lobby = new Match("m-1", "host-1", "deck-host", "ABC123");
        lobby.setPlayerTwoId("joiner-1");
        lobby.setHostReady(false);
        lobby.setGuestReady(true);
        when(matchRepository.findById("m-1")).thenReturn(Optional.of(lobby));

        assertThatThrownBy(() -> matchService.startMatch("m-1", "host-1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("must be ready");
    }

    @Test
    void startMatch_hostHasNoDeck_throws409() {
        Match lobby = new Match("m-1", "host-1", "deck-host", "ABC123");
        lobby.setPlayerTwoId("joiner-1");
        lobby.setDeckTwoId("deck-join");
        lobby.setHostReady(true);
        lobby.setGuestReady(true);
        lobby.setDeckOneId(null);
        when(matchRepository.findById("m-1")).thenReturn(Optional.of(lobby));

        assertThatThrownBy(() -> matchService.startMatch("m-1", "host-1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Host has not selected a deck");
    }

    @Test
    void startMatch_guestHasNoDeck_throws409() {
        Match lobby = new Match("m-1", "host-1", "deck-host", "ABC123");
        lobby.setPlayerTwoId("joiner-1");
        lobby.setHostReady(true);
        lobby.setGuestReady(true);
        lobby.setDeckTwoId(null);
        when(matchRepository.findById("m-1")).thenReturn(Optional.of(lobby));

        assertThatThrownBy(() -> matchService.startMatch("m-1", "host-1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Guest has not selected a deck");
    }

    @Test
    void cancelMatch_setupStatus_finishesMatch() {
        Match lobby = startedAndStubbed(MatchStatus.SETUP, "engine-99");
        matchService.cancelMatch("match-1", "host-1");
        assertThat(lobby.getStatus()).isEqualTo(MatchStatus.FINISHED);
        assertThat(lobby.getWinnerId()).isEqualTo("joiner-1");
    }

    @Test
    void cancelMatch_activeGuestCancels_hostWins() {
        Match lobby = startedAndStubbed(MatchStatus.ACTIVE, "engine-99");
        matchService.cancelMatch("match-1", "joiner-1");
        assertThat(lobby.getWinnerId()).isEqualTo("host-1");
    }

    /** Stubs the snapshot-restore chain for a lobby whose in-memory engine is gone. */
    private void stubSnapshotRestore(Match lobby, String oldEngine, String newEngine, boolean withGuest)
            throws Exception {
        lobby.setStateJson("{\"snapshot\":true}");
        when(gameEngine.getMatchContext(oldEngine)).thenThrow(new RuntimeException("engine gone"));
        GameSnapshot snapshot = mock(GameSnapshot.class);
        GameContext ctx = mock(GameContext.class);
        when(objectMapper.readValue(anyString(), eq(GameSnapshot.class))).thenReturn(snapshot);
        when(snapshot.toGameContext(cardRegistryService)).thenReturn(ctx);
        Player p1 = new Player();
        p1.setId("host-1");
        when(playerRepository.findById("host-1")).thenReturn(Optional.of(p1));
        if (withGuest) {
            Player p2 = new Player();
            p2.setId("joiner-1");
            when(playerRepository.findById("joiner-1")).thenReturn(Optional.of(p2));
            when(gameEngine.restoreMatch(eq(ctx), eq(p1), eq(p2))).thenReturn(newEngine);
        } else {
            when(gameEngine.restoreMatch(eq(ctx), eq(p1), isNull())).thenReturn(newEngine);
        }
        when(gameEngine.getMatchState(newEngine, "host-1")).thenReturn(state(false, false, null));
    }

    @Test
    void reconnectPlayer_engineGone_restoresFromSnapshot() throws Exception {
        Match lobby = startedAndStubbed(MatchStatus.ACTIVE, "engine-old");
        stubSnapshotRestore(lobby, "engine-old", "engine-new", true);

        MatchStateDTO result = matchService.reconnectPlayer("match-1", "host-1");

        assertThat(result).isNotNull();
        assertThat(lobby.getEngineMatchId()).isEqualTo("engine-new");
        verify(gameEngine).restoreMatch(any(), any(), any());
    }

    @Test
    void reconnectPlayer_engineGone_soloHost_restoresWithNullGuest() throws Exception {
        Match lobby = new Match("match-1", "host-1", "deck-host", "ABC123");
        lobby.setStatus(MatchStatus.ACTIVE);
        lobby.setEngineMatchId("engine-old");
        when(matchRepository.findById("match-1")).thenReturn(Optional.of(lobby));
        stubSnapshotRestore(lobby, "engine-old", "engine-new", false);

        MatchStateDTO result = matchService.reconnectPlayer("match-1", "host-1");

        assertThat(result).isNotNull();
    }

    @Test
    void getMatchState_engineNotInMemory_reconnectsViaSnapshot() throws Exception {
        Match lobby = startedAndStubbed(MatchStatus.ACTIVE, "engine-old");
        stubSnapshotRestore(lobby, "engine-old", "engine-new", true);

        MatchStateDTO result = matchService.getMatchState("match-1", "host-1");

        assertThat(result).isNotNull();
    }

    @Test
    void getMatchState_notWaitingButSetupIncomplete_keepsStatus() {
        Match lobby = startedAndStubbed(MatchStatus.SETUP, "engine-99");
        MatchStateDTO st = new MatchStateDTO();
        st.setFinished(false);
        st.setWaitingForActiveSelection(false);
        st.setSetupComplete(false);
        when(gameEngine.getMatchContext("engine-99")).thenReturn(mock(GameContext.class));
        when(gameEngine.getMatchState("engine-99", "host-1")).thenReturn(st);

        matchService.getMatchState("match-1", "host-1");

        assertThat(lobby.getStatus()).isEqualTo(MatchStatus.SETUP);
    }

    @Test
    void cancelMatch_activeNoEngine_finishesWithoutRemovingEngine() {
        Match lobby = new Match("match-1", "host-1", "deck-host", "ABC123");
        lobby.setPlayerTwoId("joiner-1");
        lobby.setStatus(MatchStatus.ACTIVE);
        lobby.setEngineMatchId(null);
        when(matchRepository.findById("match-1")).thenReturn(Optional.of(lobby));

        matchService.cancelMatch("match-1", "host-1");

        assertThat(lobby.getStatus()).isEqualTo(MatchStatus.FINISHED);
        verify(gameEngine, never()).removeMatch(any());
    }

    @Test
    void surrender_noEngine_finishesWithoutRemovingEngine() {
        Match lobby = new Match("match-1", "host-1", "deck-host", "ABC123");
        lobby.setPlayerTwoId("joiner-1");
        lobby.setStatus(MatchStatus.ACTIVE);
        lobby.setEngineMatchId(null);
        when(matchRepository.findById("match-1")).thenReturn(Optional.of(lobby));

        matchService.surrender("match-1", "host-1");

        assertThat(lobby.getStatus()).isEqualTo(MatchStatus.FINISHED);
        assertThat(lobby.getWinnerId()).isEqualTo("joiner-1");
        verify(gameEngine, never()).removeMatch(any());
    }

    @Test
    void getMatchState_finishedWithNullWinner_doesNotGrantCardBacks() {
        Match lobby = startedAndStubbed(MatchStatus.ACTIVE, "engine-99");
        when(gameEngine.getMatchContext("engine-99")).thenReturn(mock(GameContext.class));
        when(gameEngine.getMatchState("engine-99", "host-1")).thenReturn(state(true, false, null));

        matchService.getMatchState("match-1", "host-1");

        assertThat(lobby.getStatus()).isEqualTo(MatchStatus.FINISHED);
        verify(cardBackService, never()).syncProgressUnlocks(any());
    }

    @Test
    void reconnectPlayer_snapshotDeserializationFails_throws500() throws Exception {
        Match lobby = new Match("match-1", "host-1", "deck-host", "ABC123");
        lobby.setStatus(MatchStatus.ACTIVE);
        lobby.setEngineMatchId(null);
        lobby.setStateJson("{broken}");
        when(matchRepository.findById("match-1")).thenReturn(Optional.of(lobby));
        when(objectMapper.readValue(anyString(), eq(GameSnapshot.class)))
                .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("bad") { });

        assertThatThrownBy(() -> matchService.reconnectPlayer("match-1", "host-1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Could not restore");
    }

    // ───────────────────────── helpers ─────────────────────────

    private Match startedLobby(MatchStatus status, String engineMatchId) {
        Match lobby = new Match("match-1", "host-1", "deck-host", "ABC123");
        lobby.setPlayerTwoId("joiner-1");
        lobby.setStatus(status);
        lobby.setEngineMatchId(engineMatchId);
        return lobby;
    }

    private MatchStateDTO state(boolean finished, boolean playing, String winnerId) {
        MatchStateDTO dto = new MatchStateDTO();
        dto.setFinished(finished);
        dto.setWaitingForActiveSelection(!playing);
        // Una partida "jugando" ya tiene el setup completo; el sync a ACTIVE lo requiere.
        dto.setSetupComplete(playing);
        dto.setWinnerPlayerId(winnerId);
        // Populate both sides so enrichWithCardBackUrls exercises its non-null branches.
        MatchStateDTO.PlayerBattlefieldDTO self = new MatchStateDTO.PlayerBattlefieldDTO();
        self.setPlayerId("host-1");
        MatchStateDTO.PlayerBattlefieldDTO opponent = new MatchStateDTO.PlayerBattlefieldDTO();
        opponent.setPlayerId("joiner-1");
        dto.setSelf(self);
        dto.setOpponent(opponent);
        return dto;
    }

    private Player playerWithDeck(String playerId, String deckId, Deck deck) {
        Player player = new Player();
        player.setId(playerId);
        player.setCollection(null);
        player.setDecks(new ArrayList<>(List.of(deck)));
        if (!deck.getId().equals(deckId)) {
            deck.setId(deckId);
        }
        return player;
    }

    private Deck validDeck(String deckId) {
        List<Card> cards = new ArrayList<>();
        int slot = 0;
        while (cards.size() < VALID_DECK_SIZE) {
            for (int copy = 0; copy < COPIES_PER_CARD && cards.size() < VALID_DECK_SIZE; copy++) {
                cards.add(basic(deckId + "-" + slot, slot));
            }
            slot++;
        }
        return new Deck(deckId, "Valid Deck", cards);
    }

    private PokemonCard basic(String id, int slot) {
        return PokemonCard.builder()
                .id(id)
                .name("Basic " + slot)
                .hp(60)
                .stage(PokemonStage.BASIC)
                .pokemonType(EnergyType.COLORLESS)
                .attacks(List.of())
                .abilities(List.of())
                .weaknesses(List.of())
                .resistances(List.of())
                .costToAttack(List.of())
                .retreatCost(0)
                .build();
    }
}
