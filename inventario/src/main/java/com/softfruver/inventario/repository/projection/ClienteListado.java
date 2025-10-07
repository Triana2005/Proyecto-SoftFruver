package com.softfruver.inventario.repository.projection;

import java.math.BigDecimal;

public interface ClienteListado {
  Long getId();
  String getNombre();
  String getTelefono();
  BigDecimal getSaldoTotal();
}
