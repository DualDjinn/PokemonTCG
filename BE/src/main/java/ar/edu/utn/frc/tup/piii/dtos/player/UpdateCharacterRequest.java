package ar.edu.utn.frc.tup.piii.dtos.player;

import lombok.Data;

@Data
public class UpdateCharacterRequest {
    private String character;
    private String color;
}
