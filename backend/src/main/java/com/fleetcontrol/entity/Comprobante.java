package com.fleetcontrol.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "comprobantes")
@Getter @Setter @NoArgsConstructor
public class Comprobante {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_comprobante")
    private Long idComprobante;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_movimiento")
    private MovimientoFinanciero movimiento;

    @Column(name = "nombre_archivo", nullable = false)
    private String nombreArchivo;

    @Column(name = "ruta_archivo", nullable = false)
    private String rutaArchivo;

    @Column(name = "tipo_archivo", nullable = false, length = 50)
    private String tipoArchivo;

    @Column(name = "fecha_carga", nullable = false)
    private LocalDateTime fechaCarga = LocalDateTime.now();

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_usuario_carga")
    private Usuario usuarioCarga;
}
