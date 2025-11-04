-- src/main/resources/db/migration/V8__ajustes_inventario.sql

-- 1) Garantizar esquema (no falla si ya existe)
CREATE SCHEMA IF NOT EXISTS softfruver;

-- 2) Vista de inventario que usa lo que YA tienes
--    v_inventario ya existe y trae (producto_id, nombre, stock_kg)
CREATE OR REPLACE VIEW softfruver.v_inventario_listado AS
SELECT producto_id, nombre, stock_kg
FROM softfruver.v_inventario;

-- 3) Vista de alertas, basada en tu v_stock_alerta
CREATE OR REPLACE VIEW softfruver.v_inventario_alerta AS
SELECT producto_id, nombre, stock_kg
FROM softfruver.v_stock_alerta;

-- (Opcional) Si quieres encapsular compras/ventas, usa los nombres REALES:
-- compra_item / venta_item. No crear nada aquí si tu app no lo necesita.
-- Nada de "compra_detalle".
