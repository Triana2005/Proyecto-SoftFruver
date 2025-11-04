package com.softfruver.inventario.service;

import com.softfruver.inventario.repository.ComprasRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ComprasService {

    private final ComprasRepository comprasRepository;

    public ComprasService(ComprasRepository comprasRepository) {
        this.comprasRepository = comprasRepository;
    }

    public record ItemNuevaCompra(Long productoId, BigDecimal cantidadKg, BigDecimal precioUnit){}

    @Transactional
    public Long registrarCompra(Long proveedorId, LocalDate fecha, List<ItemNuevaCompra> items) {
        List<ItemNuevaCompra> limpios = new ArrayList<>();
        for (ItemNuevaCompra it : items) {
            if (it == null || it.productoId() == null) continue;
            if (it.cantidadKg() == null || it.cantidadKg().signum() <= 0) continue;
            if (it.precioUnit() == null || it.precioUnit().signum() < 0) continue;
            limpios.add(it);
        }
        if (limpios.isEmpty()) {
            throw new IllegalArgumentException("La compra debe tener al menos un ítem con cantidades/precio válidos.");
        }

        Long compraId = comprasRepository.insertCabecera(proveedorId, fecha);
        comprasRepository.insertItems(compraId, limpios);
        // triggers en DB actualizan inventario y total en compra
        return compraId;
    }

    @Transactional
    public void modificarCompra(Long compraId, Long proveedorId, LocalDate fecha, List<ItemNuevaCompra> items) {
        List<ItemNuevaCompra> limpios = new ArrayList<>();
        if (items != null) {
            for (ItemNuevaCompra it : items) {
                if (it == null || it.productoId() == null) continue;
                if (it.cantidadKg() == null || it.cantidadKg().signum() <= 0) continue;
                if (it.precioUnit() == null || it.precioUnit().signum() < 0) continue;
                limpios.add(it);
            }
        }
        if (limpios.isEmpty()) {
            throw new IllegalArgumentException("La compra debe tener al menos un ítem válido.");
        }

        comprasRepository.updateCabecera(compraId, proveedorId, fecha);
        comprasRepository.deleteItems(compraId);
        comprasRepository.insertItems(compraId, limpios);
    }

    @Transactional
    public void eliminarCompra(Long compraId) {
        comprasRepository.deleteItems(compraId);
        comprasRepository.deleteCabecera(compraId);
    }
}
