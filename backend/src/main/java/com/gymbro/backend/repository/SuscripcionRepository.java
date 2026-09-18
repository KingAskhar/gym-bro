package com.gymbro.backend.repository;

import com.gymbro.backend.model.entity.Suscripcion;
import com.gymbro.backend.model.enums.EstadoSuscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Acceso a datos de las suscripciones de los socios. */
@Repository
public interface SuscripcionRepository extends JpaRepository<Suscripcion, Long> {

    /** Historial completo de suscripciones de un socio, de la mas reciente a la mas antigua. */
    List<Suscripcion> findByUsuario_IdOrderByFechaInicioDesc(Long usuarioId);

    /** Suscripciones de un socio en un estado determinado. */
    List<Suscripcion> findByUsuario_IdAndEstado(Long usuarioId, EstadoSuscripcion estado);

    /**
     * La suscripcion vigente de un socio, si tiene alguna.
     *
     * Aqui el nombre del metodo se volveria ilegible, asi que
     * escribimos la consulta a mano con @Query.
     *
     * Esto es JPQL, no SQL: se consultan clases y atributos de Java
     * (Suscripcion, s.usuario.id) en vez de tablas y columnas. JPA
     * lo traduce a SQL segun la base de datos que se este usando.
     */
    @Query("""
           SELECT s FROM Suscripcion s
           WHERE s.usuario.id = :usuarioId
             AND s.estado = com.gymbro.backend.model.enums.EstadoSuscripcion.VIGENTE
             AND s.fechaFin >= :hoy
             AND s.activo = true
           ORDER BY s.fechaFin DESC
           LIMIT 1
           """)
    Optional<Suscripcion> buscarVigenteDe(@Param("usuarioId") Long usuarioId,
                                          @Param("hoy") LocalDate hoy);

    /**
     * Suscripciones que vencen dentro de un rango de fechas.
     * Sirve para avisar a los socios antes de que se les venza el plan.
     */
    List<Suscripcion> findByEstadoAndFechaFinBetween(
            EstadoSuscripcion estado, LocalDate desde, LocalDate hasta);

    /** Suscripciones vencidas que siguen marcadas como vigentes: hay que actualizarlas. */
    List<Suscripcion> findByEstadoAndFechaFinBefore(
            EstadoSuscripcion estado, LocalDate fecha);

    /** Cuantas suscripciones se vendieron de un plan. Sirve para reportes. */
    long countByMembresia_Id(Long membresiaId);
}
