# Manual de usuario de FleetControl

FleetControl centraliza los vehículos, los conductores, los viajes, los gastos con su comprobante
y el dinero que los conductores entregan a la empresa. Este manual explica cómo usarlo según su rol.

- **Administrador:** es quien administra la flota, revisa los gastos y verifica las entregas.
- **Conductor:** registra sus viajes, sus gastos con comprobante y las entregas de dinero que hace.

Cada conductor solo ve sus propios viajes, gastos y entregas. El administrador ve la información de toda la empresa.

---

## 1. Ingresar al sistema

1. Abra la aplicación en el navegador (por ejemplo, `http://127.0.0.1:5500`).
2. Escriba el **correo** y la **contraseña** que le entregó la administración.
3. Pulse **Ingresar**.

Si los datos no son correctos, aparece el mensaje *"Correo o contraseña incorrectos"*. Revise que el correo esté bien escrito y que no tenga espacios.

Para salir, pulse **Cerrar sesión** en la parte inferior del menú lateral. Al volver a entrar, encontrará la misma información: nada se borra al cerrar la sesión.

---

## 2. Tareas del administrador

### 2.1 Resumen

Es la primera pantalla después de ingresar. Muestra:

- **Vehículos activos:** vehículos que no están inactivos.
- **Viajes completados:** viajes marcados como completados.
- **Ingresos registrados:** suma del valor cobrado en los viajes completados.
- **Egresos aprobados:** suma de los gastos aprobados.
- **Pendiente por conciliar:** dinero que todavía falta por entregar.
- **Gastos pendientes de revisión:** si este número es mayor que cero, hay gastos esperando su decisión.

### 2.2 Vehículos

**Registrar un vehículo**

1. En el menú, entre a **Vehículos**.
2. En el formulario de la derecha, escriba la placa, la marca, el modelo y el año.
3. Elija el estado: *Disponible*, *En servicio*, *Mantenimiento* o *Inactivo*.
4. Pulse **Guardar vehículo**.

La placa no puede repetirse. Un vehículo **inactivo** no se puede asignar a viajes nuevos ni recibir gastos nuevos.

**Editar un vehículo.** En la tabla, pulse **Editar**. El formulario se llena con los datos. Haga los cambios y pulse **Guardar vehículo**. Para salir sin guardar, pulse **Cancelar edición**.

**Buscar por placa.** Escriba la placa en el campo *Buscar por placa*. La lista se filtra mientras escribe.

**Ver el historial financiero de un vehículo.** En la tabla, pulse **Historial**. Se abre la pantalla de movimientos ya filtrada por ese vehículo.

### 2.3 Conductores

**Registrar un conductor**

1. Entre a **Conductores**.
2. Complete el nombre, el correo de acceso, una contraseña temporal de al menos 8 caracteres, la identificación y el teléfono (opcional).
3. Pulse **Guardar conductor**.

Entregue el correo y la contraseña temporal al conductor. Recomiéndele cambiarla al ingresar por primera vez.

**Editar un conductor.** Pulse **Editar**, haga los cambios y pulse **Guardar conductor**.

**Activar o desactivar.** Pulse **Desactivar** para que el conductor no pueda recibir viajes nuevos. Pulse **Activar** para restablecerlo.

### 2.4 Viajes

**Programar un viaje**

1. Entre a **Viajes**.
2. En el formulario de la derecha, escriba el código del viaje (por ejemplo, *VJ-1049*), elija el vehículo y el conductor.
3. Elija el tipo de servicio: *Intermunicipal* o *Puerta a puerta*.
4. Escriba el origen, el destino, la fecha y hora de salida y el valor cobrado.
5. Pulse **Guardar viaje**.

El código del viaje no puede repetirse.

**Cambiar el estado de un viaje.** En la tabla, use los botones de la columna *Acciones*:

- **Iniciar:** pasa el viaje a *En curso*.
- **Completar:** marca el viaje como completado. Solo los viajes completados generan ingreso en la conciliación.
- **Cancelar:** el viaje no cuenta como servicio realizado. Los gastos y entregas ya registrados se conservan para revisión.

**Filtrar viajes.** Use los campos de arriba de la tabla (fecha, conductor, vehículo, tipo de servicio o estado) y pulse **Filtrar viajes**.

### 2.5 Movimientos financieros

Muestra todos los gastos de la empresa, con su estado de revisión, su vehículo, su viaje y su comprobante. Use los filtros para buscar por fecha, vehículo, viaje, categoría, conductor o estado, y pulse **Filtrar gastos**.

**Ver un comprobante.** Pulse **Ver soporte**. Se abre el PDF o la imagen en una pestaña nueva.

Si un gasto aparece con la etiqueta **Pagado por la empresa**, significa que no se pagó con el dinero del viaje. Ese gasto no se descuenta de la entrega del conductor.

### 2.6 Revisión de gastos

Esta pantalla muestra los gastos **pendientes** de decisión.

**Aprobar un gasto.** Revise la descripción, el valor y el comprobante con **Ver soporte**. Pulse **Aprobar**. El gasto pasa a *Aprobado*.

**Rechazar un gasto.** Pulse **Rechazar**. El sistema le pedirá el motivo. Escriba una observación clara (por ejemplo, *"Factura ilegible"*). Sin observación el rechazo no se guarda.

Usted no puede aprobar ni rechazar sus propios gastos. Un gasto solo se decide una vez.

En la parte inferior, **Historial de decisiones** muestra todos los gastos ya aprobados o rechazados, con su motivo.

### 2.7 Entregas de dinero

Muestra el dinero que los conductores entregaron a la empresa.

**Registrar una entrega recibida.** Use el formulario de la derecha: elija el conductor, el viaje (opcional, o *Entrega general*), el valor entregado, la referencia del recibo y las observaciones. Pulse **Registrar entrega recibida**.

**Verificar una entrega reportada por un conductor.** Cuando un conductor reporta una entrega, aparece como *Registrada*. Cuente el dinero y pulse **Verificar**. Solo las entregas **verificadas** descuentan del saldo pendiente.

### 2.8 Conciliación

Para cada viaje muestra:

| Columna | Significado |
|---|---|
| **Ingreso** | Valor cobrado del viaje, si el viaje está completado. |
| **Gastos a descontar** | Gastos aprobados que se pagaron con el dinero del viaje. |
| **Pagados por la empresa** | Gastos aprobados que la empresa pagó. No se descuentan. |
| **Esperado** | Ingreso − gastos a descontar. Es el dinero que el conductor debe entregar. |
| **Entregado** | Dinero que el conductor ya entregó y fue verificado. |
| **Diferencia** | Esperado − entregado. |

Lectura de la diferencia:

- **Por entregar** (positivo): al conductor le falta entregar ese valor.
- **Conciliado** (cero): el conductor entregó exactamente lo que debía.
- **Entrega superior** (negativo): se entregó más de lo esperado. Revise el caso.

Si aparece el aviso **Cancelado con gastos aprobados: revisar**, el viaje fue cancelado pero tiene gastos aprobados. Decida si esos gastos deben mantenerse.

### 2.9 Reportes

1. Entre a **Reportes**.
2. Elija la fecha inicial y la fecha final del periodo.
3. Pulse **Aplicar periodo**.

El reporte resume ingresos, gastos reportados, gastos aprobados, gastos pendientes, entregas verificadas y entregas pendientes. Incluye un detalle por vehículo, por conductor y por categoría.

Pulse **Exportar CSV** para descargar el reporte y abrirlo en Excel.

---

## 3. Tareas del conductor

### 3.1 Mis viajes

Muestra solo los viajes que le fueron asignados: código, ruta, fecha de salida, vehículo, valor y estado. Consulte aquí los datos antes de salir.

### 3.2 Registrar un gasto

1. Entre a **Registrar gasto**.
2. Elija la **categoría** (combustible, peajes, parqueadero, lavado, mantenimiento, reparaciones u otros gastos autorizados).
3. Elija el **vehículo**. Solo aparecen los vehículos que puede usar.
4. Elija el **viaje**, si el gasto corresponde a uno. Si no, deje *Sin viaje asociado*.
5. Escriba el **valor** y una **descripción** clara (por ejemplo, *"Tanqueada en estación Terpel"*).
6. Revise la fecha y hora.
7. Adjunte el **comprobante**: un PDF, JPG o PNG de máximo 5 MB.
8. Revise la casilla **Pagado con dinero recaudado en el viaje**.
9. Pulse **Registrar gasto**.

**La casilla es importante.** Si el gasto lo pagó con el dinero que cobró en el viaje, déjela marcada: ese valor se descuenta de lo que debe entregar. Si lo pagó la empresa directamente, desmárquela: el gasto queda registrado pero no se descuenta de su entrega.

Su gasto queda como **Pendiente** hasta que el administrador lo revise.

### 3.3 Mis gastos

Muestra todos sus gastos con su estado:

- **Pendiente:** esperando revisión.
- **Aprobado:** fue aceptado.
- **Rechazado:** fue rechazado. La columna *Observación* explica el motivo. Puede registrar el gasto de nuevo con el soporte correcto.

Pulse **Ver soporte** para abrir el comprobante que adjuntó.

### 3.4 Reportar una entrega

1. Entre a **Reportar entrega**.
2. Elija el viaje al que corresponde la entrega, o deje *Entrega general*.
3. Escriba el valor que entrega, la referencia (si tiene un número de recibo) y observaciones.
4. Pulse **Enviar entrega a revisión**.

La entrega queda como **Registrada** hasta que el administrador la verifique. Solo entonces descuenta de su saldo pendiente.

### 3.5 Mi conciliación

Muestra, para cada uno de sus viajes, cuánto debe entregar. Las columnas son las mismas que ve el administrador (ver sección 2.8).

Si la diferencia aparece como **Por entregar**, ese es el valor que debe llevar a la empresa. Si aparece como **Conciliado**, sus cuentas están al día.

---

## 4. Preguntas frecuentes

**¿Por qué mi gasto aparece como "Pendiente"?**
Todos los gastos nuevos quedan pendientes hasta que el administrador los revise. Mientras tanto no se descuentan de su entrega.

**¿Por qué el valor que debo entregar no coincide con lo que recaudé?**
Porque se descuentan solo los gastos aprobados que se pagaron con el dinero del viaje. Revise en *Mis gastos* el estado de cada uno.

**Adjunté un comprobante y no se guardó.**
Verifique que el archivo sea PDF, JPG o PNG y que pese menos de 5 MB.

**Olvidé mi contraseña.**
Comuníquese con el administrador para que le asigne una nueva.

**Un gasto fue rechazado. ¿Qué hago?**
Lea la observación en *Mis gastos*. Corrija lo que se indicó y regístrelo de nuevo con el comprobante correcto.

---

## 5. Buenas prácticas

- Registre los gastos el mismo día en que los pague.
- Adjunte siempre el comprobante. Un gasto sin soporte puede ser rechazado.
- Verifique la casilla de *pagado con dinero recaudado* antes de guardar.
- Revise cada semana la sección *Mi conciliación*.
- No comparta su contraseña.
