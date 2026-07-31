package ar.edu.utn.frc.tup.piii.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frc.tup.piii.configs.Authenticated;
import ar.edu.utn.frc.tup.piii.dtos.common.ErrorApi;
import ar.edu.utn.frc.tup.piii.dtos.pack.OpenPackRequest;
import ar.edu.utn.frc.tup.piii.dtos.pack.OpenedPackResponse;
import ar.edu.utn.frc.tup.piii.dtos.pack.PackInfoDTO;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.services.PackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * Tienda de sobres de cartas (NPC Blanca).
 */
@RestController
@RequestMapping("/api/packs")
public class PackController {

    private final PackService packService;

    public PackController(PackService packService) {
        this.packService = packService;
    }

    @Operation(summary = "List available packs",
               description = "Returns the catalog of pack types with their price and rarity probabilities.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pack catalog",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = PackInfoDTO.class))))
    })
    @GetMapping
    public ResponseEntity<List<PackInfoDTO>> listPacks() {
        return ResponseEntity.ok(packService.listPacks());
    }

    @Operation(summary = "Open a pack",
               description = "Charges the pack price to the authenticated player's coins, "
                           + "draws 3 cards according to the pack's rarity distribution, "
                           + "adds them to the player's collection and returns them so the "
                           + "UI can run the reveal animation.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pack opened",
            content = @Content(schema = @Schema(implementation = OpenedPackResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid pack type or missing field",
            content = @Content(schema = @Schema(implementation = ErrorApi.class))),
        @ApiResponse(responseCode = "402", description = "Insufficient coins",
            content = @Content(schema = @Schema(implementation = ErrorApi.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated",
            content = @Content(schema = @Schema(implementation = ErrorApi.class)))
    })
    @PostMapping("/open")
    public ResponseEntity<?> openPack(@Authenticated Player player,
                                      @RequestHeader(value = "Idempotency-Key", required = false)
                                      String idempotencyKey,
                                      @RequestBody OpenPackRequest request) {
        try {
            OpenedPackResponse response =
                    packService.openPack(player.getId(), request.getPackType(), idempotencyKey);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(buildError(HttpStatus.BAD_REQUEST, e.getMessage()));
        } catch (IllegalStateException e) {
            HttpStatus status = "Insufficient coins".equals(e.getMessage())
                    ? HttpStatus.PAYMENT_REQUIRED
                    : HttpStatus.CONFLICT;
            return ResponseEntity.status(status).body(buildError(status, e.getMessage()));
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
