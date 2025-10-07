package com.softfruver.inventario.repository.projection;

import java.math.BigDecimal;

public interface ProveedorListado {
  Long getId();
  String getNombre();
  String getTelefono();
  BigDecimal getDeudaTotal();
}
