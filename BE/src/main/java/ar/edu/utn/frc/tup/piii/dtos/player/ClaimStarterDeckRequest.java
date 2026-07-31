package ar.edu.utn.frc.tup.piii.dtos.player;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClaimStarterDeckRequest {
    @NotBlank
    private String deckName;
}
