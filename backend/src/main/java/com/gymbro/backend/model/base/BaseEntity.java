package com.gymbro.backend.model.base;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Clase base de la que heredan todas las entidades del dominio.
 *
 * @MappedSuperclass significa que esta clase NO genera una tabla propia.
 * Sus atributos se copian como columnas en cada tabla hija. Asi evitamos
 * repetir el id y los campos de auditoria en las seis entidades.
 */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class BaseEntity {

    /**
     * IDENTITY delega la generacion del numero a PostgreSQL,
     * que usa una columna autoincremental.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** La escribe Hibernate al insertar. updatable=false impide modificarla luego. */
    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    /** Hibernate la actualiza en cada UPDATE. */
    @UpdateTimestamp
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    /**
     * Borrado logico: en vez de eliminar la fila, se marca como inactiva.
     * Asi no se pierde el historial ni se rompen las relaciones.
     */
    @Builder.Default
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    /**
     * Bloqueo optimista. Si dos usuarios editan el mismo registro a la vez,
     * el segundo recibe un error en lugar de pisar el cambio del primero.
     */
    @Version
    @Column(name = "version")
    private Long version;

    /**
     * equals y hashCode escritos a mano, no generados por Lombok.
     *
     * Por que: Lombok compararia todos los atributos, incluidas las
     * relaciones, y eso dispara consultas a la base de datos sin querer.
     * Comparamos solo por id, que es la identidad real del registro.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseEntity otra)) return false;
        return id != null && Objects.equals(id, otra.getId());
    }

    /**
     * Valor constante a proposito: el id es null antes de guardar y cambia
     * despues. Si el hashCode dependiera de el, el objeto se "perderia"
     * dentro de un HashSet al momento de persistirlo.
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
