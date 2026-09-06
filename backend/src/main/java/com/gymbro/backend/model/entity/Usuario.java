package com.gymbro.backend.model.entity;

import com.gymbro.backend.model.base.BaseEntity;
import com.gymbro.backend.model.embeddable.Direccion;
import com.gymbro.backend.model.enums.EstadoUsuario;
import com.gymbro.backend.model.enums.TipoDocumento;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Persona registrada en el gimnasio. Puede ser socio, empleado o
 * administrador; el rol se determina por sus relaciones y permisos.
 *
 * Es la entidad central del modelo: de ella cuelgan las suscripciones,
 * las rutinas y, si corresponde, la ficha de empleado.
 */
@Entity
@Table(
    name = "usuario",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_usuario_correo", columnNames = "correo"),
        @UniqueConstraint(name = "uq_usuario_documento",
                          columnNames = {"tipo_documento", "numero_documento"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(exclude = {"empleado", "suscripciones", "rutinas"})
public class Usuario extends BaseEntity {

    @Column(name = "nombres", nullable = false, length = 100)
    private String nombres;

    @Column(name = "apellidos", nullable = false, length = 100)
    private String apellidos;

    /**
     * EnumType.STRING guarda el texto del enum ("CEDULA_CIUDADANIA").
     * Nunca usar ORDINAL, que guarda la posicion (0, 1, 2): si alguien
     * reordena o inserta un valor en el enum, todos los datos ya
     * guardados pasan a significar otra cosa.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 30)
    private TipoDocumento tipoDocumento;

    @Column(name = "numero_documento", nullable = false, length = 20)
    private String numeroDocumento;

    @Column(name = "correo", nullable = false, length = 150)
    private String correo;

    /**
     * Nunca se guarda la contrasena en texto plano, solo su hash BCrypt,
     * que siempre ocupa 60 caracteres.
     */
    @Column(name = "password_hash", nullable = false, length = 60)
    private String passwordHash;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    /**
     * @Embedded incrusta los campos de Direccion como columnas de esta
     * misma tabla (direccion_calle, direccion_ciudad, etc.).
     */
    @Embedded
    private Direccion direccion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoUsuario estado = EstadoUsuario.ACTIVO;

    // ---------------------------------------------------------------
    //  RELACIONES
    // ---------------------------------------------------------------

    /**
     * UNO A UNO con Empleado.
     *
     * Solo algunos usuarios trabajan en el gimnasio. mappedBy indica que
     * la llave foranea vive en la tabla empleado, no aqui.
     */
    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL,
              fetch = FetchType.LAZY, orphanRemoval = true)
    private Empleado empleado;

    /**
     * UNO A MUCHOS con Suscripcion.
     *
     * Un socio contrata planes a lo largo del tiempo; se conserva el
     * historial completo, no solo el vigente.
     */
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL,
               fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<Suscripcion> suscripciones = new ArrayList<>();

    /** UNO A MUCHOS con Rutina. Un socio acumula rutinas a lo largo del tiempo. */
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL,
               fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<Rutina> rutinas = new ArrayList<>();

    // ---------------------------------------------------------------
    //  METODOS DE APOYO
    // ---------------------------------------------------------------

    /**
     * Mantiene sincronizados los dos lados de la relacion.
     * Si solo se hiciera lista.add(...), el objeto en memoria quedaria
     * inconsistente con lo que se guarda en la base de datos.
     */
    public void agregarSuscripcion(Suscripcion suscripcion) {
        suscripciones.add(suscripcion);
        suscripcion.setUsuario(this);
    }

    public void agregarRutina(Rutina rutina) {
        rutinas.add(rutina);
        rutina.setUsuario(this);
    }

    public String getNombreCompleto() {
        return nombres + " " + apellidos;
    }
}
