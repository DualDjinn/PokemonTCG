package ar.edu.utn.frc.tup.piii.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frc.tup.piii.configs.Authenticated;
import ar.edu.utn.frc.tup.piii.dtos.common.ErrorApi;
import ar.edu.utn.frc.tup.piii.dtos.skin.CardSkinDTO;
import ar.edu.utn.frc.tup.piii.dtos.skin.EquipSkinRequest;
import ar.edu.utn.frc.tup.piii.dtos.skin.PlayerSkinDTO;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.services.SkinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * Endpoints para skins de cartas.
 */
@RestController
@RequestMapping("/api/skins")
public class SkinController {

    private final SkinService skinService;

    public SkinController(SkinService skinService) {
        this.skinService = skinService;
    }

    @Operation(summary = "List all skins",
               description = "Returns the full catalog of available card skins.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Skin catalog",
        content = @Content(array = @ArraySchema(
            schema = @Schema(implementation = CardSkinDTO.class)))))
    @GetMapping
    public ResponseEntity<List<CardSkinDTO>> getAllSkins() {
        return ResponseEntity.ok(skinService.getAllSkins());
    }

    @Operation(summary = "List skins for a card",
               description = "Returns all skins applicable to the given base card.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Skins for card",
        content = @Content(array = @ArraySchema(
            schema = @Schema(implementation = CardSkinDTO.class)))))
    @GetMapping("/card/{cardId}")
    public ResponseEntity<List<CardSkinDTO>> getSkinsForCard(@PathVariable String cardId) {
        return ResponseEntity.ok(skinService.getSkinsForCard(cardId));
    }

    @Operation(summary = "List player's skins",
               description = "Returns all skins owned by the authenticated player.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Player skins",
        content = @Content(array = @ArraySchema(
            schema = @Schema(implementation = PlayerSkinDTO.class)))))
    @GetMapping("/me")
    public ResponseEntity<List<PlayerSkinDTO>> getPlayerSkins(@Authenticated Player player) {
        return ResponseEntity.ok(skinService.getPlayerSkins(player.getId()));
    }

    @Operation(summary = "Equip a skin",
               description = "Equips a skin on a card in the player's collection.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Skin equipped"),
        @ApiResponse(responseCode = "400", description = "Invalid card or skin",
            content = @Content(schema = @Schema(implementation = ErrorApi.class))),
        @ApiResponse(responseCode = "409", description = "Player does not own the skin",
            content = @Content(schema = @Schema(implementation = ErrorApi.class)))
    })
    @PostMapping("/me/equip")
    public ResponseEntity<?> equipSkin(@Authenticated Player player,
                                       @RequestBody EquipSkinRequest request) {
        try {
            skinService.equipSkin(player.getId(), request.getCardId(), request.getSkinId());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(buildError(HttpStatus.BAD_REQUEST, e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(buildError(HttpStatus.CONFLICT, e.getMessage()));
        }
    }

    @Operation(summary = "Unequip a skin",
               description = "Removes the active skin from a card, reverting to default art.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Skin unequipped"),
        @ApiResponse(responseCode = "400", description = "Player does not own the card",
            content = @Content(schema = @Schema(implementation = ErrorApi.class)))
    })
    @PostMapping("/me/unequip")
    public ResponseEntity<?> unequipSkin(@Authenticated Player player,
                                         @RequestBody EquipSkinRequest request) {
        try {
            skinService.unequipSkin(player.getId(), request.getCardId());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(buildError(HttpStatus.BAD_REQUEST, e.getMessage()));
        }
    }

    private ErrorApi buildError(HttpStatus status, String message) {
        return ErrorApi.builder()
                .timestamp(java.time.LocalDateTime.now().toString())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .build();
    }
}
