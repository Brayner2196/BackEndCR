-- Migración: Hacer el campo stock nullable en la tabla publicaciones del schema solemio
-- Descripción: Permite que el campo stock acepte valores NULL

ALTER TABLE solemio.publicaciones
ALTER COLUMN stock DROP NOT NULL;
