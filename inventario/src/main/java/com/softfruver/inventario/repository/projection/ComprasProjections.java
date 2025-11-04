package com.softfruver.inventario.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class ComprasProjections {

  // Listado de compras (cabecera)
  public interface CompraListadoItem {
    Long getId();
    LocalDate getFecha();
    String getProveedor();
    BigDecimal getTotal();
  }

  // Detalle de una compra
  public interface CompraDetalleItem {
    Long getProductoId();
    String getProducto();
    BigDecimal getCantidadKg();
    BigDecimal getPrecioUnitario();
  }

  // Para combos (proveedores / productos)
  public interface Opcion {
    Long getId();
    String getNombre();
  }

  private ComprasProjections() {}
}
