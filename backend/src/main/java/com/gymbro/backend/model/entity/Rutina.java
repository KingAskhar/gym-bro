package com.gymbro.backend.model.entity;

import com.gymbro.backend.model.base.BaseEntity;
import com.gymbro.backend.model.enums.NivelDificultad;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Plan de entrenamiento asignado a un socio.
 *
 * Agrupa varios ejercicios y pertenece a un unico usuario.
 */
@Entity
@Table(name = "rutina")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(exclude = {"usuario", "ejercicios"})
public class Rutina extends BaseEntity {

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "objetivo", length = 300)
    private String objetivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel", nullable = false, length = 20)
    @Builder.Default
    private NivelDificultad nivel = NivelDificultad.PRINCIPIANTE;

    @Column(name = "dias_por_semana", nullable = false)
    private Integer diasPorSemana;

    @Column(name = "fecha_asignacion", nullable = false)
    private LocalDate fechaAsignacion;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    // ---------------------------------------------------------------
    //  RELACIONES
    // ---------------------------------------------------------------

    /** MUCHOS A UNO: un socio puede tener varias rutinas a lo largo del tiempo. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_rutina_usuario"))
    private Usuario usuario;

    /**
     * MUCHOS A MUCHOS con Ejercicio.
     *
     * Una rutina incluye varios ejercicios, y un mismo ejercicio aparece
     * en muchas rutinas. JPA crea una tabla intermedia (rutina_ejercicio)
     * con las dos llaves foraneas.
     *
     * Este es el lado propietario: es el que define @JoinTable.
     *
     * Se usa Set y no List: evita duplicados y funciona mejor que List
     * en relaciones muchos a muchos, donde JPA borraria y reinsertaria
     * todas las filas ante cualquier cambio.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "rutina_ejercicio",
        joinColumns = @JoinColumn(name = "rutina_id",
                        foreignKey = @ForeignKey(name = "fk_rutina_ejercicio_rutina")),
        inverseJoinColumns = @JoinColumn(name = "ejercicio_id",
                        foreignKey = @ForeignKey(name = "fk_rutina_ejercicio_ejercicio"))
    )
    @Builder.Default
    private Set<Ejercicio> ejercicios = new HashSet<>();

    // ---------------------------------------------------------------
    //  METODOS DE APOYO
    // ---------------------------------------------------------------

    /** Mantiene sincronizados los dos lados de la relacion. */
    public void agregarEjercicio(Ejercicio ejercicio) {
        ejercicios.add(ejercicio);
        ejercicio.getRutinas().add(this);
    }

    public void quitarEjercicio(Ejercicio ejercicio) {
        ejercicios.remove(ejercicio);
        ejercicio.getRutinas().remove(this);
    }
}
