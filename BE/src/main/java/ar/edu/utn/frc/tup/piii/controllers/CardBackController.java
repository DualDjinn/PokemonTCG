package ar.edu.utn.frc.tup.piii.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frc.tup.piii.configs.Authenticated;
import ar.edu.utn.frc.tup.piii.dtos.cardback.CardBackDTO;
import ar.edu.utn.frc.tup.piii.dtos.cardback.EquipCardBackRequest;
import ar.edu.utn.frc.tup.piii.dtos.cardback.PlayerCardBacksDTO;
import ar.edu.utn.frc.tup.piii.dtos.cardback.PurchaseCardBackRequest;
import ar.edu.utn.frc.tup.piii.dtos.common.ErrorApi;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.services.CardBackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * Endpoints de dorsos de carta cosméticos.
 */
@RestController
@RequestMapping("/api/card-backs")
public class CardBackController {

    private final CardBackService cardBackService;

    public CardBackController(CardBackService cardBackService) {
        this.cardBackService = cardBackService;
    }

    @Operation(summary = "List card back catalog",
               description = "Returns every cosmetic card back with its origin, "
                           + "price (shop) or unlock requirement (battle/level).")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Card back catalog",
        content = @Content(array = @ArraySchema(schema = @Schema(implementation = CardBackDTO.class)))))
    @GetMapping
    public ResponseEntity<List<CardBackDTO>> getCatalog() {
        return ResponseEntity.ok(cardBackService.getCatalog());
    }

    @Operation(summary = "List player's card backs",
               description = "Returns the card backs the authenticated player owns "
                           + "and which one is equipped (null = default back).")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Owned card backs",
            content = @Content(schema = @Schema(implementation = PlayerCardBacksDTO.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated",
            content = @Content(schema = @Schema(implementation = ErrorApi.class)))
    })
    @GetMapping("/me")
    public ResponseEntity<PlayerCardBacksDTO> getPlayerBacks(@Authenticated Player player) {
        return ResponseEntity.ok(cardBackService.getPlayerBacks(player.getId()));
    }

    @Operation(summary = "Purchase a card back",
               description = "Buys a SHOP card back with coins and grants it to "
                           + "the authenticated player.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Purchased; returns updated ownership",
            content = @Content(schema = @Schema(implementation = PlayerCardBacksDTO.class))),
        @ApiResponse(responseCode = "400", description = "Unknown card back",
            content = @Content(schema = @Schema(implementation = ErrorApi.class))),
        @ApiResponse(responseCode = "409", description = "Not for sale, already owned or insufficient coins",
            content = @Content(schema = @Schema(implementation = ErrorApi.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated",
            content = @Content(schema = @Schema(implementation = ErrorApi.class)))
    })
    @PostMapping("/me/purchase")
    public ResponseEntity<PlayerCardBacksDTO> purchase(
            @Authenticated Player player,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody PurchaseCardBackRequest request) {
        return ResponseEntity.ok(
                cardBackService.purchase(player.getId(), request.getCardBackId(), idempotencyKey));
    }

    @Operation(summary = "Equip a card back",
               description = "Sets the player's global card back. Send cardBackId "
                           + "null to restore the default back.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Equipped; returns updated ownership",
            content = @Content(schema = @Schema(implementation = PlayerCardBacksDTO.class))),
        @ApiResponse(responseCode = "409", description = "Player does not own the card back",
            content = @Content(schema = @Schema(implementation = ErrorApi.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated",
            content = @Content(schema = @Schema(implementation = ErrorApi.class)))
    })
    @PutMapping("/me/active")
    public ResponseEntity<PlayerCardBacksDTO> equip(
            @Authenticated Player player,
            @RequestBody EquipCardBackRequest request) {
        return ResponseEntity.ok(cardBackService.equip(player.getId(), request.getCardBackId()));
    }
}
