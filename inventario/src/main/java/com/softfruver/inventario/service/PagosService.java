package com.softfruver.inventario.service;

import com.softfruver.inventario.repository.PagosRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class PagosService {

    private final PagosRepository repo;

    public PagosService(PagosRepository repo) {
        this.repo = repo;
    }

    private static String validarMetodo(String metodo) {
        if (metodo == null || metodo.isBlank())
            throw new IllegalArgumentException("El método de pago es obligatorio (EFECTIVO o TRANSFERENCIA).");
        String m = metodo.trim().toUpperCase();
        if (!m.equals("EFECTIVO") && !m.equals("TRANSFERENCIA"))
            throw new IllegalArgumentException("Método inválido. Use: EFECTIVO o TRANSFERENCIA.");
        return m;
    }

    @Transactional
    public Long registrarPago(String tipo, Long refId, LocalDate fecha, BigDecimal monto, String metodo) {
        if (refId == null) throw new IllegalArgumentException("Debe seleccionar cliente/proveedor.");
        if (fecha == null) throw new IllegalArgumentException("La fecha es obligatoria.");
        if (monto == null || monto.signum() <= 0) throw new IllegalArgumentException("El monto debe ser mayor a 0.");

        String m = validarMetodo(metodo);
        String t = (tipo == null ? "" : tipo.trim().toUpperCase());
        return switch (t) {
            case "CLIENTE"   -> repo.insertPagoCliente(refId, fecha, monto, m);
            case "PROVEEDOR" -> repo.insertPagoProveedor(refId, fecha, monto, m);
            default -> throw new IllegalArgumentException("Tipo inválido: " + tipo);
        };
    }

    @Transactional
    public void modificarPago(Long id, String tipo, Long refId, LocalDate fecha, BigDecimal monto, String metodo) {
        if (id == null) throw new IllegalArgumentException("ID de pago requerido.");
        if (refId == null) throw new IllegalArgumentException("Debe seleccionar cliente/proveedor.");
        if (fecha == null) throw new IllegalArgumentException("La fecha es obligatoria.");
        if (monto == null || monto.signum() <= 0) throw new IllegalArgumentException("El monto debe ser mayor a 0.");

        String m = validarMetodo(metodo);
        String t = (tipo == null ? "" : tipo.trim().toUpperCase());
        switch (t) {
            case "CLIENTE"   -> repo.updatePagoCliente(id, refId, fecha, monto, m);
            case "PROVEEDOR" -> repo.updatePagoProveedor(id, refId, fecha, monto, m);
            default -> throw new IllegalArgumentException("Tipo inválido: " + tipo);
        }
    }

    @Transactional
    public void eliminarPago(Long id) {
        String tipo = repo.tipoPorId(id);
        if (tipo == null) throw new IllegalArgumentException("Pago no encontrado: " + id);
        if ("CLIENTE".equals(tipo)) repo.deletePagoCliente(id);
        else repo.deletePagoProveedor(id);
    }
}
