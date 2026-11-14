package com.fleetcontrol.repository;

import com.fleetcontrol.entity.Viaje;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ViajeRepository extends JpaRepository<Viaje, Long> {
    List<Viaje> findAllByOrderByFechaSalidaDesc();
    List<Viaje> findByConductorIdConductorOrderByFechaSalidaDesc(Long idConductor);
    boolean existsByCodigo(String codigo);
}
