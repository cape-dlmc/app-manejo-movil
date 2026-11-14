package com.fleetcontrol.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "conductores")
@Getter @Setter @NoArgsConstructor
public class Conductor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_conductor")
    private Long idConductor;

    @OneToOne(optional = false)
    @JoinColumn(name = "id_usuario", unique = true)
    private Usuario usuario;

    private String telefono;

    @Column(nullable = false, unique = true, length = 30)
    private String identificacion;

    @Column(nullable = false, length = 20)
    private String estado = "ACTIVO";
}
