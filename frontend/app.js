/* FleetControl: cliente web. Se comunica solo con la API del backend (RNF-04). */
const API = (window.FC_CONFIG && window.FC_CONFIG.apiUrl) || 'http://localhost:8080/api';
const ESTADOS_VEHICULO = ['DISPONIBLE', 'EN_SERVICIO', 'MANTENIMIENTO', 'INACTIVO'];
const ESTADOS_VIAJE = ['PROGRAMADO', 'EN_CURSO', 'COMPLETADO', 'CANCELADO'];
const TIPOS_SERVICIO = ['INTERMUNICIPAL', 'PUERTA_A_PUERTA'];

const MENU = {
  ADMIN: [
    ['dashboard', 'Resumen'], ['vehiculos', 'Vehículos'], ['conductores', 'Conductores'],
    ['viajes', 'Viajes'], ['movimientos', 'Movimientos financieros'],
    ['revisiones', 'Revisión de gastos'], ['entregas', 'Entregas de dinero'],
    ['conciliacion', 'Conciliación'], ['reportes', 'Reportes']
  ],
  CONDUCTOR: [
    ['viajes', 'Mis viajes'], ['gasto', 'Registrar gasto'],
    ['movimientos', 'Mis gastos'], ['entregas', 'Reportar entrega'], ['conciliacion', 'Mi conciliación']
  ]
};
const TITULOS = Object.fromEntries(Object.values(MENU).flat());
TITULOS.historial = 'Historial financiero';

let sesion = JSON.parse(sessionStorage.getItem('fc_sesion') || 'null');
let vehiculoEditandoId = null;
let conductorEditandoId = null;
let periodoReporte = null;
let reporteActual = null;

/* ---------- Utilidades ---------- */
const $ = (selector) => document.querySelector(selector);
const esc = (t) => String(t ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const money = (v) => new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 }).format(Number(v ?? 0));
const fecha = (f) => (f ? new Date(f).toLocaleString('es-CO', { dateStyle: 'medium', timeStyle: 'short' }) : '—');
const etiqueta = (t) => String(t ?? '').replaceAll('_', ' ').toLowerCase().replace(/^./, (c) => c.toUpperCase());
const opciones = (lista, seleccionado = '') => lista.map((v) => `<option value="${esc(v)}" ${v === seleccionado ? 'selected' : ''}>${esc(etiqueta(v))}</option>`).join('');
const badge = (estado) => `<span class="estado estado-${esc(estado)}">${esc(etiqueta(estado))}</span>`;
const ahoraLocal = () => { const d = new Date(); d.setMinutes(d.getMinutes() - d.getTimezoneOffset()); return d.toISOString().slice(0, 16); };
const fechaLocal = () => ahoraLocal().slice(0, 10);
const leer = (form) => Object.fromEntries(new FormData(form));
const pintar = (html) => { $('#vista').innerHTML = html; };
const vacio = (texto) => `<tr><td class="vacio" colspan="20">${esc(texto)}</td></tr>`;
const tabla = (columnas, filas, textoVacio, id = '') => `
  <div class="tabla-wrap"><table ${id ? `id="${esc(id)}"` : ''}>
    <thead><tr>${columnas.map((c) => `<th>${c}</th>`).join('')}</tr></thead>
    <tbody>${filas || vacio(textoVacio)}</tbody>
  </table></div>`;

function mostrarOk(mensaje) { mostrarAviso(mensaje, 'ok'); }
function mostrarError(mensaje) { mostrarAviso(mensaje, 'error'); }
function mostrarAviso(mensaje, tipo) {
  const n = $('#notificacion');
  n.textContent = mensaje;
  n.className = `notificacion ${tipo}`;
  clearTimeout(mostrarAviso.temporizador);
  mostrarAviso.temporizador = setTimeout(() => n.classList.add('hidden'), 4500);
}

/* ---------- Comunicación con la API ---------- */
async function api(ruta, { method = 'GET', body, form } = {}) {
  const headers = {};
  if (sesion) headers.Authorization = `Bearer ${sesion.token}`;
  let payload;
  if (form) {
    payload = form;                                  // multipart: el navegador asigna el Content-Type
  } else if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
    payload = JSON.stringify(body);
  }
  const res = await fetch(API + ruta, { method, headers, body: payload });
  if (res.status === 401 && sesion && ruta !== '/auth/login') {
    salir();
    throw new Error('Su sesión expiró. Ingrese de nuevo.');
  }
  if (res.status === 204) return null;
  const datos = await res.json().catch(() => null);
  if (!res.ok) throw new Error(datos?.error || `Error ${res.status}`);
  return datos;
}

async function verSoporte(id) {
  const res = await fetch(`${API}/comprobantes/${id}`, { headers: { Authorization: `Bearer ${sesion.token}` } });
  if (!res.ok) {
    const datos = await res.json().catch(() => null);
    throw new Error(datos?.error || 'No se pudo abrir el comprobante');
  }
  window.open(URL.createObjectURL(await res.blob()), '_blank');
}

const botonSoporte = (m) => (m.comprobanteIds?.length
  ? m.comprobanteIds.map((id) => `<button class="btn mini secundario" data-accion="soporte" data-id="${id}">Ver soporte</button>`).join('')
  : '<small>Sin soporte</small>');

/* ---------- Sesión y navegación ---------- */
function mostrarLogin() {
  $('#app').classList.add('hidden');
  $('#login').classList.remove('hidden');
}

function mostrarApp() {
  $('#login').classList.add('hidden');
  $('#app').classList.remove('hidden');
  $('#usuario-nombre').textContent = sesion.nombre;
  $('#rol-badge').textContent = sesion.rol === 'ADMIN' ? 'Administrador' : 'Conductor';
  $('#nav').innerHTML = MENU[sesion.rol].map(([id, texto]) => `<button data-nav="${id}">${ICONOS[id] ?? ''}<span>${texto}</span></button>`).join('');
  $('#avatar-ini').textContent = sesion.nombre.split(' ').filter(Boolean).slice(0, 2).map((p) => p[0]).join('').toUpperCase();
  $('#usuario-rol').textContent = sesion.rol === 'ADMIN' ? 'Administrador' : 'Conductor';
  ir(sesion.rol === 'ADMIN' ? 'dashboard' : 'viajes');
}

function salir() {
  sesion = null;
  sessionStorage.removeItem('fc_sesion');
  mostrarLogin();
}

async function ir(id) {
  document.querySelectorAll('[data-nav]').forEach((b) => b.classList.toggle('activo', b.dataset.nav === id));
  const elementoMenu = MENU[sesion.rol].find(([vista]) => vista === id);
  $('#titulo').textContent = elementoMenu ? elementoMenu[1] : (TITULOS[id] ?? '');
  $('#migas-actual').textContent = $('#titulo').textContent;
  pintar(`<div class="cargando" role="status" aria-live="polite">
    <span class="spinner" aria-hidden="true"></span><span>Cargando información…</span></div>`);
  try {
    await VISTAS[id]();
  } catch (e) {
    pintar(`<div class="card estado-error-vista" role="alert">
      <h2>No pudimos cargar esta sección</h2>
      <p class="nota">${esc(e.message)}</p>
      <button class="btn primario" data-accion="reintentar" data-vista="${esc(id)}">Reintentar</button>
    </div>`);
    mostrarError(e.message);
  }
}

/* ---------- Vistas del administrador ---------- */
const svg = (d) => `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${d}</svg>`;
const ICONOS = {
  dashboard: svg('<rect x="3" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="3" width="7" height="7" rx="1.5"/><rect x="3" y="14" width="7" height="7" rx="1.5"/><rect x="14" y="14" width="7" height="7" rx="1.5"/>'),
  vehiculos: svg('<path d="M3 16v-3.5a1.5 1.5 0 0 1 .4-1L6 7.5a2 2 0 0 1 1.7-1h8.6a2 2 0 0 1 1.7 1l2.6 4a1.5 1.5 0 0 1 .4 1V16"/><path d="M3 16h18"/><circle cx="7" cy="16" r="2"/><circle cx="17" cy="16" r="2"/>'),
  conductores: svg('<circle cx="9" cy="8" r="3.5"/><path d="M2.5 20c.6-3.4 3.2-5.5 6.5-5.5s5.9 2.1 6.5 5.5"/><path d="M16 4.6a3.5 3.5 0 0 1 0 6.8"/><path d="M18 14.7c1.6.8 2.6 2.7 2.8 5.3"/>'),
  viajes: svg('<circle cx="6" cy="6" r="2.5"/><circle cx="18" cy="18" r="2.5"/><path d="M8.5 6h5.5a3.5 3.5 0 0 1 0 7h-3a3.5 3.5 0 0 0 0 7h5.5"/>'),
  movimientos: svg('<path d="M6 3h12v18l-2-1.5L14 21l-2-1.5L10 21l-2-1.5L6 21z"/><path d="M9 8h6M9 12h6"/>'),
  revisiones: svg('<circle cx="12" cy="12" r="9"/><path d="M8 12.5l2.5 2.5L16 9.5"/>'),
  entregas: svg('<rect x="3" y="6" width="18" height="13" rx="2.5"/><path d="M3 10h18"/><path d="M16 14.5h2"/>'),
  conciliacion: svg('<path d="M12 3l9 5-9 5-9-5 9-5z"/><path d="M3 13l9 5 9-5"/>'),
  reportes: svg('<path d="M4 20V10M10 20V4M16 20v-7M22 20H2"/>'),
  gasto: svg('<circle cx="12" cy="12" r="9"/><path d="M12 8v8M8 12h8"/>'),
  ingresos: svg('<path d="M3 17l6-6 4 4 8-8"/><path d="M14 7h7v7"/>'),
  egresos: svg('<path d="M6 3h12v18l-2-1.5L14 21l-2-1.5L10 21l-2-1.5L6 21z"/><path d="M9 8h6M9 12h6"/>'),
  alerta: svg('<path d="M12 9v4M12 17h.01"/><path d="M10.3 3.9L2.6 17.3A2 2 0 0 0 4.3 20h15.4a2 2 0 0 0 1.7-2.7L13.7 3.9a2 2 0 0 0-3.4 0z"/>')
};

function kpi(etiquetaTexto, valor, clase = '', icono = '') {
  return `<div class="kpi ${clase}">
    <div class="kpi-top"><span>${etiquetaTexto}</span>${icono ? `<i class="kpi-icono">${icono}</i>` : ''}</div>
    <strong>${valor}</strong>
  </div>`;
}

async function vistaDashboard() {
  const d = await api('/dashboard');
  const hayRevision = d.gastosPendientes > 0;
  const hayConciliar = Number(d.pendienteConciliar) > 0;
  const atencion = [
    hayRevision ? `<button class="atencion-item" data-accion="irA" data-id="revisiones">
        <i class="atencion-icono alerta">${ICONOS.alerta}</i>
        <span><strong>${d.gastosPendientes} gasto(s) por revisar</strong><small>Aprueba o rechaza los gastos reportados</small></span>
      </button>` : '',
    hayConciliar ? `<button class="atencion-item" data-accion="irA" data-id="conciliacion">
        <i class="atencion-icono">${ICONOS.entregas}</i>
        <span><strong>${money(d.pendienteConciliar)} por conciliar</strong><small>Dinero pendiente de entregar o verificar</small></span>
      </button>` : ''
  ].join('');

  pintar(`
    <div class="kpis">
      ${kpi('Vehículos activos', d.vehiculosActivos, '', ICONOS.vehiculos)}
      ${kpi('Viajes completados', d.viajesCompletados, '', ICONOS.viajes)}
      ${kpi('Ingresos registrados', money(d.ingresos), '', ICONOS.ingresos)}
      ${kpi('Egresos aprobados', money(d.egresosAprobados), '', ICONOS.egresos)}
      ${kpi('Pendiente por conciliar', money(d.pendienteConciliar), '', ICONOS.entregas)}
      ${kpi('Gastos pendientes de revisión', d.gastosPendientes, hayRevision ? 'alerta' : '', ICONOS.alerta)}
    </div>
    <div class="grid-2">
      <section class="card">
        <div class="card-cabecera">
          <div><h2>Requiere atención</h2><small>Pendientes de la operación</small></div>
          ${hayRevision || hayConciliar ? `<span class="contador">${(hayRevision ? 1 : 0) + (hayConciliar ? 1 : 0)}</span>` : ''}
        </div>
        <div class="atencion-lista">
          ${atencion || '<p class="nota">Todo está al día: no hay gastos ni saldos pendientes.</p>'}
        </div>
      </section>
      <section class="card">
        <div class="card-cabecera"><div><h2>Acciones rápidas</h2><small>Lo que más se usa en la operación</small></div></div>
        <div class="atajos">
          <button class="atajo" data-accion="irA" data-id="vehiculos">${ICONOS.vehiculos}<span>Vehículos</span></button>
          <button class="atajo" data-accion="irA" data-id="viajes">${ICONOS.viajes}<span>Programar viaje</span></button>
          <button class="atajo" data-accion="irA" data-id="revisiones">${ICONOS.revisiones}<span>Revisar gastos</span></button>
          <button class="atajo" data-accion="irA" data-id="reportes">${ICONOS.reportes}<span>Ver reportes</span></button>
        </div>
      </section>
    </div>
    <p class="nota nota-pie">Los gastos pendientes no se descuentan en la conciliación hasta que sean aprobados (RN-08).</p>`);
}

async function vistaVehiculos() {
  const lista = await api('/vehiculos');
  pintar(`
    <div class="grid-2">
      <section class="card">
        <h2>Vehículos</h2>
        <label style="margin-bottom:.75rem">Buscar por placa
          <input id="buscar" placeholder="Ej.: TLM421" autocomplete="off">
        </label>
        ${tabla(['Placa', 'Vehículo', 'Año', 'Estado', 'Acciones'], lista.map((v) => `
          <tr data-placa="${esc(v.placa)}">
            <td><strong>${esc(v.placa)}</strong></td>
            <td>${esc(v.marca)} ${esc(v.modelo)}</td>
            <td>${v.anio}</td>
            <td>${badge(v.estado)}</td>
            <td><button class="btn mini secundario" data-accion="editar-vehiculo" data-id="${v.id}">Editar</button>
                <button class="btn mini secundario" data-accion="historial-vehiculo" data-id="${v.id}">Historial</button></td>
          </tr>`).join(''), 'Aún no hay vehículos registrados')}
      </section>
      <section class="card">
        <h2 id="titulo-form-vehiculo">Registrar vehículo</h2>
        <form id="form-vehiculo" class="form">
          <label>Placa <input name="placa" required maxlength="10"></label>
          <label>Marca <input name="marca" required></label>
          <label>Modelo <input name="modelo" required></label>
          <label>Año <input name="anio" type="number" min="1990" max="2100" required></label>
          <label>Estado <select name="estado">${opciones(ESTADOS_VEHICULO)}</select></label>
          <label>Observaciones <textarea name="observaciones"></textarea></label>
          <button id="guardar-vehiculo" class="btn primario">Guardar vehículo</button>
          <button id="cancelar-edicion-vehiculo" type="button" class="btn secundario hidden">Cancelar edición</button>
        </form>
      </section>
    </div>`);

  $('#buscar').addEventListener('input', (e) => {
    const texto = e.target.value.trim().toUpperCase().replace(/\s/g, '');
    document.querySelectorAll('tr[data-placa]').forEach((fila) => {
      fila.style.display = fila.dataset.placa.replace(/\s/g, '').includes(texto) ? '' : 'none';
    });
  });

  $('#form-vehiculo').addEventListener('submit', async (e) => {
    e.preventDefault();
    const d = leer(e.target);
    try {
      const actualizando = vehiculoEditandoId !== null;
      const ruta = actualizando ? `/vehiculos/${vehiculoEditandoId}` : '/vehiculos';
      await api(ruta, { method: actualizando ? 'PUT' : 'POST', body: { ...d, anio: Number(d.anio) } });
      mostrarOk(actualizando ? 'Vehículo actualizado' : 'Vehículo registrado');
      vehiculoEditandoId = null;
      ir('vehiculos');
    } catch (err) { mostrarError(err.message); }
  });
  $('#cancelar-edicion-vehiculo').addEventListener('click', () => {
    vehiculoEditandoId = null;
    ir('vehiculos');
  });
}

async function vistaConductores() {
  const lista = await api('/conductores');
  pintar(`
    <div class="grid-2">
      <section class="card">
        <h2>Conductores</h2>
        ${tabla(['Nombre', 'Correo', 'Identificación', 'Estado', 'Acciones'], lista.map((c) => `
          <tr>
            <td>${esc(c.nombre)}</td>
            <td>${esc(c.correo)}</td>
            <td>${esc(c.identificacion)}</td>
            <td>${badge(c.estado)}</td>
            <td><button class="btn mini secundario" data-accion="editar-conductor" data-id="${c.id}">Editar</button>
                <button class="btn mini secundario" data-accion="conductor-estado" data-id="${c.id}"
                 data-valor="${c.estado === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO'}">
                 ${c.estado === 'ACTIVO' ? 'Desactivar' : 'Activar'}</button></td>
          </tr>`).join(''), 'Aún no hay conductores registrados')}
      </section>
      <section class="card">
        <h2 id="titulo-form-conductor">Registrar conductor</h2>
        <form id="form-conductor" class="form">
          <label>Nombre completo <input name="nombre" required></label>
          <label>Correo de acceso <input type="email" name="correo" required></label>
          <label>Contraseña temporal (mín. 8) <input type="password" name="passwordTemporal" minlength="8" required></label>
          <label>Identificación <input name="identificacion" required></label>
          <label>Teléfono <input name="telefono"></label>
          <button id="guardar-conductor" class="btn primario">Guardar conductor</button>
          <button id="cancelar-edicion-conductor" type="button" class="btn secundario hidden">Cancelar edición</button>
        </form>
      </section>
    </div>`);

  $('#form-conductor').addEventListener('submit', async (e) => {
    e.preventDefault();
    const d = leer(e.target);
    if (!d.passwordTemporal) delete d.passwordTemporal;
    try {
      const actualizando = conductorEditandoId !== null;
      await api(actualizando ? `/conductores/${conductorEditandoId}` : '/conductores', {
        method: actualizando ? 'PUT' : 'POST',
        body: d
      });
      mostrarOk(actualizando ? 'Conductor actualizado' : 'Conductor registrado');
      conductorEditandoId = null;
      ir('conductores');
    } catch (err) { mostrarError(err.message); }
  });
  $('#cancelar-edicion-conductor').addEventListener('click', () => {
    conductorEditandoId = null;
    ir('conductores');
  });
}

const botonesViaje = (v) => {
  const b = (accion, texto, clase = 'secundario') =>
    `<button class="btn mini ${clase}" data-accion="viaje-estado" data-id="${v.id}" data-valor="${accion}">${texto}</button>`;
  if (v.estado === 'PROGRAMADO') return b('EN_CURSO', 'Iniciar') + b('CANCELADO', 'Cancelar', 'rechazar');
  if (v.estado === 'EN_CURSO') return b('COMPLETADO', 'Completar', 'aprobar') + b('CANCELADO', 'Cancelar', 'rechazar');
  return '—';
};

function filasViajes(viajes, esAdmin) {
  return viajes.map((v) => `
    <tr>
      <td><strong>${esc(v.codigo)}</strong></td>
      <td>${esc(v.origen)} → ${esc(v.destino)}<br><small>${esc(etiqueta(v.tipoServicio))}</small></td>
      <td>${fecha(v.fechaSalida)}</td>
      <td>${esc(v.conductor)}</td>
      <td>${esc(v.placa)}</td>
      <td>${money(v.valorCobrado)}</td>
      <td>${badge(v.estado)}</td>
      ${esAdmin ? `<td class="acciones">${botonesViaje(v)}</td>` : ''}
    </tr>`).join('');
}

async function vistaViajes() {
  const esAdmin = sesion.rol === 'ADMIN';
  const [viajes, vehiculos, conductores] = await Promise.all([
    api('/viajes'),
    esAdmin ? api('/vehiculos') : [],
    esAdmin ? api('/conductores') : []
  ]);

  const columnas = ['Código', 'Ruta', 'Salida', 'Conductor', 'Placa', 'Valor', 'Estado', ...(esAdmin ? ['Acciones'] : [])];

  pintar(`
    <div class="${esAdmin ? 'grid-2' : ''}">
      <section class="card">
        <h2>${esAdmin ? 'Viajes' : 'Mis viajes asignados'}</h2>
        <form id="filtro-viajes" class="form filtro-lista">
          <label>Desde <input type="date" name="desde"></label>
          <label>Hasta <input type="date" name="hasta"></label>
          <label>Estado <select name="estado"><option value="">Todos</option>${opciones(ESTADOS_VIAJE)}</select></label>
          <label>Tipo <select name="tipoServicio"><option value="">Todos</option>${opciones(TIPOS_SERVICIO)}</select></label>
          ${esAdmin ? `
          <label>Vehículo <select name="vehiculoId"><option value="">Todos</option>
            ${vehiculos.map((v) => `<option value="${v.id}">${esc(v.placa)}</option>`).join('')}
          </select></label>
          <label>Conductor <select name="conductorId"><option value="">Todos</option>
            ${conductores.map((c) => `<option value="${c.id}">${esc(c.nombre)}</option>`).join('')}
          </select></label>` : ''}
          <button class="btn secundario">Filtrar viajes</button>
        </form>
        ${tabla(columnas, filasViajes(viajes, esAdmin), 'No hay viajes registrados', 'tabla-viajes')}
      </section>
      ${esAdmin ? `
      <section class="card">
        <h2>Programar viaje</h2>
        <form id="form-viaje" class="form">
          <label>Código <input name="codigo" required placeholder="VJ-1049"></label>
          <label>Vehículo
            <select name="vehiculoId" required>
              ${vehiculos.filter((v) => v.estado !== 'INACTIVO')
                .map((v) => `<option value="${v.id}">${esc(v.placa)} · ${esc(v.marca)} ${esc(v.modelo)}</option>`).join('')}
            </select>
          </label>
          <label>Conductor
            <select name="conductorId" required>
              ${conductores.filter((c) => c.estado === 'ACTIVO')
                .map((c) => `<option value="${c.id}">${esc(c.nombre)}</option>`).join('')}
            </select>
          </label>
          <label>Tipo de servicio <select name="tipoServicio">${opciones(TIPOS_SERVICIO)}</select></label>
          <label>Origen <input name="origen" required></label>
          <label>Destino <input name="destino" required></label>
          <label>Fecha y hora de salida <input type="datetime-local" name="fechaSalida" required value="${ahoraLocal()}"></label>
          <label>Valor cobrado (COP) <input type="number" name="valorCobrado" min="1" step="100" required></label>
          <label>Observaciones <textarea name="observaciones"></textarea></label>
          <button class="btn primario">Guardar viaje</button>
        </form>
      </section>` : ''}
    </div>`);

  $('#filtro-viajes').addEventListener('submit', async (e) => {
    e.preventDefault();
    const parametros = new URLSearchParams(Object.entries(leer(e.target)).filter(([, valor]) => valor));
    try {
      const resultados = await api(`/viajes?${parametros}`);
      $('#tabla-viajes tbody').innerHTML = filasViajes(resultados, esAdmin) || vacio('No hay viajes para estos filtros');
    } catch (err) { mostrarError(err.message); }
  });

  if (esAdmin) {
    $('#form-viaje').addEventListener('submit', async (e) => {
      e.preventDefault();
      const d = leer(e.target);
      try {
        await api('/viajes', {
          method: 'POST',
          body: {
            ...d,
            vehiculoId: Number(d.vehiculoId),
            conductorId: Number(d.conductorId),
            valorCobrado: Number(d.valorCobrado)
          }
        });
        mostrarOk('Viaje programado');
        ir('viajes');
      } catch (err) { mostrarError(err.message); }
    });
  }
}

async function vistaRevisiones() {
  const [pendientes, todos] = await Promise.all([api('/movimientos?estado=PENDIENTE'), api('/movimientos')]);
  const decididos = todos.filter((m) => m.estado !== 'PENDIENTE');

  pintar(`
    <section class="card">
      <h2>Gastos pendientes (${pendientes.length})</h2>
      ${tabla(['Fecha', 'Conductor', 'Viaje', 'Gasto', 'Valor', 'Soporte', 'Decisión'], pendientes.map((m) => `
        <tr>
          <td>${fecha(m.fecha)}</td>
          <td>${esc(m.registradoPor)}</td>
          <td>${esc(m.codigoViaje || '—')}<br><small>${esc(m.placa)}</small></td>
          <td><strong>${esc(m.categoria)}</strong><br><small>${esc(m.descripcion)}</small>${etiquetaRecaudo(m)}</td>
          <td>${money(m.valor)}</td>
          <td>${botonSoporte(m)}</td>
          <td class="acciones">
            <button class="btn mini aprobar" data-accion="aprobar" data-id="${m.id}">Aprobar</button>
            <button class="btn mini rechazar" data-accion="rechazar" data-id="${m.id}">Rechazar</button>
          </td>
        </tr>`).join(''), 'No hay gastos pendientes de revisión')}
    </section>
    <section class="card" style="margin-top:1.25rem">
      <h2>Historial de decisiones</h2>
      ${tabla(['Fecha', 'Gasto', 'Valor', 'Decisión', 'Observación'], decididos.map((m) => `
        <tr>
          <td>${fecha(m.fecha)}</td>
          <td>${esc(m.categoria)} · ${esc(m.registradoPor)}</td>
          <td>${money(m.valor)}</td>
          <td>${badge(m.estado)}</td>
          <td>${esc(m.observacionRevision || '—')}</td>
        </tr>`).join(''), 'Aún no hay decisiones registradas')}
    </section>`);
}

async function vistaEntregas() {
  const esAdmin = sesion.rol === 'ADMIN';
  const [entregas, viajes, conciliaciones, conductores] = await Promise.all([
    api('/entregas'),
    api('/viajes'),
    api('/conciliacion/viajes'),
    esAdmin ? api('/conductores') : Promise.resolve([])
  ]);
  const viajesDisponibles = viajes.filter((v) => v.estado === 'COMPLETADO');

  if (!esAdmin) {
    pintar(`
      <section class="card">
        <h2>Mis entregas y saldos</h2>
        <p class="nota">El saldo esperado descuenta únicamente gastos aprobados. Las entregas reportadas quedan pendientes hasta que administración las verifique.</p>
        ${tabla(['Viaje', 'Ingreso', 'Gastos aprobados', 'Esperado', 'Entregado', 'Diferencia'], conciliaciones.map((x) => `
          <tr><td><strong>${esc(x.codigo)}</strong></td><td>${money(x.ingreso)}</td>
          <td>${money(x.gastosAprobados)}</td><td>${money(x.esperado)}</td>
          <td>${money(x.entregado)}</td><td>${money(x.diferencia)}</td></tr>`).join(''), 'Aún no hay viajes para conciliar')}
      </section>
      <div class="grid-2" style="margin-top:1rem">
        <section class="card">
          <h2>Mis entregas reportadas</h2>
            <form id="filtro-entregas" class="form filtro-lista">
              <label>Desde <input type="date" name="desde"></label>
              <label>Hasta <input type="date" name="hasta"></label>
              <label>Viaje <select name="viajeId"><option value="">Todos</option>
                ${viajes.map((v) => `<option value="${v.id}">${esc(v.codigo)}</option>`).join('')}
              </select></label>
              <label>Estado <select name="estado"><option value="">Todos</option>${opciones(['REGISTRADA', 'VERIFICADA'])}</select></label>
              <button class="btn secundario">Filtrar entregas</button>
            </form>
            ${tabla(['Fecha', 'Conductor', 'Viaje', 'Valor', 'Referencia', 'Estado', 'Verificado por'],
              filasEntregas(entregas, false), 'Aún no has reportado entregas', 'tabla-entregas')}
        </section>
        <section class="card">
          <h2>Reportar entrega</h2>
          <form id="form-entrega" class="form">
            <label>Viaje completado
              <select name="viajeId" required>
                <option value="">Seleccione un viaje</option>
                ${viajesDisponibles.map((v) => `<option value="${v.id}">${esc(v.codigo)} · ${esc(v.origen)} → ${esc(v.destino)}</option>`).join('')}
              </select>
            </label>
            <label>Dinero entregado (COP) <input type="number" name="valorEntregado" min="0.01" step="0.01" required></label>
            <label>Fecha y hora <input type="datetime-local" name="fechaEntrega" value="${ahoraLocal()}" required></label>
            <label>Referencia o recibo <input name="referencia" maxlength="100"></label>
            <label>Observaciones <textarea name="observaciones"></textarea></label>
            <button class="btn primario">Enviar entrega a revisión</button>
          </form>
        </section>
      </div>`);
  } else {
    pintar(`
      <div class="grid-2">
        <section class="card">
          <h2>Entregas recibidas</h2>
          <form id="filtro-entregas" class="form filtro-lista">
            <label>Desde <input type="date" name="desde"></label>
            <label>Hasta <input type="date" name="hasta"></label>
            <label>Conductor <select name="conductorId"><option value="">Todos</option>
              ${conductores.map((c) => `<option value="${c.id}">${esc(c.nombre)}</option>`).join('')}
            </select></label>
            <label>Viaje <select name="viajeId"><option value="">Todos</option>
              ${viajes.map((v) => `<option value="${v.id}">${esc(v.codigo)}</option>`).join('')}
            </select></label>
            <label>Estado <select name="estado"><option value="">Todos</option>${opciones(['REGISTRADA', 'VERIFICADA'])}</select></label>
            <button class="btn secundario">Filtrar entregas</button>
          </form>
          ${tabla(['Fecha', 'Conductor', 'Viaje', 'Valor', 'Referencia', 'Estado', 'Acción'],
            filasEntregas(entregas, true), 'Aún no hay entregas registradas', 'tabla-entregas')}
        </section>
        <section class="card">
          <h2>Registrar entrega recibida</h2>
          <form id="form-entrega" class="form">
            <label>Conductor
              <select name="conductorId" required>
                ${conductores.filter((c) => c.estado === 'ACTIVO')
                  .map((c) => `<option value="${c.id}">${esc(c.nombre)}</option>`).join('')}
              </select>
            </label>
            <label>Viaje (opcional)
              <select name="viajeId"><option value="">Entrega general</option>
                ${viajes.map((v) => `<option value="${v.id}">${esc(v.codigo)} · ${esc(v.conductor)}</option>`).join('')}
              </select>
            </label>
            <label>Valor entregado (COP) <input type="number" name="valorEntregado" min="0.01" step="0.01" required></label>
            <label>Fecha y hora <input type="datetime-local" name="fechaEntrega" value="${ahoraLocal()}" required></label>
            <label>Referencia o recibo <input name="referencia" maxlength="100"></label>
            <label>Observaciones <textarea name="observaciones"></textarea></label>
            <button class="btn primario">Registrar entrega recibida</button>
          </form>
        </section>
      </div>`);
  }

  $('#filtro-entregas').addEventListener('submit', async (e) => {
    e.preventDefault();
    const parametros = new URLSearchParams(Object.entries(leer(e.target)).filter(([, valor]) => valor));
    try {
      const resultados = await api(`/entregas?${parametros}`);
      $('#tabla-entregas tbody').innerHTML = filasEntregas(resultados, esAdmin)
        || vacio('No hay entregas para estos filtros');
    } catch (err) { mostrarError(err.message); }
  });

  $('#form-entrega').addEventListener('submit', async (e) => {
    e.preventDefault();
    const d = leer(e.target);
    try {
      await api('/entregas', {
        method: 'POST',
        body: {
          ...(esAdmin ? { conductorId: Number(d.conductorId) } : {}),
          viajeId: d.viajeId ? Number(d.viajeId) : null,
          valorEntregado: Number(d.valorEntregado),
          fechaEntrega: d.fechaEntrega,
          referencia: d.referencia,
          observaciones: d.observaciones
        }
      });
      mostrarOk(esAdmin ? 'Entrega registrada' : 'Entrega reportada; queda pendiente de verificación');
      ir('entregas');
    } catch (err) { mostrarError(err.message); }
  });
}

/* ---------- Vistas del conductor ---------- */
async function vistaGasto() {
  const [categorias, vehiculos, viajes] = await Promise.all([api('/categorias'), api('/vehiculos'), api('/viajes')]);
  const viajesActivos = viajes.filter((v) => v.estado !== 'CANCELADO');
  const vehiculosAutorizados = vehiculos.filter((v) => v.estado !== 'INACTIVO'
    && viajes.some((viaje) => viaje.vehiculoId === v.id));

  pintar(`
    <section class="card narrow">
      <h2>Registrar gasto</h2>
      <p class="nota">Su gasto quedará <strong>pendiente</strong> hasta que el administrador lo revise.</p>
      <form id="form-gasto" class="form">
        <label>Categoría
          <select name="categoriaId" required>
            ${categorias.map((c) => `<option value="${c.id}">${esc(c.nombre)}</option>`).join('')}
          </select>
        </label>
        <label>Vehículo
          <select name="vehiculoId" required>
            ${vehiculosAutorizados
              .map((v) => `<option value="${v.id}">${esc(v.placa)} · ${esc(v.marca)} ${esc(v.modelo)}</option>`).join('')}
          </select>
        </label>
        <label>Viaje (opcional)
          <select name="viajeId">
            <option value="">Sin viaje asociado</option>
            ${viajesActivos.map((v) => `<option value="${v.id}">${esc(v.codigo)} · ${esc(v.origen)} → ${esc(v.destino)}</option>`).join('')}
          </select>
        </label>
        <label>Valor (COP) <input type="number" name="valor" min="1" step="100" required></label>
        <label>Descripción <input name="descripcion" required maxlength="200" placeholder="Ej.: Tanqueada en estación"></label>
        <label>Fecha y hora <input type="datetime-local" name="fechaMovimiento" value="${ahoraLocal()}"></label>
        <label>Comprobante (PDF, JPG o PNG, máx. 5 MB)
          <input type="file" name="comprobante" accept=".pdf,.jpg,.jpeg,.png">
        </label>
        <label class="check">
          <input type="checkbox" name="pagadoConRecaudo" checked>
          <span>Pagado con dinero recaudado en el viaje <small>(se descuenta de la entrega)</small></span>
        </label>
        <button class="btn primario">Registrar gasto</button>
      </form>
    </section>`);

  $('#form-gasto').addEventListener('submit', async (e) => {
    e.preventDefault();
    const d = leer(e.target);
    const archivo = e.target.comprobante.files[0];
    try {
      const gasto = await api('/movimientos', {
        method: 'POST',
        body: {
          categoriaId: Number(d.categoriaId),
          vehiculoId: Number(d.vehiculoId),
          viajeId: d.viajeId ? Number(d.viajeId) : null,
          valor: Number(d.valor),
          descripcion: d.descripcion,
          fechaMovimiento: d.fechaMovimiento || null,
          pagadoConRecaudo: e.target.pagadoConRecaudo.checked
        }
      });
      if (archivo) {
        const form = new FormData();
        form.append('archivo', archivo);
        await api(`/movimientos/${gasto.id}/comprobantes`, { method: 'POST', form });
      }
      mostrarOk('Gasto registrado y pendiente de revisión');
      ir('movimientos');
    } catch (err) { mostrarError(err.message); }
  });
}

function etiquetaRecaudo(m) {
  return m.pagadoConRecaudo === false
    ? '<br><span class="etiqueta">Pagado por la empresa</span>'
    : '';
}

function filasMovimientos(lista) {
  return lista.map((m) => `
    <tr>
      <td>${fecha(m.fecha)}</td>
      <td><strong>${esc(m.categoria)}</strong><br><small>${esc(m.descripcion)}</small>${etiquetaRecaudo(m)}</td>
      <td>${esc(m.placa)}<br><small>${esc(m.codigoViaje || 'Sin viaje')}</small></td>
      <td>${money(m.valor)}</td>
      <td>${badge(m.estado)}</td>
      <td>${esc(m.observacionRevision || '—')}</td>
      <td>${botonSoporte(m)}</td>
    </tr>`).join('');
}

function filasEntregas(entregas, esAdmin) {
  return entregas.map((x) => `
    <tr><td>${fecha(x.fecha)}</td><td>${esc(x.conductor)}</td>
    <td>${esc(x.codigoViaje || 'General')}</td><td>${money(x.valorEntregado)}</td>
    <td>${esc(x.referencia || '—')}</td><td>${badge(x.estado)}</td>
    <td>${esAdmin && x.estado === 'REGISTRADA'
      ? `<button class="btn mini aprobar" data-accion="verificar" data-id="${x.id}">Verificar</button>`
      : esc(x.recibidoPor || 'Pendiente')}</td></tr>`).join('');
}

async function vistaMovimientos() {
  const esAdmin = sesion.rol === 'ADMIN';
  const [lista, categorias, vehiculos, viajes, conductores = []] = await Promise.all([
    api('/movimientos'), api('/categorias'), api('/vehiculos'), api('/viajes'),
    ...(esAdmin ? [api('/conductores')] : [])
  ]);
  pintar(`
    <section class="card">
      <h2>${esAdmin ? 'Movimientos financieros' : 'Mis gastos'}</h2>
      <form id="filtro-movimientos" class="form filtro-lista">
        <label>Desde <input type="date" name="desde"></label>
        <label>Hasta <input type="date" name="hasta"></label>
        <label>Estado <select name="estado"><option value="">Todos</option>${opciones(['PENDIENTE', 'APROBADO', 'RECHAZADO'])}</select></label>
        <label>Vehículo <select name="vehiculoId"><option value="">Todos</option>
          ${vehiculos.map((v) => `<option value="${v.id}">${esc(v.placa)}</option>`).join('')}
        </select></label>
        <label>Viaje <select name="viajeId"><option value="">Todos</option>
          ${viajes.map((v) => `<option value="${v.id}">${esc(v.codigo)}</option>`).join('')}
        </select></label>
        <label>Categoría <select name="categoriaId"><option value="">Todas</option>
          ${categorias.map((c) => `<option value="${c.id}">${esc(c.nombre)}</option>`).join('')}
        </select></label>
        ${esAdmin ? `<label>Conductor <select name="conductorId"><option value="">Todos</option>
          ${conductores.map((c) => `<option value="${c.id}">${esc(c.nombre)}</option>`).join('')}
        </select></label>` : ''}
        <button class="btn secundario">Filtrar gastos</button>
      </form>
      ${tabla(['Fecha', 'Gasto', 'Vehículo / viaje', 'Valor', 'Estado', 'Observación', 'Soporte'],
        filasMovimientos(lista), 'Aún no ha registrado gastos', 'tabla-movimientos')}
    </section>`);
  $('#filtro-movimientos').addEventListener('submit', async (e) => {
    e.preventDefault();
    const parametros = new URLSearchParams(Object.entries(leer(e.target)).filter(([, valor]) => valor));
    try {
      const resultados = await api(`/movimientos?${parametros}`);
      $('#tabla-movimientos tbody').innerHTML = filasMovimientos(resultados) || vacio('No hay gastos para estos filtros');
    } catch (err) { mostrarError(err.message); }
  });
}

async function vistaConciliacion() {
  const filas = await api('/conciliacion/viajes');
  const clase = (d) => (d > 0 ? 'alerta' : d < 0 ? 'revisar' : 'ok');
  const leyenda = (d) => (d > 0 ? 'Por entregar' : d < 0 ? 'Entrega superior' : 'Conciliado');
  const aviso = (f) => (f.estadoViaje === 'CANCELADO' && (f.gastosAprobados > 0 || f.gastosPagadosEmpresa > 0)
    ? '<div class="aviso">Cancelado con gastos aprobados: revisar</div>'
    : '');

  pintar(`
    <section class="card">
      <h2>Conciliación por viaje</h2>
      <p class="nota">
        Esperado = ingreso recaudado − gastos aprobados pagados con dinero del viaje.
        Diferencia = esperado − entregas verificadas. Los gastos pagados por la empresa no se descuentan (sección 12.2).
      </p>
      ${tabla(['Viaje', 'Conductor', 'Ingreso', 'Gastos a descontar', 'Pagados por la empresa', 'Esperado', 'Entregado', 'Diferencia'],
        filas.map((f) => `
        <tr>
          <td><strong>${esc(f.codigo)}</strong><br><small>${esc(etiqueta(f.estadoViaje))}</small>${aviso(f)}</td>
          <td>${esc(f.conductor)}</td>
          <td>${money(f.ingreso)}</td>
          <td>${money(f.gastosAprobados)}</td>
          <td>${money(f.gastosPagadosEmpresa)}</td>
          <td>${money(f.esperado)}</td>
          <td>${money(f.entregado)}</td>
          <td class="${clase(f.diferencia)}">${money(f.diferencia)}<br><small>${leyenda(f.diferencia)}</small></td>
        </tr>`).join(''), 'Aún no hay viajes para conciliar')}
    </section>`);
}

function filasReporte(grupos) {
    return grupos.map((g) => `
      <tr><td>${esc(g.nombre)}</td><td>${money(g.ingresos)}</td>
      <td>${money(g.gastosReportados)}</td><td>${money(g.gastosAprobados)}</td>
      <td>${money(g.entregas)}</td><td>${money(g.entregasPendientes)}</td>
      <td>${money(g.saldo)}</td></tr>`).join('');
}

function csvValor(valor) {
    if (typeof valor === 'number') return String(valor);
    const texto = String(valor ?? '');
    const seguro = /^[=+\-@]/.test(texto) ? `'${texto}` : texto;
    return `"${seguro.replaceAll('"', '""')}"`;
}

function descargarReporte() {
    if (!reporteActual) return;
    const filas = [
      ['FleetControl - Reporte financiero', `${periodoReporte.desde} a ${periodoReporte.hasta}`],
      ['Ingresos', reporteActual.ingresos],
      ['Gastos reportados', reporteActual.gastosReportados],
      ['Gastos aprobados', reporteActual.gastosAprobados],
      ['Gastos pendientes', reporteActual.gastosPendientes],
      ['Entregas verificadas', reporteActual.dineroEntregado],
      ['Entregas pendientes de verificación', reporteActual.entregasPendientes],
      ['Pendiente de conciliación', reporteActual.saldoPendiente],
      [],
      ['Resumen por vehículo'],
      ['Nombre', 'Ingresos', 'Gastos reportados', 'Gastos aprobados', 'Entregas verificadas', 'Entregas pendientes', 'Saldo'],
      ...reporteActual.porVehiculo.map((x) => [x.nombre, x.ingresos, x.gastosReportados, x.gastosAprobados, x.entregas, x.entregasPendientes, x.saldo]),
      [],
      ['Resumen por conductor'],
      ['Nombre', 'Ingresos', 'Gastos reportados', 'Gastos aprobados', 'Entregas verificadas', 'Entregas pendientes', 'Saldo'],
      ...reporteActual.porConductor.map((x) => [x.nombre, x.ingresos, x.gastosReportados, x.gastosAprobados, x.entregas, x.entregasPendientes, x.saldo]),
      [],
      ['Gastos por categoría'],
      ['Categoría', 'Ingresos', 'Gastos reportados', 'Gastos aprobados', 'Entregas verificadas', 'Entregas pendientes', 'Saldo'],
      ...reporteActual.porCategoria.map((x) => [x.nombre, x.ingresos, x.gastosReportados, x.gastosAprobados, x.entregas, x.entregasPendientes, x.saldo])
    ];
    const csv = `\uFEFF${filas.map((fila) => fila.map(csvValor).join(';')).join('\r\n')}`;
    const enlace = document.createElement('a');
    enlace.href = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
    enlace.download = `fleetcontrol-reporte-${periodoReporte.desde}-${periodoReporte.hasta}.csv`;
    enlace.click();
    setTimeout(() => URL.revokeObjectURL(enlace.href), 1000);
}

async function vistaReportes() {
    if (!periodoReporte) {
      const hoy = fechaLocal();
      periodoReporte = { desde: `${hoy.slice(0, 7)}-01`, hasta: hoy };
    }
    const query = new URLSearchParams();
    if (periodoReporte.desde) query.set('desde', periodoReporte.desde);
    if (periodoReporte.hasta) query.set('hasta', periodoReporte.hasta);
    reporteActual = await api(`/reportes?${query}`);
    const r = reporteActual;
    pintar(`
      <section class="card">
        <form id="form-periodo-reporte" class="form filtro-periodo">
          <label>Desde <input type="date" name="desde" value="${esc(periodoReporte.desde)}" required></label>
          <label>Hasta <input type="date" name="hasta" value="${esc(periodoReporte.hasta)}" required></label>
          <button class="btn primario">Aplicar periodo</button>
          <button id="exportar-reporte" type="button" class="btn secundario">Exportar CSV</button>
        </form>
      </section>
      <div class="kpis" style="margin-top:1rem">
        ${kpi('Viajes completados', r.viajesCompletados)}
        ${kpi('Ingresos recaudados', money(r.ingresos))}
        ${kpi('Gastos reportados', money(r.gastosReportados))}
        ${kpi('Gastos aprobados', money(r.gastosAprobados))}
        ${kpi('Pendiente de revisión', money(r.gastosPendientes), r.gastosPendientes > 0 ? 'alerta' : '')}
        ${kpi('Dinero entregado', money(r.dineroEntregado))}
        ${kpi('Entregas pendientes', money(r.entregasPendientes), r.entregasPendientes > 0 ? 'alerta' : '')}
        ${kpi('Diferencia de conciliación', money(r.saldoPendiente), r.saldoPendiente !== 0 ? 'alerta' : '')}
      </div>
      <p class="nota">El saldo pendiente se calcula como ingresos completados − gastos aprobados − entregas verificadas. Las entregas y gastos pendientes de revisión se muestran aparte y no reducen el saldo.</p>
      <section class="card">
        <h2>Ingresos, gastos y saldo por vehículo</h2>
        ${tabla(['Vehículo', 'Ingresos', 'Gastos reportados', 'Aprobados', 'Entregas verificadas', 'Entregas pendientes', 'Saldo'], filasReporte(r.porVehiculo), 'No hay movimientos para este periodo')}
      </section>
      <section class="card" style="margin-top:1rem">
        <h2>Resumen por conductor</h2>
        ${tabla(['Conductor', 'Ingresos', 'Gastos reportados', 'Aprobados', 'Entregas verificadas', 'Entregas pendientes', 'Saldo'], filasReporte(r.porConductor), 'No hay movimientos para este periodo')}
      </section>
      <section class="card" style="margin-top:1rem">
        <h2>Gastos por categoría</h2>
        ${tabla(['Categoría', 'Ingresos', 'Gastos reportados', 'Aprobados', 'Entregas verificadas', 'Entregas pendientes', 'Saldo'], filasReporte(r.porCategoria), 'No hay gastos para este periodo')}
      </section>`);
    $('#form-periodo-reporte').addEventListener('submit', (e) => {
      e.preventDefault();
      periodoReporte = leer(e.target);
      vistaReportes().catch((err) => mostrarError(err.message));
    });
    $('#exportar-reporte').addEventListener('click', descargarReporte);
}

/* ---------- Mapa de vistas y acciones ---------- */
const VISTAS = {
  dashboard: vistaDashboard,
  vehiculos: vistaVehiculos,
  conductores: vistaConductores,
  viajes: vistaViajes,
  revisiones: vistaRevisiones,
  entregas: vistaEntregas,
  gasto: vistaGasto,
  movimientos: vistaMovimientos,
  conciliacion: vistaConciliacion,
  reportes: vistaReportes
};

const acciones = {
  irA(id) {
    ir(id);
  },
  async soporte(id) {
    await verSoporte(id);
  },
  reintentar(_id, _valor) {
    const boton = document.querySelector('[data-accion="reintentar"]');
    ir(boton?.dataset.vista ?? 'dashboard');
  },
  async 'editar-vehiculo'(id) {
    const vehiculo = (await api('/vehiculos')).find((v) => v.id === Number(id));
    if (!vehiculo) throw new Error('No se encontró el vehículo seleccionado');
    vehiculoEditandoId = vehiculo.id;
    const form = $('#form-vehiculo');
    for (const [campo, valor] of Object.entries(vehiculo)) {
      if (form.elements[campo]) form.elements[campo].value = valor ?? '';
    }
    $('#titulo-form-vehiculo').textContent = `Editar vehículo ${vehiculo.placa}`;
    $('#guardar-vehiculo').textContent = 'Guardar cambios';
    $('#cancelar-edicion-vehiculo').classList.remove('hidden');
    form.scrollIntoView({ behavior: 'smooth', block: 'center' });
  },
  async 'historial-vehiculo'(id) {
    await ir('movimientos');
    const form = $('#filtro-movimientos');
    form.elements.vehiculoId.value = id;
    form.requestSubmit();
  },
  async 'editar-conductor'(id) {
    const conductor = (await api('/conductores')).find((c) => c.id === Number(id));
    if (!conductor) throw new Error('No se encontró el conductor seleccionado');
    conductorEditandoId = conductor.id;
    const form = $('#form-conductor');
    for (const campo of ['nombre', 'correo', 'identificacion', 'telefono']) {
      form.elements[campo].value = conductor[campo] ?? '';
    }
    form.elements.passwordTemporal.value = '';
    form.elements.passwordTemporal.required = false;
    form.elements.passwordTemporal.placeholder = 'Dejar vacío para conservarla';
    $('#titulo-form-conductor').textContent = `Editar conductor ${conductor.nombre}`;
    $('#guardar-conductor').textContent = 'Guardar cambios';
    $('#cancelar-edicion-conductor').classList.remove('hidden');
    form.scrollIntoView({ behavior: 'smooth', block: 'center' });
  },
  async aprobar(id) {
    await api(`/movimientos/${id}/revision`, { method: 'PATCH', body: { decision: 'APROBADO' } });
    mostrarOk('Gasto aprobado');
    ir('revisiones');
  },
  async rechazar(id) {
    const observacion = (prompt('Motivo del rechazo (obligatorio):') || '').trim();
    if (!observacion) throw new Error('Un rechazo requiere una observación');
    await api(`/movimientos/${id}/revision`, { method: 'PATCH', body: { decision: 'RECHAZADO', observacion } });
    mostrarOk('Gasto rechazado con observación');
    ir('revisiones');
  },
  async verificar(id) {
    await api(`/entregas/${id}/verificar`, { method: 'PATCH' });
    mostrarOk('Entrega verificada');
    ir('entregas');
  },
  async 'conductor-estado'(id, nuevo) {
    await api(`/conductores/${id}/estado`, { method: 'PATCH', body: { estado: nuevo } });
    mostrarOk('Estado del conductor actualizado');
    ir('conductores');
  },
  async 'viaje-estado'(id, nuevo) {
    await api(`/viajes/${id}/estado`, { method: 'PATCH', body: { estado: nuevo } });
    mostrarOk('Viaje actualizado');
    ir('viajes');
  }
};

/* ---------- Arranque ---------- */
document.addEventListener('DOMContentLoaded', async () => {
  $('#form-login').addEventListener('submit', async (e) => {
    e.preventDefault();
    const d = leer(e.target);
    const error = $('#login-error');
    error.classList.add('hidden');
    try {
      const datos = await api('/auth/login', { method: 'POST', body: { correo: d.correo, password: d.password } });
      sesion = { token: datos.token, nombre: datos.nombre, rol: datos.rol, correo: datos.correo };
      sessionStorage.setItem('fc_sesion', JSON.stringify(sesion));
      e.target.reset();
      mostrarApp();
    } catch (err) {
      error.textContent = err.message;
      error.classList.remove('hidden');
    }
  });

  $('#salir').addEventListener('click', salir);
  $('#nav').addEventListener('click', (e) => {
    const boton = e.target.closest('[data-nav]');
    if (boton) ir(boton.dataset.nav);
  });
  $('#vista').addEventListener('click', async (e) => {
    const boton = e.target.closest('[data-accion]');
    if (!boton) return;
    try {
      await acciones[boton.dataset.accion](boton.dataset.id, boton.dataset.valor);
    } catch (err) {
      mostrarError(err.message);
    }
  });

  if (sesion) {
    try {
      const yo = await api('/auth/me');
      sesion = { ...sesion, nombre: yo.nombre, rol: yo.rol };
      mostrarApp();
    } catch {
      salir();
    }
  } else {
    mostrarLogin();
  }
});
