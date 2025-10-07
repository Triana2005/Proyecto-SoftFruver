-- Evita duplicados de teléfono en clientes activos
CREATE UNIQUE INDEX IF NOT EXISTS ux_cliente_telefono_activo
  ON softfruver.cliente (telefono)
  WHERE archived_at IS NULL AND telefono IS NOT NULL;
