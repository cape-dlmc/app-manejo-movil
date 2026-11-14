package com.fleetcontrol.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "categorias_movimiento")
@Getter @Setter @NoArgsConstructor
public class CategoriaMovimiento {
    @Id
    @Column(name = "id_categoria")
    private Long idCategoria;

    @Column(nullable = false, unique = true, length = 60)
    private String nombre;

    @Column(nullable = false, length = 15)
    private String tipo;

    private String descripcion;

    @Column(nullable = false)
    private Boolean activa = true;
}
