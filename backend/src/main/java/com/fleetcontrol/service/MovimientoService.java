package com.fleetcontrol.service;

import com.fleetcontrol.dto.Dtos.CategoriaResponse;
import com.fleetcontrol.dto.Dtos.GastoRequest;
import com.fleetcontrol.dto.Dtos.MovimientoResponse;
import com.fleetcontrol.dto.Dtos.RevisionRequest;
import com.fleetcontrol.entity.CategoriaMovimiento;
import com.fleetcontrol.entity.Comprobante;
import com.fleetcontrol.entity.MovimientoFinanciero;
import com.fleetcontrol.entity.Usuario;
import com.fleetcontrol.entity.Viaje;
import com.fleetcontrol.exception.ApiException;
import com.fleetcontrol.repository.CategoriaRepository;
import com.fleetcontrol.repository.ComprobanteRepository;
import com.fleetcontrol.repository.MovimientoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MovimientoService {

    private final MovimientoRepository movimientoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ComprobanteRepository comprobanteRepository;
    private final VehiculoService vehiculoService;
    private final ViajeService viajeService;
    private final ConductorService conductorService;
    private final AuditoriaService auditoria;

    public List<CategoriaResponse> categorias() {
        return categoriaRepository.findAll().stream()
                .filter(c -> Boolean.TRUE.equals(c.getActiva()) && "EGRESO".equals(c.getTipo()))
                .map(c -> new CategoriaResponse(c.getIdCategoria(), c.getNombre()))
                .toList();
    }

    /** Admin: todos los movimientos. Conductor: solo los que registró (RN-10). */
    public List<MovimientoResponse> listar(Usuario actual, boolean esAdmin, String estado) {
        List<MovimientoFinanciero> lista = esAdmin
                ? movimientoRepository.findAllByOrderByFechaMovimientoDesc()
                : movimientoRepository.findByUsuarioRegistraIdUsuarioOrderByFechaMovimientoDesc(actual.getIdUsuario());
        return lista.stream()
                .filter(m -> estado == null || estado.isBlank() || estado.equals(m.getEstadoRevision()))
                .map(this::aDto)
                .toList();
    }

    public List<MovimientoResponse> listar(Usuario actual, boolean esAdmin, String estado, LocalDate desde,
                                            LocalDate hasta, Long vehiculoId, Long viajeId, Long categoriaId,
                                            Long conductorId) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La fecha inicial no puede ser posterior a la fecha final");
        }
        Long usuarioConductorId = null;
        if (conductorId != null) {
            usuarioConductorId = conductorService.buscar(conductorId).getUsuario().getIdUsuario();
        }
        Long filtroUsuario = usuarioConductorId;
        List<MovimientoFinanciero> lista = esAdmin
                ? movimientoRepository.findAllByOrderByFechaMovimientoDesc()
                : movimientoRepository.findByUsuarioRegistraIdUsuarioOrderByFechaMovimientoDesc(actual.getIdUsuario());
        return lista.stream()
                .filter(m -> estado == null || estado.isBlank() || estado.equals(m.getEstadoRevision()))
                .filter(m -> desde == null || !m.getFechaMovimiento().toLocalDate().isBefore(desde))
                .filter(m -> hasta == null || !m.getFechaMovimiento().toLocalDate().isAfter(hasta))
                .filter(m -> vehiculoId == null || m.getVehiculo().getIdVehiculo().equals(vehiculoId))
                .filter(m -> viajeId == null || (m.getViaje() != null && m.getViaje().getIdViaje().equals(viajeId)))
                .filter(m -> categoriaId == null || m.getCategoria().getIdCategoria().equals(categoriaId))
                .filter(m -> filtroUsuario == null || m.getUsuarioRegistra().getIdUsuario().equals(filtroUsuario))
                .map(this::aDto)
                .toList();
    }

    /** RN-02, RN-12 y RN-10: el gasto se asocia a vehículo y, si aplica, a un viaje del conductor. */
    public MovimientoResponse registrarGasto(Usuario actual, GastoRequest req) {
        CategoriaMovimiento categoria = categoriaRepository.findById(req.categoriaId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Categoría no encontrada"));
        if (!Boolean.TRUE.equals(categoria.getActiva()) || !"EGRESO".equals(categoria.getTipo())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La categoría no está disponible para gastos");
        }
        var vehiculo = vehiculoService.buscar(req.vehiculoId());
        if ("INACTIVO".equals(vehiculo.getEstado())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No se pueden registrar gastos en un vehículo inactivo");
        }

        Viaje viaje = null;
        if (req.viajeId() != null) {
            viaje = viajeService.buscar(req.viajeId());
            if ("CANCELADO".equals(viaje.getEstado())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "No se pueden registrar gastos en un viaje cancelado");
            }
            if (!viaje.getVehiculo().getIdVehiculo().equals(vehiculo.getIdVehiculo())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "El viaje no corresponde al vehículo seleccionado");
            }
            if ("CONDUCTOR".equals(actual.getRol())) {
                Long idPropio = conductorService.delUsuario(actual).getIdConductor();
                if (!viaje.getConductor().getIdConductor().equals(idPropio)) {
                    throw new ApiException(HttpStatus.FORBIDDEN, "Solo puede registrar gastos en sus propios viajes");
                }
            }
        } else if ("CONDUCTOR".equals(actual.getRol())
                && !viajeService.tieneVehiculoAsignado(actual, vehiculo.getIdVehiculo())) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Solo puede registrar gastos en vehículos que tenga asignados a sus viajes");
        }

        MovimientoFinanciero m = new MovimientoFinanciero();
        m.setCategoria(categoria);
        m.setVehiculo(vehiculo);
        m.setViaje(viaje);
        m.setUsuarioRegistra(actual);
        m.setTipo("EGRESO");
        m.setFechaMovimiento(req.fechaMovimiento() != null ? req.fechaMovimiento() : LocalDateTime.now());
        m.setValor(req.valor());
        m.setDescripcion(req.descripcion());
        m.setPagadoConRecaudo(req.pagadoConRecaudo() == null || req.pagadoConRecaudo());
        m.setEstadoRevision("PENDIENTE");          // nace pendiente (RN-08)
        m = movimientoRepository.save(m);

        auditoria.registrar(actual, "movimientos_financieros", m.getIdMovimiento(), "CREACION",
                "Gasto " + categoria.getNombre() + " por " + req.valor());
        return aDto(m);
    }

    /**
     * Aprobar o rechazar (RF-09, RN-04, RN-08).
     * Un gasto se revisa una sola vez; el revisor no puede ser quien lo registró.
     */
    public MovimientoResponse revisar(Usuario admin, Long id, RevisionRequest req) {
        MovimientoFinanciero m = movimientoRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Movimiento no encontrado"));
        if (!"PENDIENTE".equals(m.getEstadoRevision())) {
            throw new ApiException(HttpStatus.CONFLICT, "Este gasto ya fue " + m.getEstadoRevision().toLowerCase());
        }
        if (m.getUsuarioRegistra().getIdUsuario().equals(admin.getIdUsuario())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No puede aprobar ni rechazar sus propios gastos");
        }
        String decision = req.decision();
        if (!decision.equals("APROBADO") && !decision.equals("RECHAZADO")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La decisión debe ser APROBADO o RECHAZADO");
        }
        if (decision.equals("RECHAZADO") && (req.observacion() == null || req.observacion().isBlank())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Un rechazo requiere una observación");
        }

        m.setEstadoRevision(decision);
        m.setUsuarioRevisa(admin);
        m.setFechaRevision(LocalDateTime.now());
        m.setObservacionRevision(req.observacion());
        m = movimientoRepository.save(m);

        auditoria.registrar(admin, "movimientos_financieros", id, decision, req.observacion());
        return aDto(m);
    }

    MovimientoResponse aDto(MovimientoFinanciero m) {
        List<Long> comprobantes = comprobanteRepository.findByMovimientoIdMovimiento(m.getIdMovimiento())
                .stream().map(Comprobante::getIdComprobante).toList();
        return new MovimientoResponse(
                m.getIdMovimiento(),
                m.getCategoria().getNombre(),
                m.getVehiculo().getIdVehiculo(),
                m.getVehiculo().getPlaca(),
                m.getViaje() != null ? m.getViaje().getIdViaje() : null,
                m.getViaje() != null ? m.getViaje().getCodigo() : null,
                m.getUsuarioRegistra().getNombre(),
                m.getValor(),
                m.getDescripcion(),
                m.getFechaMovimiento(),
                m.getEstadoRevision(),
                m.getObservacionRevision(),
                comprobantes,
                m.descontableDeEntrega());
    }
}
