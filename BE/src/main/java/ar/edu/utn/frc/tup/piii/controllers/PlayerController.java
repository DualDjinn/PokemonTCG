package ar.edu.utn.frc.tup.piii.controllers;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ar.edu.utn.frc.tup.piii.configs.Authenticated;
import ar.edu.utn.frc.tup.piii.dtos.player.AddCardRequest;
import ar.edu.utn.frc.tup.piii.dtos.player.CardSummaryDTO;
import ar.edu.utn.frc.tup.piii.dtos.player.DeckDTO;
import ar.edu.utn.frc.tup.piii.dtos.player.LoginRequest;
import ar.edu.utn.frc.tup.piii.dtos.player.PlayerRequest;
import ar.edu.utn.frc.tup.piii.dtos.player.PlayerResponse;
import ar.edu.utn.frc.tup.piii.dtos.player.UpdateCharacterRequest;
import ar.edu.utn.frc.tup.piii.dtos.player.UpdateTrainerNameRequest;
import ar.edu.utn.frc.tup.piii.dtos.player.ClaimStarterDeckRequest;
import ar.edu.utn.frc.tup.piii.dtos.player.ForgotPasswordRequest;
import ar.edu.utn.frc.tup.piii.dtos.player.GoogleLoginRequest;
import ar.edu.utn.frc.tup.piii.dtos.player.ResetPasswordRequest;
import ar.edu.utn.frc.tup.piii.dtos.common.ErrorApi;
import java.util.Map;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.services.PlayerService;
import ar.edu.utn.frc.tup.piii.services.DeckService;
import ar.edu.utn.frc.tup.piii.services.CollectionService;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private static final Logger logger = LoggerFactory.getLogger(PlayerController.class);

    private final PlayerService playerService;
    private final DeckService deckService;
    private final CollectionService collectionService;

    public PlayerController(PlayerService playerService,
                            DeckService deckService,
                            CollectionService collectionService) {
        this.playerService = playerService;
        this.deckService = deckService;
        this.collectionService = collectionService;
    }

    @Operation(summary = "Register a new player",
               description = "Creates a new player account with 3 initial starter decks")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Player registered successfully",
            content = @Content(schema = @Schema(implementation = PlayerResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input or username already exists",
            content = @Content(schema = @Schema(implementation = ErrorApi.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<PlayerResponse> register(@Valid @RequestBody PlayerRequest request) {
        PlayerResponse response = playerService.register(request);
        String sessionId = playerService.createSession(response.getId());
        ResponseCookie sessionCookie = ResponseCookie.from("sessionId", sessionId)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(86400)
                .build();
        return ResponseEntity.status(201)
                .header(HttpHeaders.SET_COOKIE, sessionCookie.toString())
                .body(response);
    }

    @Operation(summary = "Login a player",
               description = "Authenticates a player and returns a session cookie")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login successful",
            content = @Content(schema = @Schema(implementation = PlayerResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid credentials",
            content = @Content(schema = @Schema(implementation = ErrorApi.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<PlayerResponse> login(@RequestBody LoginRequest request) {
        PlayerResponse response = playerService.login(request);
        String sessionId = playerService.createSession(response.getId());
        ResponseCookie sessionCookie = ResponseCookie.from("sessionId", sessionId)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(86400)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionCookie.toString())
                .body(response);
    }

    @Operation(summary = "Update trainer name",
               description = "Updates the trainer name of the authenticated player")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Trainer name updated",
            content = @Content(schema = @Schema(implementation = PlayerResponse.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated",
            content = @Content(schema = @Schema(implementation = ErrorApi.class)))
    })
    @PutMapping("/trainer-name")
    public ResponseEntity<PlayerResponse> updateTrainerName(
            @Authenticated Player player,
            @RequestBody UpdateTrainerNameRequest request) {
        PlayerResponse response = playerService.updateTrainerName(player.getId(), request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update player character",
               description = "Updates the selected character (e.g. red, green, ruby) and optional color variant for the authenticated player")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Character updated",
            content = @Content(schema = @Schema(implementation = PlayerResponse.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated",
            content = @Content(schema = @Schema(implementation = ErrorApi.class)))
    })
    @PutMapping("/character")
    public ResponseEntity<PlayerResponse> updateCharacter(
            @Authenticated Player player,
            @RequestBody UpdateCharacterRequest request) {
        PlayerResponse response = playerService.updateCharacter(player.getId(), request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Set active deck for the player",
               description = "Sets the active deck for the authenticated player. Pass null to clear the selection.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Active deck updated",
            content = @Content(schema = @Schema(implementation = PlayerResponse.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated",
            content = @Content(schema = @Schema(implementation = ErrorApi.class))),
        @ApiResponse(responseCode = "404", description = "Deck not found",
            content = @Content(schema = @Schema(implementation = ErrorApi.class)))
    })
    @PutMapping("/me/active-deck")
    public ResponseEntity<PlayerResponse> setActiveDeck(
            @Authenticated Player player,
            @RequestBody Map<String, String> body) {
        PlayerResponse response = playerService.setActiveDeck(player.getId(), body.get("deckId"));
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get current player data",
               description = "Returns the full player data for the authenticated session")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Player data retrieved",
            content = @Content(schema = @Schema(implementation = PlayerResponse.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated",
            content = @Content(schema = @Schema(implementation = ErrorApi.class)))
    })
    @GetMapping("/me")
    public ResponseEntity<PlayerResponse> getCurrentPlayer(@Authenticated Player player) {
        PlayerResponse response = playerService.getPlayerById(player.getId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Claim starter deck",
               description = "Grants the chosen starter deck after the tutorial. Can only be used once.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Deck granted",
            content = @Content(schema = @Schema(implementation = PlayerResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid deck name",
            content = @Content(schema = @Schema(implementation = ErrorApi.class))),
        @ApiResponse(responseCode = "409", description = "Starter deck already claimed",
            content = @Content(schema = @Schema(implementation = ErrorApi.class)))
    })
    @PostMapping("/me/starter-deck")
    public ResponseEntity<PlayerResponse> claimStarterDeck(
            @Authenticated Player player,
            @Valid @RequestBody ClaimStarterDeckRequest request) {
        PlayerResponse response = playerService.claimStarterDeck(player.getId(), request.getDeckName());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Logout current player",
               description = "Invalidates the current session and clears the session cookie")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Logged out successfully"),
        @ApiResponse(responseCode = "401", description = "Not authenticated",
            content = @Content(schema = @Schema(implementation = ErrorApi.class)))
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue("sessionId") String sessionId) {
        playerService.removeSession(sessionId);
        ResponseCookie sessionCookie = ResponseCookie.from("sessionId", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionCookie.toString())
                .build();
    }

    @Operation(summary = "Get current player decks with cards",
               description = "Returns all decks of the authenticated player with full card details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Decks retrieved",
            content = @Content(schema = @Schema(implementation = DeckDTO.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated",
            content = @Content(schema = @Schema(implementation = ErrorApi.class)))
    })
    @GetMapping("/me/decks")
    public ResponseEntity<List<DeckDTO>> getCurrentPlayerDecks(@Authenticated Player player) {
        List<DeckDTO> decks = deckService.getPlayerDecks(player.getId());
        return ResponseEntity.ok(decks);
    }

    @Operation(summary = "Get current player collection",
               description = "Returns the collection cards of the authenticated player with pagination")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Collection retrieved",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = CardSummaryDTO.class)))),
        @ApiResponse(responseCode = "401", description = "Not authenticated",
            content = @Content(schema = @Schema(implementation = ErrorApi.class)))
    })
    @GetMapping("/me/collection")
    public ResponseEntity<List<CardSummaryDTO>> getCurrentPlayerCollection(
            @Authenticated Player player,
            @Parameter(description = "Page index (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {
        List<CardSummaryDTO> collection = collectionService.getCollection(player.getId(), page, size);
        return ResponseEntity.ok(collection);
    }

    @Operation(summary = "Add card to collection",
               description = "Adds a card to the authenticated player's collection. "
                           + "If the card is already present it is silently ignored.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Card added or already present"),
        @ApiResponse(responseCode = "404", description = "Card not found",
            content = @Content(schema = @Schema(implementation = ErrorApi.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated",
            content = @Content(schema = @Schema(implementation = ErrorApi.class)))
    })
    @PostMapping("/me/collection")
    public ResponseEntity<Void> addToCollection(
            @Authenticated Player player,
            @RequestBody AddCardRequest request) {
        if (!collectionService.cardExists(request.getCardId())) {
            return ResponseEntity.notFound().build();
        }
        collectionService.addToCollection(player.getId(), request.getCardId());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Grant all cards to a player by email",
               description = "Adds every card in the catalog to the specified player's collection with quantity 4.")
    @PostMapping("/grant-all-cards")
    public ResponseEntity<Void> grantAllCards(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        collectionService.grantAllCards(email);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Remove card from collection",
               description = "Removes a card from the authenticated player's collection. "
                           + "If the card is not present it is silently ignored.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Card removed or not present"),
        @ApiResponse(responseCode = "401", description = "Not authenticated",
            content = @Content(schema = @Schema(implementation = ErrorApi.class)))
    })
    @DeleteMapping("/me/collection/{cardId}")
    public ResponseEntity<Void> removeFromCollection(
            @Authenticated Player player,
            @Parameter(description = "Card identifier to remove")
            @PathVariable String cardId) {
        collectionService.removeFromCollection(player.getId(), cardId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Login with Google",
               description = "Validates a Google ID token. Creates the player if it does not exist.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login successful",
            content = @Content(schema = @Schema(implementation = PlayerResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid Google token",
            content = @Content(schema = @Schema(implementation = ErrorApi.class)))
    })
    @PostMapping("/google-login")
    public ResponseEntity<PlayerResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        PlayerResponse response = playerService.googleLogin(request.getCredential());
        String sessionId = playerService.createSession(response.getId());
        ResponseCookie sessionCookie = ResponseCookie.from("sessionId", sessionId)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(86400)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionCookie.toString())
                .body(response);
    }

    @Operation(summary = "Request password reset",
               description = "Sends a password reset email if the address is registered. "
                           + "Always returns 200 to avoid revealing whether an email exists.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Request processed")
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            playerService.requestPasswordReset(request.getEmail());
        } catch (Exception e) {
            logger.error("Password reset request failed for email {}: {}", request.getEmail(), e.getMessage(), e);
        }
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Reset password",
               description = "Validates the reset token and sets the new password")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid or expired token",
            content = @Content(schema = @Schema(implementation = ErrorApi.class)))
    })
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        playerService.confirmPasswordReset(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Request password change for the authenticated player",
               description = "Sends a password reset email to the authenticated player's own "
                           + "registered email, reusing the same token-based flow as forgot-password. "
                           + "The email is taken from the session, never from the request body.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Request processed"),
        @ApiResponse(responseCode = "401", description = "Not authenticated",
            content = @Content(schema = @Schema(implementation = ErrorApi.class)))
    })
    @PostMapping("/me/request-password-change")
    public ResponseEntity<Void> requestPasswordChange(@Authenticated Player player) {
        try {
            playerService.requestPasswordReset(player.getEmail());
        } catch (Exception e) {
            logger.error("Password change request failed for player {}: {}", player.getId(), e.getMessage(), e);
        }
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get player by ID",
               description = "Returns the full player data including collection and decks")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Player data retrieved",
            content = @Content(schema = @Schema(implementation = PlayerResponse.class))),
        @ApiResponse(responseCode = "404", description = "Player not found",
            content = @Content(schema = @Schema(implementation = ErrorApi.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<PlayerResponse> getPlayerById(@PathVariable String id) {
        PlayerResponse response = playerService.getPlayerById(id);
        return ResponseEntity.ok(response);
    }
}
