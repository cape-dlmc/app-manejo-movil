package com.fleetcontrol.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimientos_financieros")
@Getter @Setter @NoArgsConstructor
public class MovimientoFinanciero {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimiento")
    private Long idMovimiento;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_categoria")
    private CategoriaMovimiento categoria;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_vehiculo")
    private Vehiculo vehiculo;

    @ManyToOne
    @JoinColumn(name = "id_viaje")
    private Viaje viaje;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_usuario_registra")
    private Usuario usuarioRegistra;

    @Column(nullable = false, length = 15)
    private String tipo = "EGRESO";

    @Column(name = "fecha_movimiento", nullable = false)
    private LocalDateTime fechaMovimiento;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private String descripcion;

    @Column(name = "pagado_con_recaudo", nullable = false)
    private Boolean pagadoConRecaudo = true;

    @Column(name = "estado_revision", nullable = false, length = 20)
    private String estadoRevision = "PENDIENTE";

    @ManyToOne
    @JoinColumn(name = "id_usuario_revisa")
    private Usuario usuarioRevisa;

    @Column(name = "fecha_revision")
    private LocalDateTime fechaRevision;

    @Column(name = "observacion_revision")
    private String observacionRevision;

    /** Solo los gastos pagados con dinero del viaje se descuentan de la entrega (sección 12.2). */
    public boolean descontableDeEntrega() {
        return !Boolean.FALSE.equals(pagadoConRecaudo);
    }
}
