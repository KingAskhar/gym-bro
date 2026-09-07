package com.gymbro.backend.model.entity;

import com.gymbro.backend.model.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Datos laborales de un usuario que trabaja en el gimnasio.
 *
 * Se separa de Usuario porque solo aplica a una minoria de las personas
 * registradas. Mezclarlo dejaria columnas vacias en la mayoria de filas.
 */
@Entity
@Table(
    name = "empleado",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_empleado_usuario", columnNames = "usuario_id"),
        @UniqueConstraint(name = "uq_empleado_codigo", columnNames = "codigo_empleado")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(exclude = "usuario")
public class Empleado extends BaseEntity {

    @Column(name = "codigo_empleado", nullable = false, length = 20)
    private String codigoEmpleado;

    @Column(name = "cargo", nullable = false, length = 80)
    private String cargo;

    /**
     * BigDecimal y no double: los numeros con decimales de punto flotante
     * pierden precision al operar, y con dinero eso es inaceptable.
     * precision=12 y scale=2 permiten hasta 9.999.999.999,99
     */
    @Column(name = "salario", precision = 12, scale = 2)
    private BigDecimal salario;

    @Column(name = "fecha_contratacion", nullable = false)
    private LocalDate fechaContratacion;

    @Column(name = "especialidad", length = 100)
    private String especialidad;

    // ---------------------------------------------------------------
    //  RELACIONES
    // ---------------------------------------------------------------

    /**
     * UNO A UNO con Usuario. Este es el lado propietario: la columna
     * usuario_id vive en esta tabla.
     *
     * fetch = LAZY explicito. En @OneToOne el valor por defecto es EAGER,
     * lo que traeria el usuario completo en cada consulta de empleado
     * aunque no se necesite.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false,
                foreignKey = @jakarta.persistence.ForeignKey(name = "fk_empleado_usuario"))
    private Usuario usuario;
}
