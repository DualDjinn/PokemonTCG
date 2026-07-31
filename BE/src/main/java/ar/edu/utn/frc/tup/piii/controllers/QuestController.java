package ar.edu.utn.frc.tup.piii.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frc.tup.piii.configs.Authenticated;
import ar.edu.utn.frc.tup.piii.dtos.common.ErrorApi;
import ar.edu.utn.frc.tup.piii.dtos.quest.NpcRewardClaimResponse;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.services.QuestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * Endpoints de sidequests: recompensas que entregan los NPCs del mapa.
 */
@RestController
@RequestMapping("/api/quests")
public class QuestController {

    private final QuestService questService;

    public QuestController(QuestService questService) {
        this.questService = questService;
    }

    @Operation(summary = "List claimed NPC rewards",
               description = "Returns the ids of NPCs whose secret reward the "
                           + "authenticated player has already claimed.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Claimed NPC ids",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class)))),
        @ApiResponse(responseCode = "401", description = "Not authenticated",
            content = @Content(schema = @Schema(implementation = ErrorApi.class)))
    })
    @GetMapping("/npc/claimed")
    public ResponseEntity<List<String>> getClaimedNpcRewards(@Authenticated Player player) {
        return ResponseEntity.ok(questService.getClaimedNpcIds(player.getId()));
    }

    @Operation(summary = "Claim an NPC reward",
               description = "Claims the secret card the NPC gives away. The first "
                           + "claim grants the card and returns it; later claims "
                           + "return alreadyClaimed=true without granting again.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Claim result",
            content = @Content(schema = @Schema(implementation = NpcRewardClaimResponse.class))),
        @ApiResponse(responseCode = "404", description = "NPC has no reward configured",
            content = @Content(schema = @Schema(implementation = ErrorApi.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated",
            content = @Content(schema = @Schema(implementation = ErrorApi.class)))
    })
    @PostMapping("/npc/{npcId}/claim")
    public ResponseEntity<NpcRewardClaimResponse> claimNpcReward(
            @Authenticated Player player,
            @PathVariable String npcId) {
        if (!questService.hasReward(npcId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(questService.claimReward(player, npcId));
    }
}
