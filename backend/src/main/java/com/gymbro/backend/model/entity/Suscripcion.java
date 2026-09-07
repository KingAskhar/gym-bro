package com.gymbro.backend.model.entity;

import com.gymbro.backend.model.base.BaseEntity;
import com.gymbro.backend.model.enums.EstadoSuscripcion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Contrato entre un socio y un plan de membresia durante un periodo.
 *
 * Es la tabla intermedia entre Usuario y Membresia, pero con datos
 * propios (fechas, precio pagado, estado), asi que se modela como una
 * entidad y no como un simple @ManyToMany.
 */
@Entity
@Table(name = "suscripcion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(exclude = {"usuario", "membresia"})
public class Suscripcion extends BaseEntity {

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    /**
     * Se guarda el precio del momento de la compra. Si manana sube el
     * valor del plan, las suscripciones ya vendidas no deben cambiar.
     */
    @Column(name = "precio_pagado", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioPagado;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoSuscripcion estado = EstadoSuscripcion.PENDIENTE_PAGO;

    // ---------------------------------------------------------------
    //  RELACIONES
    // ---------------------------------------------------------------

    /** MUCHOS A UNO: muchas suscripciones pertenecen a un mismo socio. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_suscripcion_usuario"))
    private Usuario usuario;

    /** MUCHOS A UNO: muchas suscripciones apuntan al mismo plan. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membresia_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_suscripcion_membresia"))
    private Membresia membresia;

    // ---------------------------------------------------------------
    //  CAMPO CALCULADO
    // ---------------------------------------------------------------

    /**
     * @Transient indica que este atributo NO se guarda como columna.
     * Se calcula cada vez a partir de la fecha de fin, porque un valor
     * asi cambia todos los dias y almacenarlo obligaria a actualizarlo.
     */
    @Transient
    public long getDiasRestantes() {
        if (fechaFin == null) {
            return 0;
        }
        long dias = ChronoUnit.DAYS.between(LocalDate.now(), fechaFin);
        return Math.max(dias, 0);
    }

    @Transient
    public boolean estaVigente() {
        return estado == EstadoSuscripcion.VIGENTE && getDiasRestantes() > 0;
    }
}
