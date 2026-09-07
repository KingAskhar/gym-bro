package com.gymbro.backend.model.entity;

import com.gymbro.backend.model.base.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Plan comercial que el gimnasio ofrece: mensual, trimestral, anual.
 *
 * Es un catalogo. Los planes se definen una vez y muchos socios se
 * suscriben a ellos.
 */
@Entity
@Table(
    name = "membresia",
    uniqueConstraints = @UniqueConstraint(name = "uq_membresia_nombre", columnNames = "nombre")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(exclude = "suscripciones")
public class Membresia extends BaseEntity {

    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Column(name = "precio", nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(name = "duracion_dias", nullable = false)
    private Integer duracionDias;

    /** Si el plan da acceso a las clases grupales o solo a la sala de maquinas. */
    @Column(name = "incluye_clases_grupales", nullable = false)
    @Builder.Default
    private Boolean incluyeClasesGrupales = false;

    @Column(name = "incluye_entrenador", nullable = false)
    @Builder.Default
    private Boolean incluyeEntrenador = false;

    // ---------------------------------------------------------------
    //  RELACIONES
    // ---------------------------------------------------------------

    /** UNO A MUCHOS: un plan es contratado por muchos socios. */
    @OneToMany(mappedBy = "membresia", cascade = CascadeType.PERSIST,
               fetch = FetchType.LAZY)
    @Builder.Default
    private List<Suscripcion> suscripciones = new ArrayList<>();
}
