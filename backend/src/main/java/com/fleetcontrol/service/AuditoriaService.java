package com.fleetcontrol.service;

import com.fleetcontrol.entity.HistorialAuditoria;
import com.fleetcontrol.entity.Usuario;
import com.fleetcontrol.repository.HistorialAuditoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/** Registra quién hizo qué y cuándo (RNF-07, RN-11, RF-14). */
@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final HistorialAuditoriaRepository repository;

    public void registrar(Usuario usuario, String entidad, Long idRegistro, String accion, String detalle) {
        HistorialAuditoria h = new HistorialAuditoria();
        h.setUsuario(usuario);
        h.setEntidad(entidad);
        h.setIdRegistro(idRegistro);
        h.setAccion(accion);
        h.setFechaAccion(LocalDateTime.now());
        h.setDetalle(detalle);
        repository.save(h);
    }
}
