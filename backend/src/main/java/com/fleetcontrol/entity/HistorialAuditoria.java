package com.fleetcontrol.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "historial_auditoria")
@Getter @Setter @NoArgsConstructor
public class HistorialAuditoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Long idAuditoria;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(nullable = false, length = 60)
    private String entidad;

    @Column(name = "id_registro", nullable = false)
    private Long idRegistro;

    @Column(nullable = false, length = 30)
    private String accion;

    @Column(name = "fecha_accion", nullable = false)
    private LocalDateTime fechaAccion;

    private String detalle;
}
