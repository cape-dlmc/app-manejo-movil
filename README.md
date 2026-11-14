# FleetControl

Sistema web para administrar vehículos, conductores, viajes, gastos con comprobante,
revisión de gastos, entregas de dinero, conciliación y reportes de una pequeña empresa de transporte.

| Capa | Tecnología | Carpeta |
|---|---|---|
| Base de datos | PostgreSQL 14 o superior | `database/` |
| Backend | Java 17, Spring Boot 3.3, Spring Security con JWT, Spring Data JPA | `backend/` |
| Frontend | HTML5, CSS3 y JavaScript (sin frameworks) | `frontend/` |

---

## 1. Requisitos

| Herramienta | Versión | Cómo verificarla |
|---|---|---|
| Java JDK | 17 o superior | `java -version` |
| Maven | 3.9 o superior | `mvn -version` |
| PostgreSQL | 14 o superior | `psql --version` |
| Navegador | Chrome, Edge o Firefox | - |
| Live Server | Extensión de VS Code | - |

---

## 2. Crear la base de datos

### Instalación nueva

Abra PowerShell en la carpeta del proyecto:

```powershell
psql -U postgres -c "CREATE DATABASE fleetcontrol ENCODING 'UTF8';"
psql -U postgres -d fleetcontrol -f database\fleetcontrol_schema.sql
psql -U postgres -d fleetcontrol -c "\dt"
```

El último comando debe listar **9 tablas**.

### Base existente de una versión anterior

Ejecute una sola vez la migración, que agrega la columna **pagado_con_recaudo**:

```powershell
psql -U postgres -d fleetcontrol -f database\migration_entregas_conductor.sql
```

Es seguro ejecutarla más de una vez: no borra datos.

---

## 3. Ejecutar las pruebas del backend

```powershell
cd backend
mvn clean test
```

Debe aparecer `Tests run: 3, Failures: 0, Errors: 0`.

---

## 4. Ejecutar el backend

Desde la carpeta `backend`:

```powershell
$env:DB_PASSWORD = "su_clave_de_postgres"
$env:JWT_SECRET  = "una-clave-secreta-de-al-menos-32-caracteres"
mvn clean spring-boot:run
```

Espere a ver la línea `Started FleetControlApplication`. La API queda disponible en
`http://localhost:8080/api`.

Si Maven se queda sin memoria, cierre otras aplicaciones y ejecute antes:

```powershell
$env:MAVEN_OPTS = "-Xmx1024m"
```

### Cuentas de demostración

La primera vez que arranca, con la base vacía, se crean estas cuentas:

| Rol | Correo | Contraseña |
|---|---|---|
| Administrador | `admin@fleetcontrol.local` | `Admin123*` |
| Conductor | `conductor@fleetcontrol.local` | `Conductor123*` |

Cambie las contraseñas antes de usar datos reales. Para definir otras, use las variables
`ADMIN_EMAIL` y `ADMIN_PASSWORD` antes de arrancar.

---

## 5. Ejecutar el frontend

1. Abra la carpeta `frontend` en Visual Studio Code.
2. Haga clic derecho sobre `index.html` y elija **Open with Live Server**.
3. Se abre en `http://127.0.0.1:5500`.

Si Live Server usa otro puerto, agréguelo en `app.cors.allowed-origins` dentro de
`backend/src/main/resources/application.properties` y reinicie el backend.

---

## 6. Recorrido para probar el sistema

Siga este orden. Cada paso corresponde a una prueba del documento del microproyecto.

1. Ingrese con una contraseña incorrecta. Debe mostrar un mensaje de error (prueba 1).
2. Ingrese como **conductor**. Revise que solo vea sus viajes (prueba 2).
3. Como conductor, registre un gasto con valor `0`. Debe rechazarlo (prueba 3).
4. Registre un gasto válido con un PDF adjunto. Queda **Pendiente** (prueba 4).
5. Cierre sesión e ingrese como **administrador**.
6. En **Revisión de gastos**, rechace un gasto sin motivo. Debe fallar.
7. Rechace el mismo gasto con motivo. Queda **Rechazado** con la observación (prueba 6).
8. Apruebe otro gasto. La conciliación del viaje cambia (prueba 5).
9. En **Entregas de dinero**, registre una entrega menor al esperado. La diferencia aparece (prueba 7).
10. Registre una entrega exacta en otro viaje. La conciliación queda en cero (prueba 8).
11. Intente subir un comprobante de más de 5 MB. Debe rechazarlo (prueba 11).
12. Cierre sesión, vuelva a ingresar y confirme que los datos siguen ahí (prueba 11).
13. En **Reportes**, aplique un periodo y exporte el CSV.

---

## 7. Reglas de negocio implementadas

| Regla | Dónde se aplica |
|---|---|
| RN-01 Valores positivos | `@Positive` en los DTOs y `CHECK` en PostgreSQL |
| RN-02 Asociación de gastos | `MovimientoService.registrarGasto` |
| RN-04 Sin autoaprobación | `MovimientoService.revisar` y `CHECK chk_no_autoaprobacion` |
| RN-07 y sección 12.2 Dinero esperado | `ConciliacionService.conciliar` |
| RN-08 Gastos pendientes | Solo los gastos `APROBADO` se descuentan |
| RN-09 Diferencias | `ConciliacionService.conciliar` |
| RN-10 Acceso por rol | `@PreAuthorize`, filtros en los servicios y `ComprobanteService` |
| RN-12 Vehículo inactivo | `ViajeService.crear` y `MovimientoService.registrarGasto` |
| RN-13 Viaje cancelado | Solo los viajes `COMPLETADO` generan ingreso. Los cancelados con gastos aprobados se marcan para revisión |
| Gasto pagado por la empresa | Campo `pagado_con_recaudo`. Si es `false`, el gasto no se descuenta de la entrega |
| Rechazo con motivo | `MovimientoService.revisar` y `CHECK chk_rechazo_con_motivo` |

---

## 8. Estructura del proyecto

```
fleetcontrol/
├── README.md
├── MANUAL_USUARIO.md
├── .gitignore
├── database/
│   ├── fleetcontrol_schema.sql          Esquema completo (instalación nueva)
│   └── migration_entregas_conductor.sql Migración para bases existentes
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/fleetcontrol/
│       │   ├── config/        Seguridad, CORS y carga inicial de datos
│       │   ├── security/      JWT: generación, validación y filtro
│       │   ├── controller/    Endpoints REST
│       │   ├── service/       Reglas de negocio y cálculos
│       │   ├── repository/    Acceso a datos
│       │   ├── entity/        Entidades alineadas con el esquema SQL
│       │   ├── dto/           Contratos de entrada y salida
│       │   └── exception/     Errores uniformes
│       ├── main/resources/application.properties
│       └── test/java/         Pruebas de reportes
└── frontend/
    ├── index.html
    ├── styles.css
    └── app.js
```

---

## 9. Endpoints principales

| Método | Ruta | Acceso |
|---|---|---|
| POST | `/api/auth/login` | Público |
| GET | `/api/auth/me` | Autenticado |
| GET, POST, PUT | `/api/vehiculos` | Lectura: todos. Escritura: admin |
| GET, POST, PUT | `/api/conductores` | Admin |
| PATCH | `/api/conductores/{id}/estado` | Admin |
| GET, POST | `/api/viajes` | Lectura filtrada por rol. Escritura: admin |
| PATCH | `/api/viajes/{id}/estado` | Admin |
| GET, POST | `/api/movimientos` | Lectura filtrada por rol |
| PATCH | `/api/movimientos/{id}/revision` | Admin |
| POST | `/api/movimientos/{id}/comprobantes` | Dueño del gasto o admin |
| GET | `/api/comprobantes/{id}` | Dueño del gasto o admin |
| GET, POST | `/api/entregas` | Lectura filtrada por rol. Registro: conductor o admin |
| PATCH | `/api/entregas/{id}/verificar` | Admin |
| GET | `/api/conciliacion/viajes` | Filtrada por rol |
| GET | `/api/dashboard` | Admin |
| GET | `/api/reportes?desde=&hasta=` | Admin |
| GET | `/api/categorias` | Autenticado |

---

## 10. Solución de problemas

| Síntoma | Causa probable | Solución |
|---|---|---|
| `Connection refused` al arrancar | PostgreSQL no está corriendo | Inicie el servicio `postgresql` desde *Servicios* de Windows |
| `password authentication failed` | Clave incorrecta | Revise `DB_PASSWORD` en la misma ventana de PowerShell |
| `relation "usuarios" does not exist` | Falta ejecutar el esquema | Ejecute `fleetcontrol_schema.sql` |
| `column "pagado_con_recaudo" does not exist` | Base de una versión anterior | Ejecute `migration_entregas_conductor.sql` |
| El frontend no conecta con la API | Puerto o CORS | Revise `app.cors.allowed-origins` y que el backend esté corriendo en el 8080 |
| `OutOfMemoryError` al compilar | Poca memoria disponible | Cierre aplicaciones y use `$env:MAVEN_OPTS = "-Xmx1024m"` |
| Sesión expirada al poco tiempo | El token dura 8 horas por defecto | Vuelva a ingresar. El tiempo se configura en `app.jwt.expiration-minutes` |

---

## 11. Limitaciones conocidas

- Los reportes leen todos los registros y filtran en memoria. Es adecuado para una flota de 1 a 5 vehículos, pero no para volúmenes grandes.
- Los comprobantes se guardan en `./storage/comprobantes` dentro del backend. En producción deben guardarse en un almacenamiento privado fuera del proyecto.
- Las contraseñas de demostración y la clave JWT por defecto son solo para pruebas.
- No hay recuperación de contraseña por correo. El administrador reasigna las contraseñas.
