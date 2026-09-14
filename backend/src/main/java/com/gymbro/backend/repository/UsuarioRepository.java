package com.gymbro.backend.repository;

import com.gymbro.backend.model.entity.Usuario;
import com.gymbro.backend.model.enums.EstadoUsuario;
import com.gymbro.backend.model.enums.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de la entidad Usuario.
 *
 * Es una interfaz, no una clase: no escribimos la implementacion.
 * Spring Data JPA la genera automaticamente al arrancar leyendo
 * los nombres de los metodos.
 *
 * JpaRepository<Usuario, Long> significa: administra entidades de
 * tipo Usuario, cuya llave primaria es de tipo Long. De ahi hereda
 * save(), findById(), findAll(), deleteById() y varios mas.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Busca un usuario por su correo.
     *
     * Spring interpreta el nombre del metodo: "findBy" + "Correo"
     * se traduce a WHERE correo = ?. No hace falta escribir SQL.
     *
     * Devuelve Optional porque puede no existir. Optional obliga a
     * quien lo use a considerar ese caso, en vez de recibir un null
     * que puede reventar mas adelante.
     *
     * Este metodo es la pieza central del login: el primer paso al
     * autenticar es buscar al usuario por su correo.
     */
    Optional<Usuario> findByCorreo(String correo);

    /** Verifica si un correo ya esta registrado, sin traer el usuario completo. */
    boolean existsByCorreo(String correo);

    /** Busca por documento de identidad. La combinacion tipo+numero es unica. */
    Optional<Usuario> findByTipoDocumentoAndNumeroDocumento(
            TipoDocumento tipoDocumento, String numeroDocumento);

    boolean existsByTipoDocumentoAndNumeroDocumento(
            TipoDocumento tipoDocumento, String numeroDocumento);

    /** Lista los usuarios en un estado determinado y que no esten dados de baja. */
    List<Usuario> findByEstadoAndActivoTrue(EstadoUsuario estado);

    /** Todos los usuarios vigentes (no eliminados logicamente). */
    List<Usuario> findByActivoTrue();

    /**
     * Busqueda por nombre o apellido, sin distinguir mayusculas.
     *
     * "Containing" se traduce a LIKE %texto%
     * "IgnoreCase" hace la comparacion insensible a mayusculas.
     * "Or" combina las dos condiciones.
     */
    List<Usuario> findByNombresContainingIgnoreCaseOrApellidosContainingIgnoreCase(
            String nombres, String apellidos);
}
