package com.gymbro.backend.model.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Direccion fisica de una persona.
 *
 * @Embeddable indica que esta clase no es una entidad: no tiene tabla
 * ni identificador propio. Sus campos se guardan como columnas dentro
 * de la tabla de la entidad que la contiene.
 *
 * Se modela asi porque una direccion no existe por si sola: siempre
 * pertenece a alguien. Separar la clase evita repetir cinco atributos
 * en cada entidad que necesite domicilio.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Direccion {

    @Column(name = "direccion_calle", length = 150)
    private String calle;

    @Column(name = "direccion_barrio", length = 100)
    private String barrio;

    @Column(name = "direccion_ciudad", length = 100)
    private String ciudad;

    @Column(name = "direccion_departamento", length = 100)
    private String departamento;

    @Column(name = "direccion_codigo_postal", length = 20)
    private String codigoPostal;
}
