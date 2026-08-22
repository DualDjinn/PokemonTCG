package ar.edu.utn.frc.tup.piii.services;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import ar.edu.utn.frc.tup.piii.dtos.engine.CreateMatchResponse;
import ar.edu.utn.frc.tup.piii.dtos.engine.GameAction;
import ar.edu.utn.frc.tup.piii.dtos.engine.MatchStateDTO;
import ar.edu.utn.frc.tup.piii.engine.GameEngine;
import ar.edu.utn.frc.tup.piii.engine.bot.BotDifficulty;
import ar.edu.utn.frc.tup.piii.models.Deck;
import ar.edu.utn.frc.tup.piii.engine.snapshot.GameSnapshot;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.Match;
import ar.edu.utn.frc.tup.piii.models.MatchStatus;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.repositories.MatchRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerRepository;

/**
 * Application service that owns the match lobby lifecycle (create, join, read
 * and act) and bridges persisted lobby aggregates to the in-memory battle
 * {@link GameEngine} facade.
 *
 * <p>Registered under an explicit bean name to coexist with the engine-layer
 * {@code ar.edu.utn.frc.tup.piii.engine.MatchService}, which keeps the same
 * simple class name.
 */
@Service("matchLobbyService")
public class MatchService {

    private static final int JOIN_CODE_LENGTH = 6;

    private static final Logger log = LoggerFactory.getLogger(MatchService.class);

    private final MatchRepository matchRepository;
    private final PlayerRepository playerRepository;
    private final GameEngine gameEngine;
    private final SimpMessagingTemplate messagingTemplate;
    private final CardBackService cardBackService;
    private final ObjectMapper objectMapper;
    private final CardRegistryService cardRegistryService;

    public MatchService(MatchRepository matchRepository,
                        PlayerRepository playerRepository,
                        GameEngine gameEngine,
                        SimpMessagingTemplate messagingTemplate,
                        CardBackService cardBackService,
                        ObjectMapper objectMapper,
                        CardRegistryService cardRegistryService) {
        this.matchRepository = matchRepository;
        this.playerRepository = playerRepository;
        this.gameEngine = gameEngine;
        this.messagingTemplate = messagingTemplate;
        this.cardBackService = cardBackService;
        this.objectMapper = objectMapper;
        this.cardRegistryService = cardRegistryService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        cleanupOrphanRooms();
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cleanupOrphanRooms() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);
        LocalDateTime activityCutoff = LocalDateTime.now().minusMinutes(2);
        LocalDateTime staleGameCutoff = LocalDateTime.now().minusMinutes(30);

        List<Match> waitingMatches = matchRepository.findByStatusOrderByCreatedAtDesc(MatchStatus.WAITING);
        for (Match match : waitingMatches) {
            if (match.getCreatedAt().isBefore(cutoff)) {
                matchRepository.delete(match);
                continue;
            }
            if (match.getPlayerTwoId() == null) {
                boolean hostExists = playerRepository.findById(match.getPlayerOneId()).isPresent();
                if (!hostExists) {
                    matchRepository.delete(match);
                    continue;
                }
                if (match.getLastActivityAt() == null
                        || match.getLastActivityAt().isBefore(activityCutoff)) {
                    matchRepository.delete(match);
                }
            }
        }

        List<MatchStatus> activeStatuses = List.of(MatchStatus.SETUP, MatchStatus.ACTIVE);
        List<Match> activeMatches = matchRepository.findByStatusInOrderByCreatedAtDesc(activeStatuses);
        for (Match match : activeMatches) {
            if (match.getLastActivityAt() != null
                    && match.getLastActivityAt().isBefore(staleGameCutoff)) {
                if (match.getEngineMatchId() != null) {
                    gameEngine.removeMatch(match.getEngineMatchId());
                    match.setEngineMatchId(null);
                }
                match.setStatus(MatchStatus.FINISHED);
                matchRepository.save(match);
            }
        }
    }

    @Scheduled(fixedRate = 300000)
    public void cleanupOrphanBattles() {
        Map<String, String> battleEntityMap = gameEngine.getBattleEntityMap();
        for (Map.Entry<String, String> entry : battleEntityMap.entrySet()) {
            String engineId = entry.getKey();
            String entityId = entry.getValue();
            if (!matchRepository.existsById(entityId)) {
                gameEngine.removeMatch(engineId);
                continue;
            }
            matchRepository.findById(entityId).ifPresent(m -> {
                if (m.getStatus() == MatchStatus.FINISHED) {
                    gameEngine.removeMatch(engineId);
                }
            });
        }
    }

    /**
     * Convenience overload that creates a public, non-spectator room without a
     * display name. Delegates to the full signature.
     */
    @Transactional
    public Match createMatch(String hostPlayerId, String deckId) {
        return createMatch(hostPlayerId, deckId, null, false, false);
    }

    /**
     * Creates a new match in {@link MatchStatus#WAITING} with the authenticated
     * player as host (player one) and generates a join code for the opponent.
     *
     * @param hostPlayerId    identifier of the authenticated host
     * @param deckId          deck the host wants to play with (must belong to the host)
     * @param roomName        display name of the room
     * @param isPrivate       whether the room is private (only joinable by code)
     * @param allowSpectators whether spectators are allowed
     * @return the persisted match aggregate
     * @throws ResponseStatusException 404 if the host or deck does not exist
     */
    @Transactional
    public Match createMatch(String hostPlayerId, String deckId, String roomName,
                             boolean isPrivate, boolean allowSpectators) {
        return createMatch(hostPlayerId, deckId, roomName, null, isPrivate, allowSpectators);
    }

    @Transactional
    public Match createMatch(String hostPlayerId, String deckId, String roomName,
                             String description, boolean isPrivate, boolean allowSpectators) {
        return createMatch(hostPlayerId, deckId, roomName, description, isPrivate, true, null, allowSpectators);
    }

    @Transactional
    public Match createMatch(String hostPlayerId, String deckId, String roomName,
                             String description, boolean isPrivate, boolean showInList,
                             String password, boolean allowSpectators) {
        if (deckId == null || deckId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A deck is required to create a match");
        }
        Player host = getPlayerWithDecksOrThrow(hostPlayerId);
        Deck deck = requireOwnedDeck(host, deckId);
        List<String> deckErrors = deck.validate(host, Set.of());
        if (!deckErrors.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Deck is not valid: " + String.join("; ", deckErrors));
        }

        Match match = new Match(UUID.randomUUID().toString(), hostPlayerId, deckId,
                generateJoinCode(), roomName, description, isPrivate, showInList, password, allowSpectators);
        match.setLastActivityAt(LocalDateTime.now());

        return matchRepository.save(match);
    }

    /**
     * Creates a match against an AI bot and immediately starts it.
     * Auto-joins the bot player resolved from the difficulty, marks both
     * sides ready and starts the engine match. The returned response already
     * reflects the SETUP status, so the client can navigate directly to the
     * battle board.
     *
     * @param hostPlayerId    authenticated human player id
     * @param deckId          deck the human will play with
     * @param botDifficultyStr difficulty string ("EASY"/"MEDIUM"/"HARD")
     * @return the started match response with engineMatchId populated
     */
    @Transactional
    public CreateMatchResponse createBotMatch(String hostPlayerId, String deckId, String botDifficultyStr) {
        BotDifficulty difficulty = BotDifficulty.fromString(botDifficultyStr);
        String botPlayerId = difficulty.botPlayerId();

        Player bot = getPlayerWithDecksOrThrow(botPlayerId);
        String botDeckId = bot.getActiveDeckId();
        if (botDeckId == null || botDeckId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Bot " + botPlayerId + " has no active deck configured");
        }

        Match match = createMatch(hostPlayerId, deckId, "Bot Match (" + difficulty.name() + ")",
                "", true, false, "", false);

        joinMatch(match.getId(), botPlayerId, botDeckId);

        match = getMatchOrThrow(match.getId());
        match.setHostReady(true);
        match.setGuestReady(true);
        match.setLastActivityAt(LocalDateTime.now());
        matchRepository.save(match);

        return startMatch(match.getId(), hostPlayerId);
    }

    /**
     * Joins the second player to a waiting match. The submitted deck is validated;
     * if valid, the engine performs the opening setup and the match transitions to
     * {@link MatchStatus#SETUP}.
     *
     * @param matchId        identifier of the match to join
     * @param joiningPlayerId identifier of the authenticated joining player
     * @param deckId         deck the joining player wants to play with
     * @return the updated match aggregate
     * @throws ResponseStatusException 404 if the match/player/deck does not exist,
     *                                 409 if the match is not waiting or the player
     *                                 is the host, 400 if the deck is invalid
     */
    @Transactional
    public Match joinMatch(String matchId, String joiningPlayerId, String deckId) {
        return joinMatch(matchId, joiningPlayerId, deckId, null);
    }

    @Transactional
    public Match joinMatch(String matchId, String joiningPlayerId, String deckId, String password) {
        Match match = getMatchOrThrow(matchId);
        if (match.getStatus() != MatchStatus.WAITING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Match is not open to join");
        }
        if (joiningPlayerId.equals(match.getPlayerOneId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Host cannot join their own match");
        }
        if (match.getPlayerTwoId() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Room is already full");
        }
        if (match.getPassword() != null && !match.getPassword().isEmpty()) {
            if (password == null || password.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED, "Password required");
            }
            if (!match.getPassword().equals(password)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Incorrect password");
            }
        }

        Player joining = getPlayerOrThrow(joiningPlayerId);

        Deck deck = requireOwnedDeck(joining, deckId);
        List<String> deckErrors = deck.validate(joining, Set.of());
        if (!deckErrors.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Submitted deck is not valid: " + String.join("; ", deckErrors));
        }

        match.setPlayerTwoId(joiningPlayerId);
        match.setDeckTwoId(deckId);
        match.setLastActivityAt(LocalDateTime.now());

        match = matchRepository.save(match);
        publishLobbyEvent(match.getId(), "GUEST_JOINED");
        return match;
    }

    /**
     * Returns the match state projected for the requesting player. The opponent's
     * hand and deck order are never revealed (the engine enforces this).
     *
     * @param matchId  identifier of the match
     * @param playerId requesting player identifier
     * @return the player-specific match state
     * @throws ResponseStatusException 404 if the match does not exist or has not
     *                                 started, 403 if the player is not a member
     */
    @Transactional
    public MatchStateDTO getMatchState(String matchId, String playerId) {
        Match match = getMatchOrThrow(matchId);
        requireMembership(match, playerId);
        String engineMatchId = requireEngineMatch(match);

        if (engineExists(engineMatchId)) {
            MatchStateDTO state = getEngineState(engineMatchId, playerId, match);
            syncStatusFromEngine(match, state);
            return state;
        }

        // Engine not in memory — restore from snapshot
        return reconnectPlayer(matchId, playerId);
    }

    private MatchStateDTO getEngineState(String engineMatchId, String playerId, Match match) {
        MatchStateDTO state = gameEngine.getMatchState(engineMatchId, playerId);
        enrichWithCardBackUrls(state, match);
        return state;
    }

    private void enrichWithCardBackUrls(MatchStateDTO state, Match match) {
        if (state.getSelf() != null) {
            String url = cardBackService.getActiveCardBackUrl(state.getSelf().getPlayerId());
            state.getSelf().setCardBackUrl(url);
        }
        if (state.getOpponent() != null) {
            String url = cardBackService.getActiveCardBackUrl(state.getOpponent().getPlayerId());
            state.getOpponent().setCardBackUrl(url);
        }
    }

    /**
     * Submits a player action to the battle engine for an in-progress match.
     *
     * @param matchId  identifier of the match
     * @param playerId acting player identifier
     * @param action   action to submit
     * @throws ResponseStatusException 404 if the match does not exist or has not
     *                                 started, 403 if the player is not a member
     */
    @Transactional
    public void submitAction(String matchId, String playerId, GameAction action) {
        Match match = getMatchOrThrow(matchId);
        requireMembership(match, playerId);
        String engineMatchId = requireEngineMatch(match);

        gameEngine.submitAction(engineMatchId, playerId, action);
        syncStatusFromEngine(match, getEngineState(engineMatchId, playerId, match));
    }

    /**
     * Requests dealing of opening hands for both players.
     * Idempotent: only the first call per match triggers the deal.
     */
    @Transactional
    public void requestDeal(String matchId, String playerId) {
        Match match = getMatchOrThrow(matchId);
        requireMembership(match, playerId);
        String engineMatchId = requireEngineMatch(match);

        gameEngine.dealCards(engineMatchId);
        syncStatusFromEngine(match, getEngineState(engineMatchId, playerId, match));
    }

    @Transactional
    public void selectActive(String matchId, String playerId, String cardId) {
        Match match = getMatchOrThrow(matchId);
        requireMembership(match, playerId);
        String engineMatchId = requireEngineMatch(match);

        gameEngine.selectActivePokemon(engineMatchId, playerId, cardId);
    }

    @Transactional
    public void placeBench(String matchId, String playerId, String cardId) {
        Match match = getMatchOrThrow(matchId);
        requireMembership(match, playerId);
        String engineMatchId = requireEngineMatch(match);

        gameEngine.placeBenchPokemon(engineMatchId, playerId, cardId);
    }

    @Transactional
    public void confirmSetup(String matchId, String playerId) {
        Match match = getMatchOrThrow(matchId);
        requireMembership(match, playerId);
        String engineMatchId = requireEngineMatch(match);

        gameEngine.confirmSetupReady(engineMatchId, playerId);
        syncStatusFromEngine(match, getEngineState(engineMatchId, playerId, match));
    }

    @Transactional
    public void startSetupCountdown(String matchId, String playerId) {
        Match match = getMatchOrThrow(matchId);
        requireMembership(match, playerId);
        String engineMatchId = requireEngineMatch(match);

        gameEngine.startSetupCountdown(engineMatchId, playerId);
    }

    @Transactional
    public void processSetup(String matchId, String playerId, String activeCardId, List<String> benchCardIds) {
        Match match = getMatchOrThrow(matchId);
        requireMembership(match, playerId);
        String engineMatchId = requireEngineMatch(match);

        gameEngine.selectActivePokemon(engineMatchId, playerId, activeCardId);
        if (benchCardIds != null) {
            for (String benchCardId : benchCardIds) {
                gameEngine.placeBenchPokemon(engineMatchId, playerId, benchCardId);
            }
        }
        gameEngine.confirmSetupReady(engineMatchId, playerId);
        syncStatusFromEngine(match, getEngineState(engineMatchId, playerId, match));
    }

    @Transactional
    public void resolveMulliganDraw(String matchId, String playerId, int count) {
        Match match = getMatchOrThrow(matchId);
        requireMembership(match, playerId);
        String engineMatchId = requireEngineMatch(match);

        gameEngine.resolveMulliganDraw(engineMatchId, playerId, count);
        syncStatusFromEngine(match, getEngineState(engineMatchId, playerId, match));
    }

    @Transactional
    public MatchStateDTO chooseCoinSide(String matchId, String playerId, String coinSide) {
        Match match = getMatchOrThrow(matchId);
        requireMembership(match, playerId);
        String engineMatchId = requireEngineMatch(match);

        gameEngine.chooseCoinSide(engineMatchId, playerId, coinSide);
        MatchStateDTO state = getEngineState(engineMatchId, playerId, match);
        syncStatusFromEngine(match, state);
        return state;
    }

    @Transactional
    public void selectPrize(String matchId, String playerId, int prizeIndex) {
        Match match = getMatchOrThrow(matchId);
        requireMembership(match, playerId);
        String engineMatchId = requireEngineMatch(match);
        gameEngine.selectPrize(engineMatchId, playerId, prizeIndex);
        syncStatusFromEngine(match, getEngineState(engineMatchId, playerId, match));
    }

    @Transactional
    public void promoteFromBench(String matchId, String playerId, int benchSlot) {
        Match match = getMatchOrThrow(matchId);
        requireMembership(match, playerId);
        String engineMatchId = requireEngineMatch(match);
        gameEngine.promoteFromBench(engineMatchId, playerId, benchSlot);
        syncStatusFromEngine(match, getEngineState(engineMatchId, playerId, match));
    }

    @Transactional
    public void surrender(String matchId, String playerId) {
        Match match = getMatchOrThrow(matchId);
        requireMembership(match, playerId);
        String opponentId = playerId.equals(match.getPlayerOneId())
                ? match.getPlayerTwoId() : match.getPlayerOneId();
        if (match.getEngineMatchId() != null) {
            gameEngine.removeMatch(match.getEngineMatchId());
            match.setEngineMatchId(null);
        }
        match.setStatus(MatchStatus.FINISHED);
        match.setWinnerId(opponentId);
        matchRepository.save(match);
        publishMatchEnded(match.getId(), opponentId, match.getPlayerOneId(), match.getPlayerTwoId());
    }

    @Transactional
    public MatchStateDTO chooseFirstTurn(String matchId, String playerId, String starterPlayerId) {
        Match match = getMatchOrThrow(matchId);
        requireMembership(match, playerId);
        String engineMatchId = requireEngineMatch(match);

        gameEngine.chooseFirstTurn(engineMatchId, playerId, starterPlayerId);
        MatchStateDTO state = getEngineState(engineMatchId, playerId, match);
        syncStatusFromEngine(match, state);
        return state;
    }

    /**
     * Returns all matches in WAITING, SETUP or ACTIVE status that are visible
     * in the lobby (showInList = true).
     *
     * @return list of matches ordered by creation date descending
     */
    @Transactional(readOnly = true)
    public List<CreateMatchResponse> listLobby() {
        List<Match> matches = matchRepository.findByStatusInOrderByCreatedAtDesc(
                List.of(MatchStatus.WAITING, MatchStatus.SETUP, MatchStatus.ACTIVE));
        return matches.stream()
                .filter(Match::isShowInList)
                .map(this::toCreateMatchResponse)
                .toList();
    }

    /**
     * Joins a match by its shareable join code. The player is added as player two
     * but the match stays in WAITING until the host starts it.
     *
     * @param code             the join code of the room
     * @param joiningPlayerId  identifier of the joining player
     * @param deckId           deck the joining player wants to use
     * @return the updated match response
     */
    @Transactional
    public CreateMatchResponse joinByCode(String code, String joiningPlayerId, String deckId) {
        return joinByCode(code, joiningPlayerId, deckId, null);
    }

    @Transactional
    public CreateMatchResponse joinByCode(String code, String joiningPlayerId, String deckId, String password) {
        Match match = matchRepository.findByJoinCode(code.toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        if (match.getStatus() != MatchStatus.WAITING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Room is not open to join");
        }
        if (joiningPlayerId.equals(match.getPlayerOneId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Host cannot join their own room");
        }
        if (match.getPlayerTwoId() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Room is already full");
        }
        if (match.getPassword() != null && !match.getPassword().isEmpty()) {
            if (password == null || password.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED, "Password required");
            }
            if (!match.getPassword().equals(password)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Incorrect password");
            }
        }

        Player joining = getPlayerOrThrow(joiningPlayerId);
        Deck deck = requireOwnedDeck(joining, deckId);
        List<String> deckErrors = deck.validate(joining, Set.of());
        if (!deckErrors.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Submitted deck is not valid: " + String.join("; ", deckErrors));
        }

        match.setPlayerTwoId(joiningPlayerId);
        match.setDeckTwoId(deckId);
        match = matchRepository.save(match);
        publishLobbyEvent(match.getId(), "GUEST_JOINED");

        return toCreateMatchResponse(match);
    }

    /**
     * Toggles the ready state of a player in the lobby.
     *
     * @param matchId  identifier of the match
     * @param playerId identifier of the player
     * @param ready    whether the player is ready
     * @return the updated match response
     */
    @Transactional
    public CreateMatchResponse setReady(String matchId, String playerId, boolean ready) {
        Match match = getMatchOrThrow(matchId);
        requireMembership(match, playerId);

        if (playerId.equals(match.getPlayerOneId())) {
            match.setHostReady(ready);
        } else {
            match.setGuestReady(ready);
        }

        match.setLastActivityAt(LocalDateTime.now());
        match = matchRepository.save(match);
        publishLobbyEvent(match.getId(), "READY_CHANGED");
        return toCreateMatchResponse(match);
    }

    /**
     * Starts a match by transitioning it from WAITING to SETUP.
     * Only the host can start. Both players must be present and ready.
     *
     * @param matchId  identifier of the match
     * @param playerId identifier of the requesting player
     * @return the updated match response
     */
    @Transactional
    public CreateMatchResponse startMatch(String matchId, String playerId) {
        Match match = getMatchOrThrow(matchId);

        if (!playerId.equals(match.getPlayerOneId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the host can start the match");
        }
        if (match.getPlayerTwoId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot start without an opponent");
        }
        if (!match.isHostReady() || !match.isGuestReady()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Both players must be ready");
        }
        if (match.getDeckOneId() == null || match.getDeckOneId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Host has not selected a deck");
        }
        if (match.getDeckTwoId() == null || match.getDeckTwoId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Guest has not selected a deck");
        }

        Player host = getPlayerWithDecksOrThrow(match.getPlayerOneId());
        Player guest = getPlayerWithDecksOrThrow(match.getPlayerTwoId());
        Deck hostDeck = requireOwnedDeck(host, match.getDeckOneId());
        Deck guestDeck = requireOwnedDeck(guest, match.getDeckTwoId());

        String engineMatchId = gameEngine.createMatch(host, guest, hostDeck, guestDeck);
        gameEngine.setEntityMatchId(engineMatchId, match.getId());
        match.setEngineMatchId(engineMatchId);
        match.setStatus(MatchStatus.SETUP);
        match.setStarted(true);
        match = matchRepository.save(match);
        publishLobbyEvent(match.getId(), "MATCH_STARTED");
        return toCreateMatchResponse(match);
    }

    /**
     * Cancels or leaves a match in the waiting room.
     * <ul>
     *   <li>If the host cancels, the match is deleted entirely.</li>
     *   <li>If the guest leaves, the guest slot is cleared so another player can join.</li>
     * </ul>
     *
     * @param matchId  identifier of the match
     * @param playerId identifier of the requesting player
     * @throws ResponseStatusException 404 if the match does not exist,
     *                                 403 if the player is not a member
     */
    @Transactional
    public void cancelMatch(String matchId, String playerId) {
        Match match = getMatchOrThrow(matchId);
        requireMembership(match, playerId);

        if (match.getStatus() == MatchStatus.SETUP || match.getStatus() == MatchStatus.ACTIVE) {
            String opponentId = playerId.equals(match.getPlayerOneId())
                    ? match.getPlayerTwoId()
                    : match.getPlayerOneId();

            if (match.getEngineMatchId() != null) {
                gameEngine.removeMatch(match.getEngineMatchId());
                match.setEngineMatchId(null);
            }

            match.setStatus(MatchStatus.FINISHED);
            match.setWinnerId(opponentId);
            matchRepository.save(match);

            Map<String, Object> payload = new HashMap<>();
            payload.put("phase", "FINISHED");
            payload.put("winnerId", opponentId);

            Map<String, Object> event = new HashMap<>();
            event.put("type", "MATCH_FINISHED");
            event.put("matchId", matchId);
            event.put("payload", payload);

            messagingTemplate.convertAndSend(
                    "/topic/matches/" + matchId + "/state/" + match.getPlayerOneId(), (Object) event);
            if (match.getPlayerTwoId() != null) {
                messagingTemplate.convertAndSend(
                        "/topic/matches/" + matchId + "/state/" + match.getPlayerTwoId(), (Object) event);
            }
        } else if (playerId.equals(match.getPlayerOneId())) {
            publishLobbyEvent(matchId, "MATCH_CANCELLED");
            matchRepository.delete(match);
        } else {
            match.setPlayerTwoId(null);
            match.setDeckTwoId(null);
            match.setGuestReady(false);
            matchRepository.save(match);
            publishLobbyEvent(matchId, "GUEST_LEFT");
        }
    }

    /**
     * Returns the lobby state for a match in the waiting room and updates
     * the host's activity timestamp so the cleanup scheduler can detect
     * stale rooms whose host has disconnected.
     *
     * @param matchId  identifier of the match
     * @param playerId requesting player identifier
     * @return the lobby state response
     */
    @Transactional
    public CreateMatchResponse getLobbyState(String matchId, String playerId) {
        Match match = getMatchOrThrow(matchId);
        requireMembership(match, playerId);
        if (playerId.equals(match.getPlayerOneId())) {
            match.setLastActivityAt(LocalDateTime.now());
            match = matchRepository.save(match);
        }
        return toCreateMatchResponse(match);
    }

    /**
     * Reconnects a player to an active match, clearing the disconnect flag
     * and returning the current engine state.
     */
    public MatchStateDTO reconnectPlayer(String matchId, String playerId) {
        Match match = getMatchOrThrow(matchId);
        requireMembership(match, playerId);

        // Clear disconnect flag
        if (playerId.equals(match.getDisconnectedPlayerId())) {
            match.setDisconnectedPlayerId(null);
            match.setDisconnectedAt(null);
            matchRepository.save(match);
        }

        // Check if the engine match exists, or restore from snapshot
        String engineId = match.getEngineMatchId();
        if (engineId != null && engineExists(engineId)) {
            return getEngineState(engineId, playerId, match);
        }

        // Restore from snapshot
        GameContext restored = restoreFromSnapshot(matchId);
        if (restored != null) {
            Player playerOne = playerRepository.findById(match.getPlayerOneId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));
            Player playerTwo = match.getPlayerTwoId() != null
                    ? playerRepository.findById(match.getPlayerTwoId()).orElse(null)
                    : null;
            engineId = gameEngine.restoreMatch(restored, playerOne, playerTwo);
            match.setEngineMatchId(engineId);
            matchRepository.save(match);
            return getEngineState(engineId, playerId, match);
        }

        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Could not restore match state");
    }

    /**
     * Returns active matches where the given player is a member.
     */
    public List<CreateMatchResponse> getPendingMatches(String playerId) {
        List<MatchStatus> activeStatuses = List.of(MatchStatus.ACTIVE, MatchStatus.SETUP, MatchStatus.WAITING);
        List<Match> asPlayerOne = matchRepository.findByPlayerOneIdAndStatusIn(playerId, activeStatuses);
        List<Match> asPlayerTwo = matchRepository.findByPlayerTwoIdAndStatusIn(playerId, activeStatuses);

        List<CreateMatchResponse> results = new java.util.ArrayList<>();
        for (Match m : asPlayerOne) results.add(toCreateMatchResponse(m));
        for (Match m : asPlayerTwo) {
            if (results.stream().noneMatch(r -> r.getMatchId().equals(m.getId()))) {
                results.add(toCreateMatchResponse(m));
            }
        }
        return results;
    }

    /**
     * Returns the most recent match the given player lost by abandonment, if any.
     *
     * <p>Only considers matches that finished by forfeit (the player disconnected
     * and failed to reconnect within the allowed window) within a recent window,
     * so stale results from long ago are ignored.
     *
     * @param playerId identifier of the requesting player
     * @return the identifier of the most recent abandoned match, or {@code null}
     */
    public String getRecentAbandonment(String playerId) {
        java.time.Duration window = java.time.Duration.ofMinutes(15);
        LocalDateTime cutoff = LocalDateTime.now().minus(window);
        return matchRepository.findByForfeitPlayerId(playerId).stream()
                .filter(m -> m.getStatus() == MatchStatus.FINISHED)
                .filter(m -> m.getFinishedAt() != null && m.getFinishedAt().isAfter(cutoff))
                .max((a, b) -> a.getFinishedAt().compareTo(b.getFinishedAt()))
                .map(Match::getId)
                .orElse(null);
    }

    private boolean engineExists(String engineMatchId) {
        try {
            gameEngine.getMatchContext(engineMatchId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void startCountdown(String matchId, String playerId, String phase, int totalSeconds) {
        Match match = getMatchOrThrow(matchId);
        if (!playerId.equals(match.getPlayerOneId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the host can start the countdown");
        }

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", "COUNTDOWN_UPDATE");
        event.put("matchId", matchId);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("phase", phase);
        payload.put("totalSeconds", totalSeconds);
        event.put("payload", payload);

        messagingTemplate.convertAndSend(
                "/topic/matches/" + matchId + "/lobby", (Object) event);
    }

    public void cancelCountdown(String matchId, String playerId) {
        Match match = getMatchOrThrow(matchId);
        if (!playerId.equals(match.getPlayerOneId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the host can cancel the countdown");
        }

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", "COUNTDOWN_CANCELLED");
        event.put("matchId", matchId);
        event.put("payload", new LinkedHashMap<>());

        messagingTemplate.convertAndSend(
                "/topic/matches/" + matchId + "/lobby", (Object) event);
    }

    /**
     * Builds a {@link CreateMatchResponse} from a {@link Match} entity,
     * resolving the host and guest trainer names from the player repository.
     */
    public CreateMatchResponse toCreateMatchResponse(Match match) {
        CreateMatchResponse response = new CreateMatchResponse();
        response.setMatchId(match.getId());
        response.setJoinCode(match.getJoinCode());
        response.setStatus(match.getStatus());
        response.setRoomName(match.getRoomName());
        response.setDescription(match.getDescription());
        response.setPrivate(match.isPrivate());
        response.setShowInList(match.isShowInList());
        response.setHasPassword(match.getPassword() != null && !match.getPassword().isEmpty());
        response.setAllowSpectators(match.isAllowSpectators());
        response.setStarted(match.isStarted());
        response.setHostReady(match.isHostReady());
        response.setGuestReady(match.isGuestReady());

        playerRepository.findByIdWithDecks(match.getPlayerOneId()).ifPresent(host -> {
            response.setHostTrainerName(host.getTrainerName());
            response.setHostCharacter(host.getCharacter());
            response.setHostCardBackUrl(cardBackService.getActiveCardBackUrl(host.getId()));
        });

        if (match.getPlayerTwoId() != null) {
            playerRepository.findByIdWithDecks(match.getPlayerTwoId()).ifPresent(guest -> {
                response.setGuestTrainerName(guest.getTrainerName());
                response.setGuestCharacter(guest.getCharacter());
                response.setGuestCardBackUrl(cardBackService.getActiveCardBackUrl(guest.getId()));
            });
        }

        return response;
    }

    private void syncStatusFromEngine(Match match, MatchStateDTO state) {
        MatchStatus previous = match.getStatus();
        if (state.isFinished()) {
            match.setStatus(MatchStatus.FINISHED);
            match.setWinnerId(state.getWinnerPlayerId());
            grantProgressCardBacks(state.getWinnerPlayerId());
        } else if (!state.isWaitingForActiveSelection() && state.isSetupComplete()) {
            match.setStatus(MatchStatus.ACTIVE);
        }
        if (match.getStatus() != previous || state.isFinished()) {
            matchRepository.save(match);
        }
        persistSnapshot(match);
    }

    /**
     * Al ganar una partida, otorga los dorsos de carta por progreso que el
     * ganador haya desbloqueado. Nunca interrumpe el flujo del match.
     */
    /**
     * Serializes the current engine state into {@link Match#stateJson} and persists it.
     */
    private void persistSnapshot(Match match) {
        try {
            String engineMatchId = match.getEngineMatchId();
            if (engineMatchId == null) return;
            GameContext ctx = gameEngine.getMatchContext(engineMatchId);
            if (ctx == null) return;
            GameSnapshot snapshot = GameSnapshot.from(ctx);
            snapshot.setDisconnectedPlayerId(match.getDisconnectedPlayerId());
            if (match.getDisconnectedAt() != null) {
                snapshot.setDisconnectedAtMs(match.getDisconnectedAt()
                        .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
            }
            match.setStateJson(objectMapper.writeValueAsString(snapshot));
            matchRepository.save(match);
        } catch (Exception e) {
            log.warn("Failed to persist match snapshot for {}: {}", match.getId(), e.getMessage());
        }
    }

    /**
     * Restores a game engine context from a persisted snapshot.
     * @return restored GameContext, or null if the snapshot is missing/invalid
     */
    public GameContext restoreFromSnapshot(String matchId) {
        Match match = getMatchOrThrow(matchId);
        String json = match.getStateJson();
        if (json == null || json.isBlank()) return null;
        try {
            GameSnapshot snapshot = objectMapper.readValue(json, GameSnapshot.class);
            return snapshot.toGameContext(cardRegistryService);
        } catch (Exception e) {
            log.warn("Failed to restore snapshot for {}: {}", matchId, e.getMessage());
            return null;
        }
    }

    private void grantProgressCardBacks(String winnerId) {
        if (winnerId == null) {
            return;
        }
        try {
            cardBackService.syncProgressUnlocks(winnerId);
        } catch (Exception e) {
            log.warn("Failed to sync card back unlocks for winner {}: {}", winnerId, e.getMessage());
        }
    }

    public Match getMatchOrThrow(String matchId) {
        if (matchId == null || matchId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found");
        }
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found"));
    }

    private Player getPlayerOrThrow(String playerId) {
        return playerRepository.findById(playerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));
    }

    private Player getPlayerWithDecksOrThrow(String playerId) {
        return playerRepository.findByIdWithDecks(playerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));
    }

    private Deck requireOwnedDeck(Player player, String deckId) {
        if (player.getDecks() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Deck not found");
        }
        return player.getDecks().stream()
                .filter(d -> d.getId().equals(deckId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deck not found"));
    }

    private void requireMembership(Match match, String playerId) {
        if (!match.containsPlayer(playerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Player does not belong to this match");
        }
    }

    private String requireEngineMatch(Match match) {
        String engineMatchId = match.getEngineMatchId();
        if (engineMatchId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match has not started yet");
        }
        return engineMatchId;
    }

    /**
     * Publishes a lobby event via STOMP so both players in the waiting room
     * receive real-time updates without polling.
     */
    private void publishLobbyEvent(String matchId, String type) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        Match match = matchRepository.findById(matchId).orElse(null);
                        if (match == null) return;
                        CreateMatchResponse state = toCreateMatchResponse(match);

                        Map<String, Object> event = new LinkedHashMap<>();
                        event.put("type", type);
                        event.put("matchId", matchId);
                        Map<String, Object> payload = new LinkedHashMap<>();
                        payload.put("status", match.getStatus());
                        payload.put("hostReady", match.isHostReady());
                        payload.put("guestReady", match.isGuestReady());
                        payload.put("guestTrainerName", state.getGuestTrainerName());
                        payload.put("guestCharacter", state.getGuestCharacter());
                        payload.put("started", match.isStarted());
                        event.put("payload", payload);

                        messagingTemplate.convertAndSend(
                                "/topic/matches/" + matchId + "/lobby", (Object) event);
                    }
                }
        );
    }

    public void notifyPokemonHover(String matchId, String playerId) {
        Match match = getMatchOrThrow(matchId);
        requireMembership(match, playerId);
        String opponentId = playerId.equals(match.getPlayerOneId())
                ? match.getPlayerTwoId() : match.getPlayerOneId();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "OPPONENT_HOVER");
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", "OPPONENT_HOVER");
        event.put("matchId", matchId);
        event.put("payload", payload);

        messagingTemplate.convertAndSend(
                "/topic/matches/" + matchId + "/state/" + opponentId, (Object) event);
    }

    public void notifyPokemonUnhover(String matchId, String playerId) {
        Match match = getMatchOrThrow(matchId);
        requireMembership(match, playerId);
        String opponentId = playerId.equals(match.getPlayerOneId())
                ? match.getPlayerTwoId() : match.getPlayerOneId();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "OPPONENT_UNHOVER");
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", "OPPONENT_UNHOVER");
        event.put("matchId", matchId);
        event.put("payload", payload);

        messagingTemplate.convertAndSend(
                "/topic/matches/" + matchId + "/state/" + opponentId, (Object) event);
    }

    private void publishMatchEnded(String matchId, String winnerId, String playerOneId, String playerTwoId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("activePlayerId", winnerId);
        payload.put("winnerId", winnerId);
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", "MATCH_FINISHED");
        event.put("matchId", matchId);
        event.put("payload", payload);

        messagingTemplate.convertAndSend(
                "/topic/matches/" + matchId + "/state/" + playerOneId, (Object) event);
        messagingTemplate.convertAndSend(
                "/topic/matches/" + matchId + "/state/" + playerTwoId, (Object) event);
        messagingTemplate.convertAndSend(
                "/topic/matches/" + matchId + "/lobby", (Object) event);
    }

    private String generateJoinCode() {
        String code;
        do {
            code = UUID.randomUUID().toString()
                    .replace("-", "")
                    .substring(0, JOIN_CODE_LENGTH)
                    .toUpperCase();
        } while (matchRepository.findByJoinCode(code).isPresent());
        return code;
    }
}
