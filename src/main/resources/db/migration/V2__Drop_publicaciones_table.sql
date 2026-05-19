-- Migración: Eliminar la tabla publicaciones del schema solemio
-- Descripción: Elimina completamente la tabla publicaciones

DROP TABLE IF EXISTS solemio.publicaciones CASCADE;
