package ar.edu.utn.frc.tup.piii.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frc.tup.piii.configs.Authenticated;
import ar.edu.utn.frc.tup.piii.dtos.common.ErrorApi;
import ar.edu.utn.frc.tup.piii.dtos.engine.AbandonedResultResponse;
import ar.edu.utn.frc.tup.piii.dtos.engine.CreateMatchRequest;
import ar.edu.utn.frc.tup.piii.dtos.engine.ChooseCoinSideRequest;
import ar.edu.utn.frc.tup.piii.dtos.engine.ChooseFirstTurnRequest;
import ar.edu.utn.frc.tup.piii.dtos.engine.CreateMatchResponse;
import ar.edu.utn.frc.tup.piii.dtos.engine.GameAction;
import ar.edu.utn.frc.tup.piii.dtos.engine.JoinByCodeRequest;
import ar.edu.utn.frc.tup.piii.dtos.engine.JoinMatchRequest;
import ar.edu.utn.frc.tup.piii.dtos.engine.MatchStateDTO;
import ar.edu.utn.frc.tup.piii.dtos.engine.SetReadyRequest;
import ar.edu.utn.frc.tup.piii.models.Match;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.services.MatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST endpoints for the match lifecycle: creating a match, joining as the
 * second player, reading the player-specific state and submitting in-game
 * actions.
 *
 * <p>The controller is intentionally thin: authentication is resolved by the
 * {@link ar.edu.utn.frc.tup.piii.configs.AuthenticatedPlayerResolver} and all
 * rules are delegated to {@link MatchService} (lobby lifecycle) and, through
 * it, to the battle engine facade.
 */
@RestController
@RequestMapping("/api/matches")
@Tag(name = "Matches", description = "Match lifecycle and gameplay endpoints")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @Operation(summary = "List lobby rooms",
               description = "Returns all public matches in WAITING status for the lobby view.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Room list retrieved")
    })
    @GetMapping("/lobby")
    public ResponseEntity<List<CreateMatchResponse>> listLobby() {
        return ResponseEntity.ok(matchService.listLobby());
    }

    @Operation(summary = "Create a new match",
               description = "The authenticated player creates a match and is assigned as player one. "
                           + "Returns the match id and a join code the opponent can use to join.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Match created",
            content = @Content(schema = @Schema(implementation = CreateMatchResponse.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated",
            content = @Content(schema = @Schema(implementation = ErrorApi.class))),
        @ApiResponse(responseCode = "404", description = "Deck not found",
            content = @Content(schema = @Schema(implementation = ErrorApi.class)))
    })
    @PostMapping
    public ResponseEntity<CreateMatchResponse> createMatch(
            @Authenticated Player player,
            @RequestBody CreateMatchRequest request) {
        if (request.isBotMatch()) {
            CreateMatchResponse response = matchService.createBotMatch(
                    player.getId(), request.getDeckId(), request.getBotDifficulty());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }
        Match match = matchService.createMatch(
                player.getId(), request.getDeckId(),
                request.getRoomName(), request.getDescription(),
                request.isPrivate(), request.isShowInList(),
                request.getPassword(), request.isAllowSpectators());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(matchService.toCreateMatchResponse(match));
    }

    @Operation(summary = "Join a match by code",
               description = "The second player joins an existing waiting match using a shareable code.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Joined successfully",
            content = @Content(schema = @Schema(implementation = CreateMatchResponse.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated",
            content = @Content(schema = @Schema(implementation = ErrorApi.class))),
        @ApiResponse(responseCode = "404", description = "Room not found",
            content = @Content(schema = @Schema(implementation = ErrorApi.class))),
        @ApiResponse(responseCode = "409", description = "Room is full or not open",
            content = @Content(schema = @Schema(implementation = ErrorApi.class)))
    })
    @PostMapping("/join-by-code")
    public ResponseEntity<CreateMatchResponse> joinByCode(
            @Authenticated Player player,
            @RequestBody JoinByCodeRequest request) {
        CreateMatchResponse response = matchService.joinByCode(
                request.getCode(), player.getId(), request.getDeckId(), request.getPassword());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Join a match",
               description = "The second player joins an existing waiting match with a chosen deck. "
                           + "If the deck is valid the match transitions to SETUP.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Joined successfully",
            content = @Content(schema = @Schema(implementation = CreateMatchResponse.class))),
        @ApiResponse(responseCode = "400", description = "Submitted deck is invalid",
            content = @Content(schema = @Schema(implementation = ErrorApi.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated",
            content = @Content(schema = @Schema(implementation = ErrorApi.class))),
        @ApiResponse(responseCode = "404", description = "Match, player or deck not found",
            content = @Content(schema = @Schema(implementation = ErrorApi.class))),
        @ApiResponse(responseCode = "409", description = "Match is not open to join",
            content = @Content(schema = @Schema(implementation = ErrorApi.class)))
    })
    @PostMapping("/{matchId}/join")
    public ResponseEntity<CreateMatchResponse> joinMatch(
            @Authenticated Player player,
            @Parameter(description = "Identifier of the match to join")
            @PathVariable String matchId,
            @RequestBody JoinMatchRequest request) {
        CreateMatchResponse response = matchService.toCreateMatchResponse(
                matchService.joinMatch(matchId, player.getId(), request.getDeckId(), request.getPassword()));
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get match state",
               description = "Returns the match state projected for the calling player. "
                           + "The opponent's hand cards and deck order are never revealed.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Match state retrieved",
            content = @Content(schema = @Schema(implementation = MatchStateDTO.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated",
            content = @Content(schema = @Schema(implementation = ErrorApi.class))),
        @ApiResponse(responseCode = "403", description = "Player does not belong to this match",
            content = @Content(schema = @Schema(implementation = ErrorApi.class))),
        @ApiResponse(responseCode = "404", description = "Match not found or not started",
            content = @Content(schema = @Schema(implementation = ErrorApi.class)))
    })
    @GetMapping("/{matchId}/state")
    public ResponseEntity<MatchStateDTO> getMatchState(
            @Authenticated Player player,
            @Parameter(description = "Identifier of the match")
            @PathVariable String matchId) {
        MatchStateDTO state = matchService.getMatchState(matchId, player.getId());
        return ResponseEntity.ok(state);
    }

    @Operation(summary = "Get lobby state",
               description = "Returns the lobby-level state (ready flags, guest info) "
                           + "for use by the waiting room. Does not require the match "
                           + "to be in an active game phase.")
    @GetMapping("/{matchId}/lobby-state")
    public ResponseEntity<CreateMatchResponse> getLobbyState(
            @Authenticated Player player,
            @Parameter(description = "Identifier of the match")
            @PathVariable String matchId) {
        return ResponseEntity.ok(
                matchService.getLobbyState(matchId, player.getId()));
    }

    @Operation(summary = "Reconnect to an active match",
               description = "Returns the current match state for a player reconnecting after a disconnect.")
    @GetMapping("/{matchId}/reconnect")
    public ResponseEntity<MatchStateDTO> reconnect(
            @Authenticated Player player,
            @PathVariable String matchId) {
        MatchStateDTO state = matchService.reconnectPlayer(matchId, player.getId());
        return ResponseEntity.ok(state);
    }

    @Operation(summary = "Check if the player recently lost by abandonment",
               description = "Returns the most recent match the authenticated player lost by "
                           + "disconnecting and failing to reconnect within the allowed window.")
    @GetMapping("/me/abandoned-result")
    public ResponseEntity<AbandonedResultResponse> abandonedResult(
            @Authenticated Player player) {
        String matchId = matchService.getRecentAbandonment(player.getId());
        boolean abandoned = matchId != null;
        return ResponseEntity.ok(new AbandonedResultResponse(matchId, abandoned));
    }

    @Operation(summary = "Get pending matches for the authenticated player",
               description = "Returns active matches where the player still has a pending session.")
    @GetMapping("/pending-matches")
    public ResponseEntity<List<CreateMatchResponse>> getPendingMatches(
            @Authenticated Player player) {
        return ResponseEntity.ok(matchService.getPendingMatches(player.getId()));
    }

    @Operation(summary = "Set player ready",
               description = "Toggles whether the authenticated player is ready in the lobby.")
    @PostMapping("/{matchId}/ready")
    public ResponseEntity<CreateMatchResponse> setReady(
            @Authenticated Player player,
            @Parameter(description = "Identifier of the match")
            @PathVariable String matchId,
            @RequestBody SetReadyRequest request) {
        CreateMatchResponse response = matchService.setReady(matchId, player.getId(), request.isReady());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Start a match",
               description = "Transitions the match from WAITING to SETUP. "
                           + "Only the host can start and both players must be ready.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Match started"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403", description = "Only the host can start"),
        @ApiResponse(responseCode = "409", description = "Both players must be ready")
    })
    @PostMapping("/{matchId}/start")
    public ResponseEntity<CreateMatchResponse> startMatch(
            @Authenticated Player player,
            @Parameter(description = "Identifier of the match to start")
            @PathVariable String matchId) {
        return ResponseEntity.ok(matchService.startMatch(matchId, player.getId()));
    }

    @PostMapping("/{matchId}/opening/coin-side")
    public ResponseEntity<MatchStateDTO> chooseCoinSide(
            @CookieValue("sessionId") String sessionId,
            @PathVariable String matchId,
            @RequestBody ChooseCoinSideRequest request, 
            @Authenticated Player player) {
        return ResponseEntity.ok(matchService.chooseCoinSide(matchId, player.getId(), request.getCoinSide()));
    }

    @PostMapping("/{matchId}/opening/starter")
    public ResponseEntity<MatchStateDTO> chooseFirstTurn(
            @CookieValue("sessionId") String sessionId,
            @PathVariable String matchId,
            @RequestBody ChooseFirstTurnRequest request, 
            @Authenticated Player player) {
        return ResponseEntity.ok(matchService.chooseFirstTurn(matchId, player.getId(), request.getStarterPlayerId()));
    }

    @PostMapping("/{matchId}/coin-toss")
    public ResponseEntity<MatchStateDTO> chooseCoinSideFlat(
            @CookieValue("sessionId") String sessionId,
            @PathVariable String matchId,
            @RequestBody ChooseCoinSideRequest request, 
            @Authenticated Player player) {
        return chooseCoinSide(sessionId, matchId, request, player);
    }

    @PostMapping("/{matchId}/choose-starter")
    public ResponseEntity<MatchStateDTO> chooseFirstTurnFlat(
            @CookieValue("sessionId") String sessionId,
            @PathVariable String matchId,
            @RequestBody ChooseFirstTurnRequest request, 
            @Authenticated Player player) {
        return chooseFirstTurn(sessionId, matchId, request, player);
    }

    /**
     * Cancels or leaves a match in the waiting room.
     * <ul>
     *   <li>If the host cancels, the match is deleted entirely.</li>
     *   <li>If the guest leaves, the guest slot is cleared.</li>
     * </ul>
     *
     * @param sessionId session cookie of the requesting player
     * @param matchId   identifier of the match to cancel
     * @return 204 No Content
     */
    @Operation(summary = "Cancel or leave a match",
               description = "Host: deletes the match. Guest: clears the guest slot.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Cancelled or left successfully"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403", description = "Player does not belong to this match"),
        @ApiResponse(responseCode = "404", description = "Match not found")
    })
    @PostMapping("/{matchId}/cancel")
    public ResponseEntity<Void> cancelMatch(
            @Authenticated Player player,
            @Parameter(description = "Identifier of the match")
            @PathVariable String matchId) {
        matchService.cancelMatch(matchId, player.getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Submit a match action",
               description = "The player submits an action (type and parameters). "
                           + "Delegates to the battle engine for rule enforcement and state transition.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Action applied; updated state returned",
            content = @Content(schema = @Schema(implementation = MatchStateDTO.class))),
        @ApiResponse(responseCode = "400", description = "Illegal action",
            content = @Content(schema = @Schema(implementation = ErrorApi.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated",
            content = @Content(schema = @Schema(implementation = ErrorApi.class))),
        @ApiResponse(responseCode = "403", description = "Player does not belong to this match",
            content = @Content(schema = @Schema(implementation = ErrorApi.class))),
        @ApiResponse(responseCode = "404", description = "Match not found or not started",
            content = @Content(schema = @Schema(implementation = ErrorApi.class)))
    })
    @PostMapping("/{matchId}/actions")
    public ResponseEntity<MatchStateDTO> submitAction(
            @Authenticated Player player,
            @Parameter(description = "Identifier of the match")
            @PathVariable String matchId,
            @RequestBody GameAction action) {
        matchService.submitAction(matchId, player.getId(), action);
        MatchStateDTO state = matchService.getMatchState(matchId, player.getId());
        return ResponseEntity.ok(state);
    }

    @PostMapping("/{matchId}/start-countdown")
    public ResponseEntity<Void> startCountdown(
            @Authenticated Player player,
            @PathVariable String matchId,
            @RequestBody Map<String, Object> body) {
        String phase = (String) body.get("phase");
        int totalSeconds = (int) body.get("totalSeconds");
        matchService.startCountdown(matchId, player.getId(), phase, totalSeconds);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{matchId}/cancel-countdown")
    public ResponseEntity<Void> cancelCountdown(
            @Authenticated Player player,
            @PathVariable String matchId) {
        matchService.cancelCountdown(matchId, player.getId());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Surrender an active match",
               description = "The player forfeits the match. The opponent wins.")
    @PostMapping("/{matchId}/surrender")
    public ResponseEntity<Void> surrender(
            @Authenticated Player player,
            @PathVariable String matchId) {
        matchService.surrender(matchId, player.getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Admin: clean up orphan matches",
               description = "Removes stale WAITING/SETUP/ACTIVE matches and orphaned battles. "
                           + "Intended for manual maintenance.")
    @PostMapping("/admin/cleanup")
    public ResponseEntity<Void> adminCleanup() {
        matchService.cleanupOrphanRooms();
        matchService.cleanupOrphanBattles();
        return ResponseEntity.ok().build();
    }
}
