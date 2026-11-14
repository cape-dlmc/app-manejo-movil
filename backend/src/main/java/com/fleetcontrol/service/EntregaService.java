package com.fleetcontrol.service;

import com.fleetcontrol.dto.Dtos.EntregaRequest;
import com.fleetcontrol.dto.Dtos.EntregaResponse;
import com.fleetcontrol.entity.Conductor;
import com.fleetcontrol.entity.EntregaDinero;
import com.fleetcontrol.entity.Usuario;
import com.fleetcontrol.entity.Viaje;
import com.fleetcontrol.exception.ApiException;
import com.fleetcontrol.repository.EntregaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EntregaService {

    private final EntregaRepository entregaRepository;
    private final ConductorService conductorService;
    private final ViajeService viajeService;
    private final AuditoriaService auditoria;

    /** Admin: todas las entregas. Conductor: solo las propias (RN-10). */
    public List<EntregaResponse> listar(Usuario actual, boolean esAdmin) {
        List<EntregaDinero> lista = esAdmin
                ? entregaRepository.findAllByOrderByFechaEntregaDesc()
                : entregaRepository.findByConductorIdConductorOrderByFechaEntregaDesc(
                        conductorService.delUsuario(actual).getIdConductor());
        return lista.stream().map(this::aDto).toList();
    }

    public List<EntregaResponse> listar(Usuario actual, boolean esAdmin, LocalDate desde, LocalDate hasta,
                                        Long conductorId, Long viajeId, String estado) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La fecha inicial no puede ser posterior a la fecha final");
        }
        List<EntregaDinero> lista = esAdmin
                ? entregaRepository.findAllByOrderByFechaEntregaDesc()
                : entregaRepository.findByConductorIdConductorOrderByFechaEntregaDesc(
                        conductorService.delUsuario(actual).getIdConductor());
        return lista.stream()
                .filter(e -> desde == null || !e.getFechaEntrega().toLocalDate().isBefore(desde))
                .filter(e -> hasta == null || !e.getFechaEntrega().toLocalDate().isAfter(hasta))
                .filter(e -> conductorId == null || e.getConductor().getIdConductor().equals(conductorId))
                .filter(e -> viajeId == null || (e.getViaje() != null && e.getViaje().getIdViaje().equals(viajeId)))
                .filter(e -> estado == null || estado.isBlank() || estado.equals(e.getEstado()))
                .map(this::aDto)
                .toList();
    }

    /**
     * Registra el dinero recibido por la empresa (RF-10, RN-06).
     * El usuario que recibe es el administrador que registra la operación.
     */
    public EntregaResponse registrar(Usuario actual, boolean esAdmin, EntregaRequest req) {
        Conductor conductor;
        if (esAdmin) {
            if (req.conductorId() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Seleccione el conductor que realiza la entrega");
            }
            conductor = conductorService.buscar(req.conductorId());
        } else {
            conductor = conductorService.delUsuario(actual);
            if (req.conductorId() != null && !req.conductorId().equals(conductor.getIdConductor())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Solo puede reportar sus propias entregas");
            }
        }
        EntregaDinero e = new EntregaDinero();
        e.setConductor(conductor);
        e.setUsuarioRecibe(esAdmin ? actual : null);
        e.setFechaEntrega(req.fechaEntrega() != null ? req.fechaEntrega() : LocalDateTime.now());
        e.setValorEntregado(req.valorEntregado());
        e.setReferencia(req.referencia());
        e.setObservaciones(req.observaciones());
        e.setEstado("REGISTRADA");

        if (req.viajeId() != null) {
            Viaje viaje = viajeService.buscar(req.viajeId());
            if (!viaje.getConductor().getIdConductor().equals(conductor.getIdConductor())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "El viaje no pertenece a ese conductor");
            }
            e.setViaje(viaje);
        }
        e = entregaRepository.save(e);
        auditoria.registrar(actual, "entregas_dinero", e.getIdEntrega(), "CREACION",
                "Entrega de " + req.valorEntregado() + " de " + conductor.getUsuario().getNombre());
        return aDto(e);
    }

    public EntregaResponse verificar(Usuario admin, Long id) {
        EntregaDinero e = entregaRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Entrega no encontrada"));
        if (!"REGISTRADA".equals(e.getEstado())) {
            throw new ApiException(HttpStatus.CONFLICT, "La entrega ya fue verificada");
        }
        e.setEstado("VERIFICADA");
        e.setUsuarioRecibe(admin);
        e = entregaRepository.save(e);
        auditoria.registrar(admin, "entregas_dinero", id, "VERIFICACION", "Entrega verificada");
        return aDto(e);
    }

    EntregaResponse aDto(EntregaDinero e) {
        return new EntregaResponse(
                e.getIdEntrega(),
                e.getConductor().getIdConductor(),
                e.getConductor().getUsuario().getNombre(),
                e.getViaje() != null ? e.getViaje().getIdViaje() : null,
                e.getViaje() != null ? e.getViaje().getCodigo() : null,
                e.getFechaEntrega(),
                e.getValorEntregado(),
                e.getReferencia(),
                e.getObservaciones(),
                e.getEstado(),
                e.getUsuarioRecibe() != null ? e.getUsuarioRecibe().getNombre() : null);
    }
}
