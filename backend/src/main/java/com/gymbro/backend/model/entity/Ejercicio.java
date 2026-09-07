package com.gymbro.backend.model.entity;

import com.gymbro.backend.model.base.BaseEntity;
import com.gymbro.backend.model.enums.GrupoMuscular;
import com.gymbro.backend.model.enums.NivelDificultad;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;

/**
 * Movimiento o actividad fisica del catalogo del gimnasio.
 *
 * Se define una sola vez y se reutiliza en todas las rutinas que lo
 * necesiten.
 */
@Entity
@Table(
    name = "ejercicio",
    uniqueConstraints = @UniqueConstraint(name = "uq_ejercicio_nombre", columnNames = "nombre")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(exclude = "rutinas")
public class Ejercicio extends BaseEntity {

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    /**
     * columnDefinition = "TEXT" es necesario en PostgreSQL para textos
     * largos. Con @Lob, Hibernate mapearia a un tipo "oid" que guarda
     * una referencia externa y complica las consultas.
     */
    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "grupo_muscular", nullable = false, length = 30)
    private GrupoMuscular grupoMuscular;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel", nullable = false, length = 20)
    @Builder.Default
    private NivelDificultad nivel = NivelDificultad.PRINCIPIANTE;

    @Column(name = "series_sugeridas")
    private Integer seriesSugeridas;

    @Column(name = "repeticiones_sugeridas")
    private Integer repeticionesSugeridas;

    @Column(name = "requiere_maquina", nullable = false)
    @Builder.Default
    private Boolean requiereMaquina = false;

    @Column(name = "url_video", length = 300)
    private String urlVideo;

    // ---------------------------------------------------------------
    //  RELACIONES
    // ---------------------------------------------------------------

    /**
     * MUCHOS A MUCHOS con Rutina, lado inverso.
     *
     * mappedBy = "ejercicios" indica que Rutina es la duena de la
     * relacion y que aqui no se define ninguna tabla adicional.
     */
    @ManyToMany(mappedBy = "ejercicios", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Rutina> rutinas = new HashSet<>();
}
