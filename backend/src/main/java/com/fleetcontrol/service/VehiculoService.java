package com.fleetcontrol.service;

import com.fleetcontrol.dto.Dtos.VehiculoRequest;
import com.fleetcontrol.dto.Dtos.VehiculoResponse;
import com.fleetcontrol.entity.Usuario;
import com.fleetcontrol.entity.Vehiculo;
import com.fleetcontrol.exception.ApiException;
import com.fleetcontrol.repository.VehiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class VehiculoService {

    static final Set<String> ESTADOS = Set.of("DISPONIBLE", "EN_SERVICIO", "MANTENIMIENTO", "INACTIVO");

    private final VehiculoRepository repository;
    private final AuditoriaService auditoria;

    public List<VehiculoResponse> listar() {
        return repository.findAll().stream().map(this::aDto).toList();
    }

    public Vehiculo buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Vehículo no encontrado"));
    }

    public VehiculoResponse crear(Usuario actor, VehiculoRequest req) {
        validarEstado(req.estado());
        String placa = normalizarPlaca(req.placa());
        if (repository.existsByPlaca(placa)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un vehículo con esa placa");
        }
        Vehiculo v = new Vehiculo();
        aplicar(v, req);
        v = repository.save(v);
        auditoria.registrar(actor, "vehiculos", v.getIdVehiculo(), "CREACION", "Vehículo " + v.getPlaca());
        return aDto(v);
    }

    public VehiculoResponse actualizar(Usuario actor, Long id, VehiculoRequest req) {
        validarEstado(req.estado());
        Vehiculo v = buscar(id);
        String placa = normalizarPlaca(req.placa());
        if (!placa.equals(v.getPlaca()) && repository.existsByPlaca(placa)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un vehículo con esa placa");
        }
        aplicar(v, req);
        v = repository.save(v);
        auditoria.registrar(actor, "vehiculos", id, "ACTUALIZACION", "Estado: " + v.getEstado());
        return aDto(v);
    }

    private void aplicar(Vehiculo v, VehiculoRequest req) {
        v.setPlaca(normalizarPlaca(req.placa()));
        v.setMarca(req.marca());
        v.setModelo(req.modelo());
        v.setAnio(req.anio());
        v.setEstado(req.estado());
        v.setObservaciones(req.observaciones());
    }

    private void validarEstado(String estado) {
        if (!ESTADOS.contains(estado)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Estado de vehículo no válido: " + estado);
        }
    }

    private static String normalizarPlaca(String placa) {
        return placa.trim().toUpperCase(Locale.ROOT);
    }

    VehiculoResponse aDto(Vehiculo v) {
        return new VehiculoResponse(v.getIdVehiculo(), v.getPlaca(), v.getMarca(), v.getModelo(),
                v.getAnio(), v.getEstado(), v.getObservaciones());
    }
}
