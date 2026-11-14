package com.fleetcontrol.repository;

import com.fleetcontrol.entity.Comprobante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComprobanteRepository extends JpaRepository<Comprobante, Long> {
    List<Comprobante> findByMovimientoIdMovimiento(Long idMovimiento);
}
