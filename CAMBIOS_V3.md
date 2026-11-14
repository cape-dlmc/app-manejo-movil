# FleetControl · Cambios de la versión 3

## Cambios realizados

### Frontend (frontend/app.js y frontend/styles.css)
- Cada cambio de vista muestra un indicador de carga antes de pedir datos, para que no quede contenido anterior visible.
- Si una vista falla, aparece un mensaje claro con el motivo y un botón "Reintentar" (RNF-09).
- En tabletas (768 px) los indicadores pasan a dos columnas y los filtros se apilan mejor.
- En celulares (560 px) los formularios y botones ocupan el ancho completo, las filas de acciones se separan y el menú se desplaza horizontalmente en vez de ocupar varias filas.
- Se respeta la preferencia de movimiento reducido del sistema.

## Verificado
- `node --check frontend/app.js`: sin errores de sintaxis.
- Balance de llaves en styles.css: correcto.

## Pendiente de verificar en su equipo
- Backend: `cd backend && mvn clean test` debe mostrar `Tests run: 3, Failures: 0, Errors: 0`.
- Ejecutar el sistema y probar en el navegador las 12 pruebas de la sección 15 del enunciado.
- Probar el diseño en escritorio, tableta (768 px) y celular (375 px) con las herramientas de desarrollo del navegador.

## Pruebas mínimas (sección 15 del enunciado)
1. Iniciar sesión con credenciales incorrectas → debe mostrar error.
2. Conductor intenta ver movimientos de otro → debe fallar en el backend.
3. Gasto con valor cero o negativo → debe rechazarse.
4. Registrar gasto → queda PENDIENTE.
5. Aprobar gasto → la conciliación se recalcula.
6. Rechazar gasto sin observación → debe pedir la observación; con observación, se conserva.
7. Entrega inferior al esperado → muestra saldo pendiente.
8. Entrega exacta → conciliación en cero.
9. Duplicar ingreso de un viaje → debe rechazarse.
10. Historial financiero de un vehículo → se consulta.
11. Cerrar sesión, volver a entrar → datos persisten.
12. Interfaz en escritorio y celular → se ve y funciona.
