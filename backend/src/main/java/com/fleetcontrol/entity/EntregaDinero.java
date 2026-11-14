package com.fleetcontrol.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "entregas_dinero")
@Getter @Setter @NoArgsConstructor
public class EntregaDinero {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_entrega")
    private Long idEntrega;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_conductor")
    private Conductor conductor;

    @ManyToOne
    @JoinColumn(name = "id_viaje")
    private Viaje viaje;

    @Column(name = "fecha_entrega", nullable = false)
    private LocalDateTime fechaEntrega;

    @Column(name = "valor_entregado", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorEntregado;

    @ManyToOne
    @JoinColumn(name = "id_usuario_recibe")
    private Usuario usuarioRecibe;

    private String referencia;
    private String observaciones;

    @Column(nullable = false, length = 20)
    private String estado = "REGISTRADA";
}
