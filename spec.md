# Pawa Scanner — Design Spec

## 1. Identidad visual

### Color principal — Verde Pawa

| Token | Valor | Uso |
| --- | --- | --- |
| `pawa_green` | `#8CC620` | Color de marca. Acciones primarias, acentos, indicadores activos, foco. |
| `pawa_green_dark` | `#6E9C19` | Estado pressed, bordes de contraste, texto verde sobre fondos claros. |
| `pawa_green_light` | `#A8D94A` | Hover, highlights, glow en superficies de vidrio. |
| `pawa_green_soft` | `#8CC620` @ 12 % | Fondos de chips, selección sutil, tints de superficie. |

El verde Pawa es el ÚNICO color de marca. Todo lo demás en la UI es neutro o semántico.

### Regla de contraste (obligatoria)

`#8CC620` sobre blanco tiene un contraste de ~2.1:1: **NO cumple WCAG AA para texto**.
Por eso:

- `#8CC620` se usa como **fondo** con texto/ícono `#0E1208` (contraste ~7:1) o blanco solo en tamaños ≥ 24 sp bold.
- Como **texto o ícono sobre fondo claro** se usa `pawa_green_dark` (`#6E9C19`) o más oscuro.
- Nunca se usa verde como única señal de estado (acompañar con ícono o texto).

### Neutros

| Token | Valor | Uso |
| --- | --- | --- |
| `surface_base` | `#0B0F0A` | Fondo de la app (modo oscuro por defecto). |
| `surface_raised` | `#121711` | Superficie de tarjetas opacas. |
| `text_primary` | `#F2F5EE` | Texto principal sobre superficie oscura. |
| `text_secondary` | `#A7B0A0` | Texto secundario, labels. |
| `outline` | `#FFFFFF` @ 18 % | Bordes de vidrio. |

### Semánticos

| Token | Valor |
| --- | --- |
| `success` | `#4CD964` |
| `warning` | `#F5A623` |
| `error` | `#FF5A5F` |
| `info` | `#4DA3FF` |

---

## 2. Estilo: Liquid Glass

La UI se construye con superficies de vidrio translúcido flotando sobre un fondo vivo
(gradiente suave con el verde Pawa + neutros oscuros). El vidrio debe sentirse físico:
luz que atraviesa, bordes que atrapan reflejos, sombras suaves y difusas.

### 2.1 Superficie de vidrio — especificación

| Propiedad | Valor |
| --- | --- |
| Fill | Blanco `#FFFFFF` @ 8 % (sobre fondo oscuro) / Negro `#000000` @ 6 % (sobre fondo claro) |
| Blur de fondo | `24dp` (radio), aplicado al contenido detrás del panel |
| Borde | `1dp` con gradiente `#FFFFFF` @ 40 % → `#FFFFFF` @ 8 % (ángulo 135°) |
| Inner highlight | Línea superior `1dp` `#FFFFFF` @ 50 % (simula luz entrando por arriba) |
| Sombra externa | `y=16dp`, `blur=40dp`, `#000000` @ 35 % |
| Radio de esquina | `28dp` paneles grandes · `20dp` tarjetas · `16dp` chips/inputs · `999dp` pills |
| Overlay de tinte | Opcional: `pawa_green` @ 8 % para paneles activos |

### 2.2 Jerarquía de capas (z-order)

1. **Fondo base** — gradiente radial/linear `#0B0F0A` → `#0E1409` con 2 blobs difusos de
   `pawa_green` @ 15 % (uno arriba-izquierda, uno abajo-derecha).
2. **Contenido** — listas, scanner preview, texto.
3. **Vidrio** — navegación inferior, barras, sheets, tarjetas flotantes.
4. **Modales / diálogos** — vidrio con mayor opacidad de fill (12 %) y blur 32dp.

### 2.3 Reglas de uso

- Máximo **2 capas de vidrio apiladas**. Más que eso destruye la legibilidad.
- El vidrio NUNCA contiene texto de cuerpo largo; solo títulos cortos, labels y acciones.
- Detrás de un panel de vidrio siempre debe haber algo con color/textura; el vidrio
  sobre fondo plano se ve sucio. Si no hay contenido detrás, usar la variante opaca.
- El blur es un lujo, no un requisito: en dispositivos o contextos sin soporte se degrada
  a fill `#FFFFFF` @ 14 % sin blur (ver 2.4).

### 2.4 Implementación en Android

- **Blur real**: `RenderEffect.createBlurEffect()` en `View.setRenderEffect()` es API 31+.
  Para `minSdk 24` se necesita fallback:
  - API 31+: blur real + borde con gradiente (`GradientDrawable` o `Compose` `Brush`).
  - API < 31: fill translúcido más opaco (14–18 %) + borde, **sin** blur. Nunca simular
    blur con imágenes pre-renderizadas.
- **Compose**: `Modifier.background(brush, shape)` + `Modifier.border(1.dp, brush, shape)`
  + `Modifier.shadow()`. Para blur, `Modifier.blur()` solo aplica al propio contenido, no
  al fondo; el blur de fondo requiere `graphicsLayer` + `RenderEffect` o `HazeEffect`.
- **XML/Views**: `CardView`/`MaterialCardView` con `cardBackgroundColor` translúcido +
  `cardElevation`, y `foreground` con gradiente para el highlight superior.
- Activar `window.setBackgroundBlurRadius()` solo si el target lo soporta; no es base.
- Evitar blur animado en tiempo real: consume GPU y batería. Blur estático por pantalla.

---

## 3. Tipografía

| Rol | Tamaño | Peso | Color |
| --- | --- | --- | --- |
| Display | 32 sp | 700 | `text_primary` |
| Título de pantalla | 24 sp | 700 | `text_primary` |
| Título de tarjeta | 18 sp | 600 | `text_primary` |
| Cuerpo | 15 sp | 400 | `text_primary` |
| Secundario | 13 sp | 400 | `text_secondary` |
| Label / botón | 14 sp | 600 | según superficie |

Fuente: system sans (Roboto). Tracking negativo (-0.5 %) en títulos grandes para el look
"liquid".

---

## 4. Componentes

### Botón primario
- Fondo: `pawa_green` sólido, esquinas `999dp` (pill), altura `52dp`.
- Texto: `#0E1208`, 14 sp, 600.
- Pressed: `pawa_green_dark` + escala 0.97.
- Sin sombra de color; el verde ya es el foco.

### Botón secundario (glass)
- Vidrio (2.1) con fill 8 %, texto `text_primary`.
- Borde 1dp blanco @ 20 %.

### Tarjeta de vidrio
- Radio 20dp, padding 16dp, gap interno 12dp.
- Si es interactiva: highlight `pawa_green` @ 8 % en pressed.

### Barra inferior / navegación
- Vidrio flotante, separada 16dp de los bordes, radio 28dp.
- Ítem activo: ícono + label en `pawa_green`; inactivo: `text_secondary`.

### Inputs
- Vidrio con radio 16dp, borde 1dp `outline`.
- Foco: borde 1.5dp `pawa_green` + glow exterior `pawa_green` @ 25 %.
- Placeholder `text_secondary`; texto `text_primary`.

### Chips
- Radio pill, fill vidrio 8 %.
- Seleccionado: fill `pawa_green_soft` + borde `pawa_green` + texto `pawa_green_light`.

### Diálogos / bottom sheets
- Vidrio con fill 12 %, blur 32dp, radio 28dp, scrim negro @ 40 %.
- Acción primaria siempre en verde.

---

## 5. Espaciado y layout

- Grid base: múltiplos de **4dp**.
- Márgenes de pantalla: **20dp**.
- Padding interno de tarjetas: **16dp**.
- Separación entre tarjetas: **12dp**.
- Área táctil mínima: **48dp**.

---

## 6. Movimiento

- Duración estándar: **220 ms**; entrada de paneles: **320 ms**.
- Curva: `ease-out` para entradas, `ease-in` para salidas, spring suave para sheets.
- El vidrio nunca "rebota" agresivamente: el movimiento es fluido, acuoso, con fricción.
- Respetar "reducir animaciones" del sistema: en ese caso, fade simple.

---

## 7. Accesibilidad

- Contraste mínimo AA (4.5:1 texto, 3:1 elementos grandes) — ver regla del verde (1).
- Todo elemento de vidrio debe mantener legibilidad con el fondo más claro y más oscuro
  posible; si no, subir opacidad del fill.
- Foco visible siempre con borde `pawa_green`.
- Textos con `android:textColor` explícito, nunca heredado de superficie translúcida.
- Soporte de `fontScale` hasta 1.5× sin romper layout de vidrio.

---

## 8. Tokens a declarar (res/values/colors.xml)

```xml
<color name="pawa_green">#FF8CC620</color>
<color name="pawa_green_dark">#FF6E9C19</color>
<color name="pawa_green_light">#FFA8D94A</color>
<color name="surface_base">#FF0B0F0A</color>
<color name="surface_raised">#FF121711</color>
<color name="text_primary">#FFF2F5EE</color>
<color name="text_secondary">#FFA7B0A0</color>
<color name="glass_fill_dark">#14FFFFFF</color>   <!-- blanco 8% -->
<color name="glass_border">#33FFFFFF</color>    <!-- blanco 20% -->
<color name="on_pawa_green">#FF0E1208</color>
```

---

## 9. Checklist de aceptación

- [ ] `pawa_green` definido como color primario del tema Material 3 (`colorPrimary`).
- [ ] Todo texto sobre verde cumple contraste AA.
- [ ] Paneles de vidrio usan fill translúcido + borde 1dp + blur real o fallback declarado.
- [ ] Máximo 2 capas de vidrio apiladas por pantalla.
- [ ] Blur estático, sin animación continua.
- [ ] Radios, espaciado y tipografía salen de los tokens de este spec.
- [ ] Foco visible en verde en todos los controles interactivos.
