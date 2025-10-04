-- V2__vistas_iniciales.sql
-- Asegura el esquema
SET search_path TO softfruver, public;

-- Vista unificada de saldos de clientes, reutilizando tu v_saldo_cliente
CREATE OR REPLACE VIEW vw_cliente_saldo AS
SELECT
  c.id         AS cliente_id,
  c.nombre     AS nombre,
  c.telefono   AS telefono,
  GREATEST(COALESCE(vs.saldo_total, 0), 0)::numeric(14,2) AS saldo,
  CASE
    WHEN COALESCE(vs.saldo_total, 0) > 0 THEN 'CON CRÉDITO'
    ELSE 'AL DÍA'
  END AS estado
FROM cliente c
LEFT JOIN v_saldo_cliente vs ON vs.cliente_id = c.id
WHERE c.archived_at IS NULL;

-- Vista ordenada para consumo directo en UI
CREATE OR REPLACE VIEW vw_cliente_saldo_orden AS
SELECT *
FROM vw_cliente_saldo
ORDER BY saldo DESC, nombre ASC;
