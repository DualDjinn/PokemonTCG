package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.configs.Authenticated;
import ar.edu.utn.frc.tup.piii.dtos.common.ErrorApi;
import ar.edu.utn.frc.tup.piii.dtos.shop.BuyCartRequest;
import ar.edu.utn.frc.tup.piii.dtos.shop.BuyCartResponse;
import ar.edu.utn.frc.tup.piii.dtos.shop.BuySplashArtsRequest;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.services.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tienda de cartas individuales: compra con coins del jugador sin pasarela de pago externa.
 */
@RestController
@RequestMapping("/api/shop")
public class ShopController {

    private final ShopService shopService;

    public ShopController(ShopService shopService) {
        this.shopService = shopService;
    }

    @Operation(
        summary = "Buy cards with coins",
        description = "Deducts the total coin cost from the authenticated player's balance "
                    + "and adds the purchased cards to their collection."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Purchase successful",
            content = @Content(schema = @Schema(implementation = BuyCartResponse.class))),
        @ApiResponse(responseCode = "402", description = "Insufficient coins",
            content = @Content(schema = @Schema(implementation = ErrorApi.class))),
        @ApiResponse(responseCode = "404", description = "Card not found",
            content = @Content(schema = @Schema(implementation = ErrorApi.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated",
            content = @Content(schema = @Schema(implementation = ErrorApi.class)))
    })
    @PostMapping("/buy")
    public ResponseEntity<?> buyCards(
            @Authenticated Player player,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody BuyCartRequest request) {
        try {
            BuyCartResponse response = shopService.buyCards(player.getId(), request, idempotencyKey);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(buildError(HttpStatus.NOT_FOUND, e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                    .body(buildError(HttpStatus.PAYMENT_REQUIRED, e.getMessage()));
        }
    }

    @Operation(
        summary = "Buy splash arts with coins",
        description = "Deducts the coin cost of the requested splash arts from the authenticated "
                    + "player's balance and persists the new balance."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Purchase successful",
            content = @Content(schema = @Schema(implementation = BuyCartResponse.class))),
        @ApiResponse(responseCode = "402", description = "Insufficient coins",
            content = @Content(schema = @Schema(implementation = ErrorApi.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated",
            content = @Content(schema = @Schema(implementation = ErrorApi.class)))
    })
    @PostMapping("/splash-arts/buy")
    public ResponseEntity<?> buySplashArts(
            @Authenticated Player player,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody BuySplashArtsRequest request) {
        try {
            BuyCartResponse response = shopService.buySplashArts(player.getId(), request, idempotencyKey);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(buildError(HttpStatus.NOT_FOUND, e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                    .body(buildError(HttpStatus.PAYMENT_REQUIRED, e.getMessage()));
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
