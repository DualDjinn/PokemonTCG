package ar.edu.utn.frc.tup.piii.dtos.player;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleLoginRequest {

    @NotBlank
    private String credential;
}