package com.fleetcontrol.service;

import com.fleetcontrol.dto.Dtos.ConductorRequest;
import com.fleetcontrol.dto.Dtos.ConductorResponse;
import com.fleetcontrol.dto.Dtos.ConductorUpdateRequest;
import com.fleetcontrol.entity.Conductor;
import com.fleetcontrol.entity.Usuario;
import com.fleetcontrol.exception.ApiException;
import com.fleetcontrol.repository.ConductorRepository;
import com.fleetcontrol.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConductorService {

    private final ConductorRepository conductorRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoria;

    public List<ConductorResponse> listar() {
        return conductorRepository.findAll().stream().map(this::aDto).toList();
    }

    /** Conductor asociado a una cuenta de usuario (un usuario = un conductor, RN-10). */
    public Conductor delUsuario(Usuario usuario) {
        return conductorRepository.findByUsuarioCorreo(usuario.getCorreo())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "La cuenta no tiene perfil de conductor"));
    }

    public Conductor buscar(Long id) {
        return conductorRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Conductor no encontrado"));
    }

    public ConductorResponse crear(Usuario actor, ConductorRequest req) {
        if (usuarioRepository.existsByCorreo(req.correo())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe una cuenta con ese correo");
        }
        if (conductorRepository.existsByIdentificacion(req.identificacion())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un conductor con esa identificación");
        }
        Usuario cuenta = new Usuario();
        cuenta.setNombre(req.nombre());
        cuenta.setCorreo(req.correo());
        cuenta.setPasswordHash(passwordEncoder.encode(req.passwordTemporal()));
        cuenta.setRol("CONDUCTOR");
        cuenta.setEstado(true);
        cuenta = usuarioRepository.save(cuenta);

        Conductor c = new Conductor();
        c.setUsuario(cuenta);
        c.setIdentificacion(req.identificacion());
        c.setTelefono(req.telefono());
        c.setEstado("ACTIVO");
        c = conductorRepository.save(c);

        auditoria.registrar(actor, "conductores", c.getIdConductor(), "CREACION", "Conductor " + req.nombre());
        return aDto(c);
    }

    public ConductorResponse cambiarEstado(Usuario actor, Long id, String estado) {
        if (!estado.equals("ACTIVO") && !estado.equals("INACTIVO")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Estado no válido: " + estado);
        }
        Conductor c = buscar(id);
        c.setEstado(estado);
        c.getUsuario().setEstado(estado.equals("ACTIVO"));
        usuarioRepository.save(c.getUsuario());
        c = conductorRepository.save(c);
        auditoria.registrar(actor, "conductores", id, "ACTUALIZACION", "Estado: " + estado);
        return aDto(c);
    }

    public ConductorResponse actualizar(Usuario actor, Long id, ConductorUpdateRequest req) {
        Conductor c = buscar(id);
        Usuario cuenta = c.getUsuario();
        Long idConductor = c.getIdConductor();
        usuarioRepository.findByCorreo(req.correo()).ifPresent(otra -> {
            if (!otra.getIdUsuario().equals(cuenta.getIdUsuario())) {
                throw new ApiException(HttpStatus.CONFLICT, "Ya existe una cuenta con ese correo");
            }
        });
        conductorRepository.findByIdentificacion(req.identificacion()).ifPresent(otro -> {
            if (!otro.getIdConductor().equals(idConductor)) {
                throw new ApiException(HttpStatus.CONFLICT, "Ya existe un conductor con esa identificación");
            }
        });

        cuenta.setNombre(req.nombre());
        cuenta.setCorreo(req.correo());
        if (req.passwordTemporal() != null && !req.passwordTemporal().isBlank()) {
            cuenta.setPasswordHash(passwordEncoder.encode(req.passwordTemporal()));
        }
        c.setIdentificacion(req.identificacion());
        c.setTelefono(req.telefono());
        usuarioRepository.save(cuenta);
        c = conductorRepository.save(c);
        auditoria.registrar(actor, "conductores", id, "ACTUALIZACION", "Datos del conductor actualizados");
        return aDto(c);
    }

    ConductorResponse aDto(Conductor c) {
        Usuario u = c.getUsuario();
        return new ConductorResponse(c.getIdConductor(), u.getIdUsuario(), u.getNombre(), u.getCorreo(),
                c.getIdentificacion(), c.getTelefono(), c.getEstado());
    }
}
