package ar.edu.utn.frc.tup.piii.dtos.pack;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpenPackRequest {
    /** "BRONZE" | "SILVER" | "GOLD" */
    private String packType;
}
