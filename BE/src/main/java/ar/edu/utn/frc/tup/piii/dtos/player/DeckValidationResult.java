package ar.edu.utn.frc.tup.piii.dtos.player;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeckValidationResult {
    private boolean valid;
    private List<String> errors;
}
