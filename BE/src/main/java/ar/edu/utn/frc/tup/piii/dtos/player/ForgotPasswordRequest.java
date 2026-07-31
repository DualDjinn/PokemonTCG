// Paquete donde se agrupan los DTOs relacionados con el jugador
package ar.edu.utn.frc.tup.piii.dtos.player;

import jakarta.validation.constraints.Email; // Valida que el campo tenga formato de email válido
import jakarta.validation.constraints.NotBlank; // Valida que el campo no sea nulo ni esté vacío
import lombok.Data; // Genera automáticamente getters, setters, toString, equals y hashCode

// DTO que recibe el email del jugador que quiere recuperar su contraseña
@Data
public class ForgotPasswordRequest {
    // Rechaza si el campo está vacío o tiene solo espacios en blanco
    @NotBlank
    // Rechaza si el campo no tiene el formato de email válido (ej: usuario@dominio.com)
    @Email
    private String email; // Email del jugador que solicita recuperar su contraseña
}
