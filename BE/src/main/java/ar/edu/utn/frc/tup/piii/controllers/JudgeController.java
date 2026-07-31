package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.common.ErrorApi;
import ar.edu.utn.frc.tup.piii.dtos.engine.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.judge.JudgeQueryCategory;
import ar.edu.utn.frc.tup.piii.dtos.judge.JudgeQueryDTO;
import ar.edu.utn.frc.tup.piii.dtos.judge.JudgeResponseDTO;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.services.JudgeService;
import ar.edu.utn.frc.tup.piii.services.PlayerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/matches")
@Tag(name = "Judge", description = "Deterministic Judge Assistant — explains engine decisions")
public class JudgeController {

    private final JudgeService judgeService;
    private final PlayerService playerService;

    public JudgeController(JudgeService judgeService, PlayerService playerService) {
        this.judgeService = judgeService;
        this.playerService = playerService;
    }

    @Operation(
            summary = "Ask the Judge",
            description = "Returns a deterministic explanation of any engine decision. "
                    + "All answers derive exclusively from engine-recorded data — no AI involved.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Judge response returned",
            content = @Content(schema = @Schema(implementation = JudgeResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Missing or unsupported query category",
            content = @Content(schema = @Schema(implementation = ErrorApi.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated",
            content = @Content(schema = @Schema(implementation = ErrorApi.class))),
        @ApiResponse(responseCode = "403", description = "Player does not belong to this match",
            content = @Content(schema = @Schema(implementation = ErrorApi.class))),
        @ApiResponse(responseCode = "404", description = "Match not found or not started",
            content = @Content(schema = @Schema(implementation = ErrorApi.class)))
    })
    @GetMapping("/{matchId}/judge/query")
    public ResponseEntity<JudgeResponseDTO> query(
            @CookieValue("sessionId") String sessionId,
            @Parameter(description = "Lobby match identifier")
            @PathVariable String matchId,
            @Parameter(description = "Type of question", required = true)
            @RequestParam JudgeQueryCategory category,
            @Parameter(description = "Action type for WHY_CANT_I queries")
            @RequestParam(required = false) GameActionType targetAction,
            @Parameter(description = "Card ID involved in the action")
            @RequestParam(required = false) String targetCardId,
            @Parameter(description = "In-play Pokémon card ID (for EXPLAIN_STATUS)")
            @RequestParam(required = false) String targetPokemonId,
            @Parameter(description = "Turn number of a past event to explain")
            @RequestParam(required = false) Integer targetTurnNumber,
            @Parameter(description = "Response language: 'es' (default) or 'en'")
            @RequestParam(required = false, defaultValue = "es") String lang) {

        Player player = authenticate(sessionId);

        JudgeQueryDTO query = new JudgeQueryDTO();
        query.setMatchId(matchId);
        query.setPlayerId(player.getId());
        query.setCategory(category);
        query.setTargetAction(targetAction);
        query.setTargetCardId(targetCardId);
        query.setTargetPokemonId(targetPokemonId);
        query.setTargetTurnNumber(targetTurnNumber);
        query.setLang(lang);

        JudgeResponseDTO response = judgeService.resolve(query);
        return ResponseEntity.ok(response);
    }

    private Player authenticate(String sessionId) {
        return playerService.getPlayerBySession(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Not authenticated"));
    }
}
