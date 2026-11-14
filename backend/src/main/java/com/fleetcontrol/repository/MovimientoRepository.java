package com.fleetcontrol.repository;

import com.fleetcontrol.entity.MovimientoFinanciero;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimientoRepository extends JpaRepository<MovimientoFinanciero, Long> {
    List<MovimientoFinanciero> findAllByOrderByFechaMovimientoDesc();
    List<MovimientoFinanciero> findByUsuarioRegistraIdUsuarioOrderByFechaMovimientoDesc(Long idUsuario);
    List<MovimientoFinanciero> findByViajeIdViaje(Long idViaje);
}
