package ar.edu.utn.frc.tup.piii.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frc.tup.piii.configs.Authenticated;
import ar.edu.utn.frc.tup.piii.dtos.common.ErrorApi;
import ar.edu.utn.frc.tup.piii.dtos.player.CreateDeckRequest;
import ar.edu.utn.frc.tup.piii.dtos.player.DeckDTO;
import ar.edu.utn.frc.tup.piii.dtos.player.DeckValidationResult;
import ar.edu.utn.frc.tup.piii.dtos.player.UpdateDeckNameRequest;
import ar.edu.utn.frc.tup.piii.dtos.player.UpdateDeckRequest;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.services.DeckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/decks")
public class DeckController {

    private final DeckService deckService;

    public DeckController(DeckService deckService) {
        this.deckService = deckService;
    }

    @Operation(summary = "List all decks",
               description = "Returns all decks of the authenticated player with name, card count and validity")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Decks retrieved",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = DeckDTO.class)))),
        @ApiResponse(responseCode = "401", description = "Not authenticated",
            content = @Content(schema = @Schema(implementation = ErrorApi.class)))
    })
    @GetMapping
    public ResponseEntity<List<DeckDTO>> listDecks(@Authenticated Player player) {
        List<DeckDTO> decks = deckService.getPlayerDecks(player.getId());
        return ResponseEntity.ok(decks);
    }

    @Operation(summary = "Get deck by ID",
               description = "Returns a single deck with full card details for the authenticated player")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Deck retrieved",
            content = @Content(schema = @Schema(implementation = DeckDTO.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated",
            content = @Content(schema = @Schema(implementation = ErrorApi.class))),
        @ApiResponse(responseCode = "404", description = "Deck not found",
            content = @Content(schema = @Schema(implementation = ErrorApi.class)))
    })
    @GetMapping("/{deckId}")
    public ResponseEntity<DeckDTO> getDeck(@Authenticated Player player,
                                           @PathVariable String deckId) {
        DeckDTO dto = deckService.getDeck(player.getId(), deckId);
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Update deck cards",
               description = "Replaces the cards in a deck with the given list of card IDs. "
                           + "The deck is re-validated after the update.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Deck updated",
            content = @Content(schema = @Schema(implementation = DeckDTO.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated",
            content = @Content(schema = @Schema(implementation = ErrorApi.class))),
        @ApiResponse(responseCode = "404", description = "Deck or card not found",
            content = @Content(schema = @Schema(implementation = ErrorApi.class)))
    })
    @PutMapping("/{deckId}")
    public ResponseEntity<DeckDTO> updateDeck(@Authenticated Player player,
                                              @PathVariable String deckId,
                                              @RequestBody UpdateDeckRequest request) {
        DeckDTO dto = deckService.updateDeckCards(player.getId(), deckId, request.getCardIds());
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Create a new deck",
               description = "Creates an empty deck with the given name for the authenticated player.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Deck created",
            content = @Content(schema = @Schema(implementation = DeckDTO.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated",
            content = @Content(schema = @Schema(implementation = ErrorApi.class)))
    })
    @PostMapping
    public ResponseEntity<DeckDTO> createDeck(@Authenticated Player player,
                                              @RequestBody CreateDeckRequest request) {
        DeckDTO dto = deckService.createDeck(player.getId(), request.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @Operation(summary = "Delete a deck",
               description = "Permanently removes a deck owned by the authenticated player.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Deck deleted"),
        @ApiResponse(responseCode = "401", description = "Not authenticated",
            content = @Content(schema = @Schema(implementation = ErrorApi.class))),
        @ApiResponse(responseCode = "404", description = "Deck not found",
            content = @Content(schema = @Schema(implementation = ErrorApi.class)))
    })
    @DeleteMapping("/{deckId}")
    public ResponseEntity<Void> deleteDeck(@Authenticated Player player,
                                           @PathVariable String deckId) {
        deckService.deleteDeck(player.getId(), deckId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Rename a deck",
               description = "Updates the name of an existing deck owned by the authenticated player.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Deck renamed",
            content = @Content(schema = @Schema(implementation = DeckDTO.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated",
            content = @Content(schema = @Schema(implementation = ErrorApi.class))),
        @ApiResponse(responseCode = "404", description = "Deck not found",
            content = @Content(schema = @Schema(implementation = ErrorApi.class)))
    })
    @PatchMapping("/{deckId}/name")
    public ResponseEntity<DeckDTO> renameDeck(@Authenticated Player player,
                                              @PathVariable String deckId,
                                              @RequestBody UpdateDeckNameRequest request) {
        DeckDTO dto = deckService.renameDeck(player.getId(), deckId, request.getName());
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Toggle deck favorite",
               description = "Toggles the favorite status of a deck owned by the authenticated player.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Favorite status toggled",
            content = @Content(schema = @Schema(implementation = DeckDTO.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated",
            content = @Content(schema = @Schema(implementation = ErrorApi.class))),
        @ApiResponse(responseCode = "404", description = "Deck not found",
            content = @Content(schema = @Schema(implementation = ErrorApi.class)))
    })
    @PutMapping("/{deckId}/favorite")
    public ResponseEntity<DeckDTO> toggleFavorite(@Authenticated Player player,
                                                  @PathVariable String deckId) {
        DeckDTO dto = deckService.toggleDeckFavorite(player.getId(), deckId);
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Validate a deck",
               description = "Runs validation rules on the deck (60 cards, >=1 Basic, <=4 copies) "
                           + "and returns valid flag with error messages if any.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Validation result",
            content = @Content(schema = @Schema(implementation = DeckDTO.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated",
            content = @Content(schema = @Schema(implementation = ErrorApi.class))),
        @ApiResponse(responseCode = "404", description = "Deck not found",
            content = @Content(schema = @Schema(implementation = ErrorApi.class)))
    })
    @GetMapping("/{deckId}/validate")
    public ResponseEntity<DeckValidationResult> validateDeck(@Authenticated Player player,
                                                             @PathVariable String deckId) {
        DeckValidationResult result = deckService.validateDeck(player.getId(), deckId);
        return ResponseEntity.ok(result);
    }
}
