-- V6__un_solo_admin_activo.sql

-- Permite como mucho 1 fila con rol = 'ADMIN' y activo = true
CREATE UNIQUE INDEX IF NOT EXISTS ux_usuario_unico_admin_activo
  ON softfruver.usuario (rol)
  WHERE rol = 'ADMIN' AND activo = true;
