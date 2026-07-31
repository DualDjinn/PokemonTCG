package ar.edu.utn.frc.tup.piii.dtos.shop;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Petición para comprar arte full-art (splash arts) con coins.
 *
 * <p>Los splash arts son cosméticos sin catálogo persistido en el backend; se
 * cobran a precio fijo por unidad ({@code quantity} ítems). El descuento de
 * coins se hace en el servidor para que el saldo quede siempre persistido.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuySplashArtsRequest {
    private int quantity;
}
