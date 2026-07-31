package ar.edu.utn.frc.tup.piii.dtos.slot;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class SlotSpinRequest {

    @Min(5)
    @Max(50)
    private int bet;

    @Min(0)
    private int winAmount;
}
