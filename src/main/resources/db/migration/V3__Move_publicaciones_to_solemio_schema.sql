-- Migración: Mover tabla publicaciones de public a solemio con stock nullable
-- Descripción: Mueve la tabla publicaciones del schema public al schema solemio
-- y modifica el campo stock para que acepte valores NULL

-- 1. Crear tabla en solemio con la misma estructura que public.publicaciones
CREATE TABLE solemio.publicaciones (
    id BIGSERIAL PRIMARY KEY,
    vendedor_id BIGINT NOT NULL,
    vendedor_nombre VARCHAR(150) NOT NULL,
    propiedad_id BIGINT,
    titulo VARCHAR(120) NOT NULL,
    descripcion VARCHAR(1000),
    precio NUMERIC(12, 0) NOT NULL,
    categoria VARCHAR(30) NOT NULL,
    contacto VARCHAR(100),
    marca VARCHAR(50),
    stock INTEGER,
    acepta_domicilio BOOLEAN NOT NULL DEFAULT false,
    metodos_pago VARCHAR(300),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    creado_en TIMESTAMP NOT NULL,
    actualizado_en TIMESTAMP NOT NULL
);

-- 2. Copiar datos de public.publicaciones a solemio.publicaciones
INSERT INTO solemio.publicaciones 
SELECT * FROM public.publicaciones;

-- 3. Eliminar la tabla original del schema public
DROP TABLE public.publicaciones CASCADE;
