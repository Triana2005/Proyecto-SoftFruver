CREATE SCHEMA IF NOT EXISTS softfruver;

CREATE OR REPLACE VIEW softfruver.v_inventario_listado AS
SELECT
  p.id,
  p.nombre,
  COALESCE(a.stock_kg, 0) AS stock_kg
FROM softfruver.producto p
LEFT JOIN softfruver.v_stock_alerta a
  ON UPPER(a.nombre) = UPPER(p.nombre);
