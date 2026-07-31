package ar.edu.utn.frc.tup.piii.dtos.shop;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuyCartItemRequest {
    private String cardId;
    private int quantity;
}
