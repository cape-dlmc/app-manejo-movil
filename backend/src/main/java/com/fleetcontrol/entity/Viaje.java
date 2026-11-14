package com.fleetcontrol.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "viajes")
@Getter @Setter @NoArgsConstructor
public class Viaje {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_viaje")
    private Long idViaje;

    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_vehiculo")
    private Vehiculo vehiculo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_conductor")
    private Conductor conductor;

    @Column(name = "tipo_servicio", nullable = false, length = 30)
    private String tipoServicio;

    @Column(nullable = false, length = 150)
    private String origen;

    @Column(nullable = false, length = 150)
    private String destino;

    @Column(name = "fecha_salida", nullable = false)
    private LocalDateTime fechaSalida;

    @Column(name = "fecha_finalizacion")
    private LocalDateTime fechaFinalizacion;

    @Column(name = "valor_cobrado", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorCobrado;

    @Column(nullable = false, length = 25)
    private String estado = "PROGRAMADO";

    private String observaciones;
}
