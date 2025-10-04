package com.softfruver.inventario.repository.projection;

public interface ClienteListado {
  Long getId();
  String getNombre();
  String getTelefono();
  java.math.BigDecimal getSaldoTotal();
}
