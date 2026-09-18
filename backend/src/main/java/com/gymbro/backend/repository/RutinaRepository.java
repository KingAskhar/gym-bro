package com.gymbro.backend.repository;

import com.gymbro.backend.model.entity.Rutina;
import com.gymbro.backend.model.enums.NivelDificultad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Acceso a datos de las rutinas de entrenamiento. */
@Repository
public interface RutinaRepository extends JpaRepository<Rutina, Long> {

    /** Historial de rutinas de un socio, de la mas reciente a la mas antigua. */
    List<Rutina> findByUsuario_IdOrderByFechaAsignacionDesc(Long usuarioId);

    /** Rutinas vigentes de un socio. */
    List<Rutina> findByUsuario_IdAndActivoTrue(Long usuarioId);

    List<Rutina> findByNivelAndActivoTrue(NivelDificultad nivel);

    /**
     * Trae una rutina junto con todos sus ejercicios en una sola consulta.
     *
     * Por que hace falta: la relacion con ejercicios es LAZY, asi que
     * normalmente no se cargan. Si se intentan leer despues de cerrada
     * la sesion, la aplicacion falla.
     *
     * JOIN FETCH le pide a JPA que los traiga de una vez. Sin esto,
     * cargar 20 rutinas con sus ejercicios dispararia 21 consultas
     * en lugar de 1: el problema conocido como "N+1".
     */
    @Query("""
           SELECT r FROM Rutina r
           LEFT JOIN FETCH r.ejercicios
           WHERE r.id = :rutinaId
           """)
    Optional<Rutina> buscarConEjercicios(@Param("rutinaId") Long rutinaId);

    /** Cuantas rutinas usan un ejercicio determinado. */
    @Query("""
           SELECT COUNT(r) FROM Rutina r
           JOIN r.ejercicios e
           WHERE e.id = :ejercicioId
           """)
    long contarRutinasQueUsan(@Param("ejercicioId") Long ejercicioId);
}
