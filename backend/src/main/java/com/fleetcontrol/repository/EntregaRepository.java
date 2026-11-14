package com.fleetcontrol.repository;

import com.fleetcontrol.entity.EntregaDinero;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EntregaRepository extends JpaRepository<EntregaDinero, Long> {
    List<EntregaDinero> findAllByOrderByFechaEntregaDesc();
    List<EntregaDinero> findByConductorIdConductorOrderByFechaEntregaDesc(Long idConductor);
    List<EntregaDinero> findByViajeIdViaje(Long idViaje);
}
