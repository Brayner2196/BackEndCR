from PIL import Image, ImageDraw, ImageFont
from pathlib import Path
import math

OUT_DIR = Path("docs/arquitectura")
PNG_PATH = OUT_DIR / "MER_BackEndCR_alta_calidad.png"
PDF_PATH = OUT_DIR / "MER_BackEndCR_exportable.pdf"

W, H = 7200, 5000
BG = "#F8FAFC"
INK = "#0F172A"
MUTED = "#64748B"
LINE = "#334155"
PK = "#F59E0B"
FK = "#2563EB"
CARD = "#FFFFFF"
BORDER = "#CBD5E1"

clusters = {
    "GLOBAL / PUBLIC": {"xy": (120, 150), "color": "#E0F2FE"},
    "NÚCLEO RESIDENCIAL": {"xy": (2120, 150), "color": "#DCFCE7"},
    "PAGOS Y CARTERA": {"xy": (120, 1850), "color": "#FEF3C7"},
    "RESERVAS Y PARQUEADEROS": {"xy": (3250, 1850), "color": "#EDE9FE"},
    "COMUNIDAD Y GESTIÓN": {"xy": (5300, 150), "color": "#FFE4E6"},
}

entities = {
    # Global
    "tenants": ("GLOBAL / PUBLIC", ["PK id", "schema_name", "nombre", "codigo", "timezone"]),
    "identidades": ("GLOBAL / PUBLIC", ["PK id", "email", "password", "rol", "FK tenant_id", "activo"]),
    "refresh_tokens": ("GLOBAL / PUBLIC", ["PK id", "token", "FK identidad_id", "expires_at", "revoked"]),
    "tenant_pasarelas": ("GLOBAL / PUBLIC", ["PK id", "FK tenant_id", "tipo_pasarela", "activa", "public_key", "private_key"]),
    "device_tokens": ("GLOBAL / PUBLIC", ["PK id", "FK usuario_id", "FK tenant_id", "token", "plataforma"]),

    # Core
    "usuarios": ("NÚCLEO RESIDENCIAL", ["PK id", "nombre", "FK identidad_id", "telefono", "estado"]),
    "tipos_propiedad": ("NÚCLEO RESIDENCIAL", ["PK id", "nombre", "FK parent_id", "es_facturable", "es_parqueadero"]),
    "propiedades": ("NÚCLEO RESIDENCIAL", ["PK id", "FK tipo_id", "identificador", "FK parent_id", "estado"]),
    "usuario_propiedades": ("NÚCLEO RESIDENCIAL", ["PK id", "FK usuario_id", "FK propiedad_id", "rol", "es_principal"]),
    "inquilino_permisos": ("NÚCLEO RESIDENCIAL", ["PK id", "FK inquilino_id", "FK propietario_id", "permiso"]),

    # Finance
    "periodos_cobro": ("PAGOS Y CARTERA", ["PK id", "anio", "mes", "fecha_inicio", "fecha_fin", "estado"]),
    "cobros": ("PAGOS Y CARTERA", ["PK id", "FK periodo_id", "FK propiedad_id", "concepto", "monto_total", "estado"]),
    "pagos": ("PAGOS Y CARTERA", ["PK id", "FK cobro_id", "FK usuario_id", "monto_pagado", "metodo_pago", "estado"]),
    "abonos": ("PAGOS Y CARTERA", ["PK id", "FK propiedad_id", "FK usuario_id", "monto_total", "estado"]),
    "movimientos_abono": ("PAGOS Y CARTERA", ["PK id", "FK abono_id", "FK cobro_id", "monto_aplicado"]),
    "saldos_favor": ("PAGOS Y CARTERA", ["PK id", "FK propiedad_id", "FK usuario_id", "saldo"]),
    "planes_pago": ("PAGOS Y CARTERA", ["PK id", "FK propiedad_id", "FK residente_id", "monto_total_plan", "estado"]),
    "cuotas_plan": ("PAGOS Y CARTERA", ["PK id", "FK plan_id", "numero_cuota", "monto", "estado"]),
    "configuracion_cuotas": ("PAGOS Y CARTERA", ["PK id", "FK tipo_propiedad_id", "FK propiedad_id", "monto", "periodicidad"]),
    "configuracion_mora": ("PAGOS Y CARTERA", ["PK id", "porcentaje_mensual", "dias_gracia", "tipo_calculo"]),
    "configuracion_plan_pago": ("PAGOS Y CARTERA", ["PK id", "max_cuotas", "recargo", "aprobacion_automatica"]),

    # Reservations / parking
    "zonas_comunes": ("RESERVAS Y PARQUEADEROS", ["PK id", "nombre", "capacidad", "categoria", "requiere_aprobacion"]),
    "reservas": ("RESERVAS Y PARQUEADEROS", ["PK id", "FK zona_comun_id", "FK residente_id", "FK propiedad_id", "fecha", "estado"]),
    "horarios_grupos_zona": ("RESERVAS Y PARQUEADEROS", ["PK id", "FK zona_comun_id", "etiqueta", "dias", "orden"]),
    "franjas_horarias": ("RESERVAS Y PARQUEADEROS", ["PK id", "FK grupo_id", "hora_inicio", "hora_fin"]),
    "excepciones_zonas_comunes": ("RESERVAS Y PARQUEADEROS", ["PK id", "FK zona_comun_id", "fecha", "tipo", "motivo"]),
    "parqueaderos": ("RESERVAS Y PARQUEADEROS", ["PK id", "identificador", "tipo", "FK propiedad_id", "FK vehiculo_id"]),
    "vehiculos": ("RESERVAS Y PARQUEADEROS", ["PK id", "placa", "tipo", "FK propiedad_id", "FK parqueadero_id", "estado"]),
    "configuracion_parqueadero": ("RESERVAS Y PARQUEADEROS", ["PK id", "total_parqueaderos", "max_vehiculos", "requiere_aprobacion"]),

    # Community
    "pqrs": ("COMUNIDAD Y GESTIÓN", ["PK id", "tipo", "asunto", "FK residente_id", "FK propiedad_id", "estado"]),
    "pqr_historial": ("COMUNIDAD Y GESTIÓN", ["PK id", "FK pqr_id", "estado_anterior", "estado_nuevo"]),
    "publicaciones": ("COMUNIDAD Y GESTIÓN", ["PK id", "FK vendedor_id", "FK propiedad_id", "titulo", "precio", "estado"]),
    "solicitudes": ("COMUNIDAD Y GESTIÓN", ["PK id", "FK publicacion_id", "FK comprador_id", "FK vendedor_id", "estado"]),
    "presupuestos": ("COMUNIDAD Y GESTIÓN", ["PK id", "anio", "titulo", "monto_total", "activo"]),
    "categorias_presupuesto": ("COMUNIDAD Y GESTIÓN", ["PK id", "FK presupuesto_id", "nombre", "monto_asignado"]),
    "gastos_registrados": ("COMUNIDAD Y GESTIÓN", ["PK id", "FK presupuesto_id", "FK categoria_id", "descripcion", "monto"]),
    "anuncios": ("COMUNIDAD Y GESTIÓN", ["PK id", "titulo", "contenido", "creado_por", "estado"]),
    "anuncio_vistas": ("COMUNIDAD Y GESTIÓN", ["PK id", "FK anuncio_id", "FK residente_id", "visto_en"]),
    "votaciones": ("COMUNIDAD Y GESTIÓN", ["PK id", "titulo", "tipo_votacion", "estado", "fecha_fin"]),
    "opciones_votacion": ("COMUNIDAD Y GESTIÓN", ["PK id", "FK votacion_id", "texto", "orden"]),
    "votos_residentes": ("COMUNIDAD Y GESTIÓN", ["PK id", "FK votacion_id", "FK residente_id", "FK opcion_id"]),
}

cluster_layouts = {
    "GLOBAL / PUBLIC": ["tenants", "identidades", "refresh_tokens", "tenant_pasarelas", "device_tokens"],
    "NÚCLEO RESIDENCIAL": ["usuarios", "tipos_propiedad", "propiedades", "usuario_propiedades", "inquilino_permisos"],
    "PAGOS Y CARTERA": ["periodos_cobro", "cobros", "pagos", "abonos", "movimientos_abono", "saldos_favor", "planes_pago", "cuotas_plan", "configuracion_cuotas", "configuracion_mora", "configuracion_plan_pago"],
    "RESERVAS Y PARQUEADEROS": ["zonas_comunes", "reservas", "horarios_grupos_zona", "franjas_horarias", "excepciones_zonas_comunes", "parqueaderos", "vehiculos", "configuracion_parqueadero"],
    "COMUNIDAD Y GESTIÓN": ["pqrs", "pqr_historial", "publicaciones", "solicitudes", "presupuestos", "categorias_presupuesto", "gastos_registrados", "anuncios", "anuncio_vistas", "votaciones", "opciones_votacion", "votos_residentes"],
}

relationships = [
    ("tenants", "identidades", "1", "N", "tenant_id"),
    ("tenants", "tenant_pasarelas", "1", "N", "tenant_id"),
    ("identidades", "refresh_tokens", "1", "N", "identidad_id"),
    ("identidades", "usuarios", "1", "N", "identidad_id"),
    ("usuarios", "device_tokens", "1", "N", "usuario_id"),
    ("tipos_propiedad", "tipos_propiedad", "1", "N", "parent_id"),
    ("tipos_propiedad", "propiedades", "1", "N", "tipo_id"),
    ("propiedades", "propiedades", "1", "N", "parent_id"),
    ("usuarios", "usuario_propiedades", "1", "N", "usuario_id"),
    ("propiedades", "usuario_propiedades", "1", "N", "propiedad_id"),
    ("usuarios", "inquilino_permisos", "1", "N", "inquilino/propietario"),
    ("periodos_cobro", "cobros", "1", "N", "periodo_id"),
    ("propiedades", "cobros", "1", "N", "propiedad_id"),
    ("cobros", "pagos", "1", "N", "cobro_id"),
    ("usuarios", "pagos", "1", "N", "usuario_id"),
    ("propiedades", "abonos", "1", "N", "propiedad_id"),
    ("usuarios", "abonos", "1", "N", "usuario_id"),
    ("abonos", "movimientos_abono", "1", "N", "abono_id"),
    ("cobros", "movimientos_abono", "1", "N", "cobro_id"),
    ("propiedades", "saldos_favor", "1", "N", "propiedad_id"),
    ("usuarios", "saldos_favor", "1", "N", "usuario_id"),
    ("propiedades", "planes_pago", "1", "N", "propiedad_id"),
    ("usuarios", "planes_pago", "1", "N", "residente_id"),
    ("planes_pago", "cuotas_plan", "1", "N", "plan_id"),
    ("tipos_propiedad", "configuracion_cuotas", "1", "N", "tipo_propiedad_id"),
    ("propiedades", "configuracion_cuotas", "1", "N", "propiedad_id"),
    ("zonas_comunes", "reservas", "1", "N", "zona_comun_id"),
    ("usuarios", "reservas", "1", "N", "residente_id"),
    ("propiedades", "reservas", "1", "N", "propiedad_id"),
    ("zonas_comunes", "horarios_grupos_zona", "1", "N", "zona_comun_id"),
    ("horarios_grupos_zona", "franjas_horarias", "1", "N", "grupo_id"),
    ("zonas_comunes", "excepciones_zonas_comunes", "1", "N", "zona_comun_id"),
    ("propiedades", "parqueaderos", "1", "N", "propiedad_id"),
    ("propiedades", "vehiculos", "1", "N", "propiedad_id"),
    ("parqueaderos", "vehiculos", "1", "N", "parqueadero_id"),
    ("usuarios", "pqrs", "1", "N", "residente_id"),
    ("propiedades", "pqrs", "1", "N", "propiedad_id"),
    ("pqrs", "pqr_historial", "1", "N", "pqr_id"),
    ("usuarios", "publicaciones", "1", "N", "vendedor_id"),
    ("propiedades", "publicaciones", "1", "N", "propiedad_id"),
    ("publicaciones", "solicitudes", "1", "N", "publicacion_id"),
    ("usuarios", "solicitudes", "1", "N", "comprador/vendedor"),
    ("presupuestos", "categorias_presupuesto", "1", "N", "presupuesto_id"),
    ("presupuestos", "gastos_registrados", "1", "N", "presupuesto_id"),
    ("categorias_presupuesto", "gastos_registrados", "1", "N", "categoria_id"),
    ("anuncios", "anuncio_vistas", "1", "N", "anuncio_id"),
    ("usuarios", "anuncio_vistas", "1", "N", "residente_id"),
    ("votaciones", "opciones_votacion", "1", "N", "votacion_id"),
    ("votaciones", "votos_residentes", "1", "N", "votacion_id"),
    ("opciones_votacion", "votos_residentes", "1", "N", "opcion_id"),
    ("usuarios", "votos_residentes", "1", "N", "residente_id"),
]

# Fonts
font_paths = [
    Path("C:/Windows/Fonts/arial.ttf"),
    Path("C:/Windows/Fonts/arialbd.ttf"),
    Path("C:/Windows/Fonts/segoeui.ttf"),
]
regular_path = font_paths[0] if font_paths[0].exists() else None
bold_path = font_paths[1] if font_paths[1].exists() else regular_path

def font(size, bold=False):
    path = bold_path if bold else regular_path
    if path:
        return ImageFont.truetype(str(path), size)
    return ImageFont.load_default()

f_title = font(72, True)
f_sub = font(34, False)
f_cluster = font(34, True)
f_entity = font(26, True)
f_field = font(21, False)
f_label = font(18, True)
f_note = font(22, False)

img = Image.new("RGB", (W, H), BG)
d = ImageDraw.Draw(img)

# Title
margin = 90
d.text((margin, 45), "Modelo Entidad-Relación · BackEndCR", fill=INK, font=f_title)
d.text((margin, 128), "Arquitectura multitenant por schema PostgreSQL · Tablas globales en public y datos operativos por tenant", fill=MUTED, font=f_sub)

# Draw cluster panels
cluster_sizes = {
    "GLOBAL / PUBLIC": (1820, 1500),
    "NÚCLEO RESIDENCIAL": (2000, 1500),
    "PAGOS Y CARTERA": (3000, 2900),
    "RESERVAS Y PARQUEADEROS": (1970, 2900),
    "COMUNIDAD Y GESTIÓN": (1940, 4550),
}
for name, meta in clusters.items():
    x, y = meta["xy"]
    w, h = cluster_sizes[name]
    d.rounded_rectangle((x, y, x+w, y+h), radius=34, fill=meta["color"], outline="#94A3B8", width=3)
    d.text((x+34, y+24), name, fill=INK, font=f_cluster)

# Entity positioning per cluster grid
positions = {}
BOX_W = 520
ROW_H = 34
HEADER_H = 54
PAD = 18
GAP_X = 70
GAP_Y = 60

cluster_cols = {
    "GLOBAL / PUBLIC": 2,
    "NÚCLEO RESIDENCIAL": 2,
    "PAGOS Y CARTERA": 3,
    "RESERVAS Y PARQUEADEROS": 2,
    "COMUNIDAD Y GESTIÓN": 2,
}

for cluster_name, names in cluster_layouts.items():
    cx, cy = clusters[cluster_name]["xy"]
    cols = cluster_cols[cluster_name]
    start_x = cx + 40
    start_y = cy + 100
    col_heights = [start_y] * cols
    for i, name in enumerate(names):
        col = min(range(cols), key=lambda c: col_heights[c])
        x = start_x + col * (BOX_W + GAP_X)
        y = col_heights[col]
        fields = entities[name][1]
        h = HEADER_H + PAD + len(fields) * ROW_H + PAD
        positions[name] = (x, y, BOX_W, h)
        col_heights[col] = y + h + GAP_Y

# Relationship lines behind boxes
def anchor(name, other):
    x, y, w, h = positions[name]
    ox, oy, ow, oh = positions[other]
    cx, cy = x + w/2, y + h/2
    ocx, ocy = ox + ow/2, oy + oh/2
    dx, dy = ocx - cx, ocy - cy
    if abs(dx) > abs(dy):
        return (x + (w if dx > 0 else 0), cy)
    return (cx, y + (h if dy > 0 else 0))

def draw_relation(a, b, left_card, right_card, label):
    if a == b:
        x, y, w, h = positions[a]
        pts = [(x+w, y+h*0.35), (x+w+38, y+h*0.35), (x+w+38, y+h*0.65), (x+w, y+h*0.65)]
        d.line(pts, fill=LINE, width=3)
        d.text((x+w+42, y+h*0.48), label, fill=MUTED, font=f_label)
        return
    ax, ay = anchor(a, b)
    bx, by = anchor(b, a)
    midx = (ax + bx) / 2
    # subtle curved-ish polyline
    pts = [(ax, ay), (midx, ay), (midx, by), (bx, by)]
    d.line(pts, fill=LINE, width=2)
    # terminal dots/cardinality
    r = 10
    d.ellipse((ax-r, ay-r, ax+r, ay+r), fill="#FFFFFF", outline=LINE, width=2)
    d.ellipse((bx-r, by-r, bx+r, by+r), fill="#FFFFFF", outline=LINE, width=2)
    d.text((ax+10, ay-25), left_card, fill=INK, font=f_label)
    d.text((bx-28, by+8), right_card, fill=INK, font=f_label)
    lx, ly = midx + 6, (ay + by) / 2 - 12
    if abs(ax-bx) > 650 or abs(ay-by) > 450:
        # keep labels sparse on long lines
        d.text((lx, ly), label, fill=MUTED, font=f_label)

for rel in relationships:
    draw_relation(*rel)

# Draw entity boxes over lines
for name, (x, y, w, h) in positions.items():
    # shadow
    d.rounded_rectangle((x+8, y+10, x+w+8, y+h+10), radius=20, fill="#CBD5E1")
    d.rounded_rectangle((x, y, x+w, y+h), radius=20, fill=CARD, outline=BORDER, width=3)
    d.rounded_rectangle((x, y, x+w, y+HEADER_H), radius=20, fill="#0F172A", outline="#0F172A")
    d.rectangle((x, y+HEADER_H-18, x+w, y+HEADER_H), fill="#0F172A")
    d.text((x+PAD, y+14), name, fill="#FFFFFF", font=f_entity)
    fy = y + HEADER_H + PAD
    for field in entities[name][1]:
        color = INK
        bullet = "•"
        if field.startswith("PK "):
            color = PK
            bullet = "◆"
        elif field.startswith("FK "):
            color = FK
            bullet = "◇"
        d.text((x+PAD, fy), bullet, fill=color, font=f_field)
        d.text((x+PAD+30, fy), field, fill=color if field.startswith(("PK ", "FK ")) else INK, font=f_field)
        fy += ROW_H

# Legend
legend_x, legend_y = 120, H - 310
d.rounded_rectangle((legend_x, legend_y, legend_x+2550, legend_y+210), radius=24, fill="#FFFFFF", outline=BORDER, width=3)
d.text((legend_x+30, legend_y+24), "Leyenda", fill=INK, font=f_cluster)
d.text((legend_x+30, legend_y+78), "◆ PK = clave primaria    ◇ FK = clave foránea / referencia    1 → N = una entidad relacionada con muchas", fill=INK, font=f_note)
d.text((legend_x+30, legend_y+122), "Nota: varias relaciones están modeladas como campos *_id en las entidades JPA, no siempre como @ManyToOne explícito.", fill=MUTED, font=f_note)
d.text((legend_x+30, legend_y+164), "Generado desde el análisis de entidades del proyecto BackEndCR.", fill=MUTED, font=f_note)

# Save high quality PNG and PDF
OUT_DIR.mkdir(parents=True, exist_ok=True)
img.save(PNG_PATH, "PNG", optimize=True)
# PDF: use 300 DPI so the giant image maps to 24x16.67 inches, crisp for export/print
img.save(PDF_PATH, "PDF", resolution=300.0)
print(PNG_PATH.resolve())
print(PDF_PATH.resolve())
