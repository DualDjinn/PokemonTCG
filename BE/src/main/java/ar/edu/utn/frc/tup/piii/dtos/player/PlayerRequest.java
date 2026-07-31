package ar.edu.utn.frc.tup.piii.dtos.player;

import java.time.LocalDate;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PlayerRequest {
    private String username;
    private String trainerName;
    @Pattern(regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$", message = "Invalid email format")
    private String email;
    private String password;
    private LocalDate dateOfBirth;
}
