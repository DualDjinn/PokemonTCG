package ar.edu.utn.frc.tup.piii.dtos.player;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
}
