package com.gymbro.backend.repository;

import com.gymbro.backend.model.entity.Ejercicio;
import com.gymbro.backend.model.enums.GrupoMuscular;
import com.gymbro.backend.model.enums.NivelDificultad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Acceso a datos del catalogo de ejercicios. */
@Repository
public interface EjercicioRepository extends JpaRepository<Ejercicio, Long> {

    Optional<Ejercicio> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

    /** Ejercicios de un grupo muscular, ordenados alfabeticamente. */
    List<Ejercicio> findByGrupoMuscularAndActivoTrueOrderByNombreAsc(GrupoMuscular grupoMuscular);

    List<Ejercicio> findByNivelAndActivoTrue(NivelDificultad nivel);

    /** Ejercicios que se pueden hacer sin maquinas. Util para rutinas en casa. */
    List<Ejercicio> findByRequiereMaquinaFalseAndActivoTrue();

    /**
     * Busqueda por nombre parcial.
     * "In" recibe una lista y se traduce a WHERE grupo_muscular IN (...)
     */
    List<Ejercicio> findByNombreContainingIgnoreCaseAndActivoTrue(String texto);

    List<Ejercicio> findByGrupoMuscularInAndActivoTrue(List<GrupoMuscular> grupos);
}
