package com.fleetcontrol.repository;

import com.fleetcontrol.entity.Conductor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConductorRepository extends JpaRepository<Conductor, Long> {
    Optional<Conductor> findByUsuarioCorreo(String correo);
    Optional<Conductor> findByIdentificacion(String identificacion);
    boolean existsByIdentificacion(String identificacion);
}
