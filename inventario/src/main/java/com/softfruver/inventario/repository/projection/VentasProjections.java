package com.softfruver.inventario.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public class VentasProjections {
    private VentasProjections() {}

    public record VentaRow(Long id, LocalDate fecha, String cliente, Integer items, BigDecimal total) {}
    public record VentaCabecera(Long id, LocalDate fecha, String cliente, String observacion) {}
    public record VentaDetalle(String producto, BigDecimal cantidadKg, BigDecimal precioKg, BigDecimal subtotal) {}
    public record Opcion(Long id, String nombre) {}
}
