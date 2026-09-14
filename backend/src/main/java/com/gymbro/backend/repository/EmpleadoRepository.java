package com.gymbro.backend.repository;

import com.gymbro.backend.model.entity.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Acceso a datos de la entidad Empleado. */
@Repository
public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {

    /**
     * Busca la ficha laboral a partir del id del usuario.
     *
     * El guion bajo en "Usuario_Id" le indica a Spring que navegue
     * hacia la entidad relacionada: usuario.id, no un campo llamado
     * "usuarioId" que no existe.
     */
    Optional<Empleado> findByUsuario_Id(Long usuarioId);

    Optional<Empleado> findByCodigoEmpleado(String codigoEmpleado);

    boolean existsByCodigoEmpleado(String codigoEmpleado);

    /** Empleados por cargo, por ejemplo todos los entrenadores. */
    List<Empleado> findByCargoIgnoreCaseAndActivoTrue(String cargo);

    List<Empleado> findByActivoTrue();
}
