package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.common.ErrorApi;
import ar.edu.utn.frc.tup.piii.dtos.front.CardDTO;
import ar.edu.utn.frc.tup.piii.services.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller that exposes read-only endpoints for querying
 * Pokémon TCG card data from the local XY1 set (db.json).
 *
 * <p>All endpoints return {@link CardDTO} objects. Filtering is
 * done server-side through optional query parameters.</p>
 */
@RestController
@RequestMapping("/cards")
@Tag(name = "Cards", description = "Read-only endpoints for XY1 Pokémon TCG card data")
public class CardController {

    /** Service that provides card data and filtering logic. */
    private final CardService cardService;

    /**
     * Constructs the controller with the required card service.
     *
     * @param cardService service layer that holds the in-memory card list
     */
    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    /**
     * Returns all XY1 cards, optionally narrowed by one or more query parameters.
     * When no parameters are provided every card in the database is returned.
     * Multiple parameters are combined with logical AND.
     *
     * @param name      partial, case-insensitive name filter (e.g., {@code "saur"})
     * @param supertype supertype filter — {@code "Pokemon"}, {@code "Trainer"},
     *                  or {@code "Energy"} (accent-insensitive)
     * @param type      energy type filter for Pokémon cards (e.g., {@code "Grass"})
     * @param rarity    rarity filter (e.g., {@code "Rare Holo EX"})
     * @param subtype   subtype filter (e.g., {@code "Basic"}, {@code "EX"}, {@code "Item"})
     * @return 200 OK with the matching card list (may be empty)
     */
    @Operation(
        summary = "Get all cards",
        description = "Returns XY1 cards. All query parameters are optional and combined with AND."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = @Content(
                array = @ArraySchema(schema = @Schema(implementation = CardDTO.class))
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal Server Error",
            content = @Content(schema = @Schema(implementation = ErrorApi.class))
        )
    })
    @GetMapping
    public ResponseEntity<List<CardDTO>> getCards(
        @Parameter(description = "Partial name filter, case-insensitive (e.g. saur)")
        @RequestParam(required = false) String name,
        @Parameter(description = "Supertype filter: Pokemon, Trainer or Energy")
        @RequestParam(required = false) String supertype,
        @Parameter(description = "Energy type for Pokémon cards, e.g. Grass, Fire, Water")
        @RequestParam(required = false) String type,
        @Parameter(description = "Rarity filter, e.g. Common, Rare Holo EX")
        @RequestParam(required = false) String rarity,
        @Parameter(description = "Subtype filter, e.g. Basic, Stage 1, EX, Item, Supporter")
        @RequestParam(required = false) String subtype) {

        boolean hasFilters = name != null || supertype != null
            || type != null || rarity != null || subtype != null;

        List<CardDTO> result = hasFilters
            ? cardService.findByFilters(name, supertype, type, rarity, subtype)
            : cardService.findAll();

        return ResponseEntity.ok(result);
    }

    /**
     * Returns a single card identified by its unique ID (e.g., {@code xy1-1}).
     *
     * @param id the card identifier to look up
     * @return 200 OK with the card, or 404 Not Found if no card matches
     */
    @Operation(
        summary = "Get card by ID",
        description = "Returns a single XY1 card identified by its unique ID, e.g. xy1-1."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Card found",
            content = @Content(schema = @Schema(implementation = CardDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Card not found",
            content = @Content(schema = @Schema(implementation = ErrorApi.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal Server Error",
            content = @Content(schema = @Schema(implementation = ErrorApi.class))
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<CardDTO> getCardById(
        @Parameter(description = "Card identifier, e.g. xy1-1")
        @PathVariable String id) {

        Optional<CardDTO> card = cardService.findById(id);
        return card.map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
