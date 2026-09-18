package com.gymbro.backend.repository;

import com.gymbro.backend.model.entity.Membresia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** Acceso a datos del catalogo de planes de membresia. */
@Repository
public interface MembresiaRepository extends JpaRepository<Membresia, Long> {

    Optional<Membresia> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

    /**
     * Planes disponibles ordenados de menor a mayor precio.
     * "OrderByPrecioAsc" agrega el ORDER BY sin escribir SQL.
     */
    List<Membresia> findByActivoTrueOrderByPrecioAsc();

    /**
     * Planes dentro de un rango de precio.
     * "Between" se traduce a BETWEEN ? AND ?
     */
    List<Membresia> findByPrecioBetweenAndActivoTrue(BigDecimal minimo, BigDecimal maximo);

    /** Planes que incluyen acompanamiento de un entrenador. */
    List<Membresia> findByIncluyeEntrenadorTrueAndActivoTrue();
}
