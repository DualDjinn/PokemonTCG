// Paquete donde se agrupan los DTOs relacionados con el jugador
package ar.edu.utn.frc.tup.piii.dtos.player;

import jakarta.validation.constraints.NotBlank; // Valida que el campo no sea nulo ni esté vacío

import lombok.Data; // Genera automáticamente getters, setters, toString, equals y hashCode

// DTO que recibe el token de recuperación y la nueva contraseña para restablecerla
@Data
public class ResetPasswordRequest {
    // Rechaza si el token está vacío o es nulo
    @NotBlank
    private String token; // Token UUID único enviado por email para verificar la identidad del jugador
    // Rechaza si la nueva contraseña está vacía o es nula
    @NotBlank
    private String newPassword; // Nueva contraseña que el jugador quiere establecer
}
