package com.fleetcontrol.service;

import com.fleetcontrol.dto.Dtos.ViajeRequest;
import com.fleetcontrol.dto.Dtos.ViajeResponse;
import com.fleetcontrol.entity.Conductor;
import com.fleetcontrol.entity.Usuario;
import com.fleetcontrol.entity.Viaje;
import com.fleetcontrol.entity.Vehiculo;
import com.fleetcontrol.exception.ApiException;
import com.fleetcontrol.repository.ViajeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ViajeService {

    static final Set<String> TIPOS = Set.of("INTERMUNICIPAL", "PUERTA_A_PUERTA");
    static final Set<String> ESTADOS = Set.of("PROGRAMADO", "EN_CURSO", "COMPLETADO", "CANCELADO");

    private final ViajeRepository viajeRepository;
    private final VehiculoService vehiculoService;
    private final ConductorService conductorService;
    private final AuditoriaService auditoria;

    /** El administrador ve todos los viajes; el conductor solo los suyos (RN-10). */
    public List<ViajeResponse> listar(Usuario actual, boolean esAdmin) {
        List<Viaje> viajes = esAdmin
                ? viajeRepository.findAllByOrderByFechaSalidaDesc()
                : viajeRepository.findByConductorIdConductorOrderByFechaSalidaDesc(
                        conductorService.delUsuario(actual).getIdConductor());
        return viajes.stream().map(this::aDto).toList();
    }

    public List<ViajeResponse> listar(Usuario actual, boolean esAdmin, LocalDate desde, LocalDate hasta,
                                      Long vehiculoId, Long conductorId, String estado, String tipoServicio) {
        validarPeriodo(desde, hasta);
        List<Viaje> viajes = esAdmin
                ? viajeRepository.findAllByOrderByFechaSalidaDesc()
                : viajeRepository.findByConductorIdConductorOrderByFechaSalidaDesc(
                        conductorService.delUsuario(actual).getIdConductor());
        return viajes.stream()
                .filter(v -> desde == null || !v.getFechaSalida().toLocalDate().isBefore(desde))
                .filter(v -> hasta == null || !v.getFechaSalida().toLocalDate().isAfter(hasta))
                .filter(v -> vehiculoId == null || v.getVehiculo().getIdVehiculo().equals(vehiculoId))
                .filter(v -> conductorId == null || v.getConductor().getIdConductor().equals(conductorId))
                .filter(v -> estado == null || estado.isBlank() || estado.equals(v.getEstado()))
                .filter(v -> tipoServicio == null || tipoServicio.isBlank() || tipoServicio.equals(v.getTipoServicio()))
                .map(this::aDto)
                .toList();
    }

    public Viaje buscar(Long id) {
        return viajeRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Viaje no encontrado"));
    }

    public boolean tieneVehiculoAsignado(Usuario usuario, Long idVehiculo) {
        Long idConductor = conductorService.delUsuario(usuario).getIdConductor();
        return viajeRepository.findByConductorIdConductorOrderByFechaSalidaDesc(idConductor).stream()
                .anyMatch(v -> v.getVehiculo().getIdVehiculo().equals(idVehiculo));
    }

    public ViajeResponse crear(Usuario actor, ViajeRequest req) {
        if (!TIPOS.contains(req.tipoServicio())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tipo de servicio no válido");
        }
        if (viajeRepository.existsByCodigo(req.codigo())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un viaje con el código " + req.codigo());
        }
        Vehiculo vehiculo = vehiculoService.buscar(req.vehiculoId());
        if ("INACTIVO".equals(vehiculo.getEstado())) {
            // RN-12: un vehículo inactivo no puede asignarse a nuevos viajes
            throw new ApiException(HttpStatus.BAD_REQUEST, "El vehículo " + vehiculo.getPlaca() + " está inactivo");
        }
        Conductor conductor = conductorService.buscar(req.conductorId());
        if (!"ACTIVO".equals(conductor.getEstado())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El conductor está inactivo");
        }

        Viaje v = new Viaje();
        v.setCodigo(req.codigo());
        v.setVehiculo(vehiculo);
        v.setConductor(conductor);
        v.setTipoServicio(req.tipoServicio());
        v.setOrigen(req.origen());
        v.setDestino(req.destino());
        v.setFechaSalida(req.fechaSalida());
        v.setValorCobrado(req.valorCobrado());
        v.setEstado("PROGRAMADO");
        v.setObservaciones(req.observaciones());
        v = viajeRepository.save(v);

        auditoria.registrar(actor, "viajes", v.getIdViaje(), "CREACION", "Viaje " + v.getCodigo());
        return aDto(v);
    }

    public ViajeResponse cambiarEstado(Usuario actor, Long id, String estado) {
        if (!ESTADOS.contains(estado)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Estado de viaje no válido: " + estado);
        }
        Viaje v = buscar(id);
        v.setEstado(estado);
        if ("COMPLETADO".equals(estado)) {
            LocalDateTime ahora = LocalDateTime.now();
            if (v.getFechaSalida() != null && ahora.isBefore(v.getFechaSalida())) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "No se puede completar el viaje antes de su fecha de salida (" + v.getFechaSalida() + ")");
            }
            v.setFechaFinalizacion(ahora);
        }
        v = viajeRepository.save(v);
        auditoria.registrar(actor, "viajes", id, "ESTADO", "Nuevo estado: " + estado);
        return aDto(v);
    }

    ViajeResponse aDto(Viaje v) {
        Conductor c = v.getConductor();
        return new ViajeResponse(v.getIdViaje(), v.getCodigo(), v.getTipoServicio(), v.getOrigen(),
                v.getDestino(), v.getFechaSalida(), v.getValorCobrado(), v.getEstado(),
                v.getVehiculo().getIdVehiculo(), v.getVehiculo().getPlaca(),
                c.getIdConductor(), c.getUsuario().getNombre());
    }

    private static void validarPeriodo(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La fecha inicial no puede ser posterior a la fecha final");
        }
    }
}
