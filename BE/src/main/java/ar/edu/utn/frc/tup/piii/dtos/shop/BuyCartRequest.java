package ar.edu.utn.frc.tup.piii.dtos.shop;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuyCartRequest {
    private List<BuyCartItemRequest> items;
}
