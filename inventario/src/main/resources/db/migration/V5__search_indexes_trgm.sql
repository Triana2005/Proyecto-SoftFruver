-- Asegurar extensiones necesarias
CREATE EXTENSION IF NOT EXISTS unaccent;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- =========================================================================
-- Wrapper IMMUTABLE para poder usar en índices de expresión
-- Nota:
--  - Llamamos al diccionario 'public.unaccent' explícitamente para que el
--    planificador lo considere constante y la función pueda ser IMMUTABLE.
--  - Marcamos la función como IMMUTABLE y PARALLEL SAFE.
--  - La ponemos en el mismo esquema de la app para evitar problemas de search_path.
-- =========================================================================
CREATE OR REPLACE FUNCTION softfruver.imm_unaccent(text)
RETURNS text
LANGUAGE sql
IMMUTABLE
PARALLEL SAFE
AS $$
  SELECT public.unaccent('public.unaccent', $1)
$$;

-- =========================================================================
-- Índices GIN (trigrama) sobre campos normalizados:
--   imm_unaccent(lower(...))  --> IMMUTABLE, apto para índice de expresión
-- =========================================================================

-- OJO: calificar con el esquema para no depender del search_path
CREATE INDEX IF NOT EXISTS ix_cliente_nombre_trgm
  ON softfruver.cliente
  USING gin ((softfruver.imm_unaccent(lower(nombre))) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS ix_cliente_tel_trgm
  ON softfruver.cliente
  USING gin ((softfruver.imm_unaccent(lower(telefono))) gin_trgm_ops);

-- (Opcional) Índices B-Tree por prefijo (q%) sobre la misma expresión
CREATE INDEX IF NOT EXISTS ix_cliente_nombre_prefijo
  ON softfruver.cliente (softfruver.imm_unaccent(lower(nombre)));

CREATE INDEX IF NOT EXISTS ix_cliente_tel_prefijo
  ON softfruver.cliente (softfruver.imm_unaccent(lower(telefono)));
