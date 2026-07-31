package ar.edu.utn.frc.tup.piii.dtos.player;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateDeckRequest {
    private List<String> cardIds;
}
