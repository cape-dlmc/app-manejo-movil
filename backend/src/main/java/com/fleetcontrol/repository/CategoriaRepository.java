package com.fleetcontrol.repository;

import com.fleetcontrol.entity.CategoriaMovimiento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<CategoriaMovimiento, Long> {
}
