package com.softfruver.inventario.service;

import com.softfruver.inventario.repository.VentasRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class VentasService {

    private final VentasRepository ventasRepository;

    public VentasService(VentasRepository ventasRepository) {
        this.ventasRepository = ventasRepository;
    }

    public record ItemNuevaVenta(Long productoId, BigDecimal cantidadKg, BigDecimal precioKg) {
    }

    @Transactional
    public Long registrarVenta(Long clienteId, LocalDate fecha, boolean esCredito, List<ItemNuevaVenta> items) {
        // Limpieza: descarta filas vacías o inválidas
        List<ItemNuevaVenta> limpios = new ArrayList<>();
        if (items != null) {
            for (ItemNuevaVenta it : items) {
                if (it == null || it.productoId() == null)
                    continue;
                if (it.cantidadKg() == null || it.cantidadKg().signum() <= 0)
                    continue;
                if (it.precioKg() == null || it.precioKg().signum() < 0)
                    continue;
                limpios.add(it);
            }
        }
        if (limpios.isEmpty()) {
            throw new IllegalArgumentException("La venta debe tener al menos un ítem con cantidades/precio válidos.");
        }

        // Inserta cabecera; triggers manejarán mayor si es crédito y total se recalcula
        // con items
        Long ventaId = ventasRepository.insertCabecera(clienteId, fecha, esCredito);

        // Inserta ítems (triggers: subtotal en item, validar stock y mover inventario,
        // y total)
        ventasRepository.insertItems(ventaId, limpios);

        return ventaId;
    }

    @Transactional
    public void modificarVenta(Long ventaId, Long clienteId, LocalDate fecha, boolean esCredito,
            List<ItemNuevaVenta> items) {
        // limpiar items
        List<ItemNuevaVenta> limpios = new ArrayList<>();
        if (items != null) {
            for (ItemNuevaVenta it : items) {
                if (it == null || it.productoId() == null)
                    continue;
                if (it.cantidadKg() == null || it.cantidadKg().signum() <= 0)
                    continue;
                if (it.precioKg() == null || it.precioKg().signum() < 0)
                    continue;
                limpios.add(it);
            }
        }
        if (limpios.isEmpty()) {
            throw new IllegalArgumentException("La venta debe tener al menos un ítem válido.");
        }

        // actualizar cabecera
        ventasRepository.updateCabecera(ventaId, clienteId, fecha, esCredito);

        // reemplazar detalle completo
        ventasRepository.deleteItems(ventaId);
        ventasRepository.insertItems(ventaId, limpios);
    }

    @Transactional
    public void eliminarVenta(Long ventaId) {
        ventasRepository.deleteItems(ventaId);
        ventasRepository.deleteCabecera(ventaId);
    }

}
