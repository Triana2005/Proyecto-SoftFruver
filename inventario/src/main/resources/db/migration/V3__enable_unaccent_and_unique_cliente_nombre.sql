-- 1) Activar la extensión unaccent (en el esquema public)
CREATE EXTENSION IF NOT EXISTS unaccent WITH SCHEMA public;

-- 2) Crear un wrapper INMUTABLE para usar en índices
--    (usamos la variante con diccionario constante para garantizar IMMUTABLE)
CREATE OR REPLACE FUNCTION softfruver.f_unaccent(text)
RETURNS text
LANGUAGE sql
IMMUTABLE
PARALLEL SAFE
RETURNS NULL ON NULL INPUT
AS $$
  SELECT public.unaccent('public.unaccent', $1)
$$;

-- 3) Índice único por nombre "normalizado" (ignora mayúsculas y tildes)
--    Solo aplica a clientes NO archivados
CREATE UNIQUE INDEX IF NOT EXISTS ux_cliente_nombre_unaccent
  ON softfruver.cliente ( lower(softfruver.f_unaccent(nombre)) )
  WHERE archived_at IS NULL;
