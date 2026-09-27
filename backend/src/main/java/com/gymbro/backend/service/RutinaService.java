package com.gymbro.backend.service;

import com.gymbro.backend.exception.ReglaNegocioException;
import com.gymbro.backend.model.entity.Rutina;
import com.gymbro.backend.model.entity.Usuario;
import com.gymbro.backend.model.enums.EstadoUsuario;
import com.gymbro.backend.model.enums.NivelDificultad;
import com.gymbro.backend.repository.RutinaRepository;
import com.gymbro.backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Logica de negocio de las rutinas de entrenamiento.
 *
 * Aqui vive la regla RN-03: un socio solo puede tener una rutina
 * activa a la vez. Cuando se le asigna una nueva, la anterior no se
 * borra: se marca como inactiva para conservar el historial.
 */
@Service
public class RutinaService {

    private final RutinaRepository rutinaRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Hacen falta los dos repositorios: uno para leer y guardar rutinas,
     * y otro para verificar contra la base que el socio exista y este
     * en estado ACTIVO.
     */
    public RutinaService(RutinaRepository rutinaRepository,
                         UsuarioRepository usuarioRepository) {
        this.rutinaRepository = rutinaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Lista rutinas con filtros opcionales combinables.
     *
     * Los tres parametros pueden venir en null. Cada filtro solo se
     * aplica si su parametro trae valor, asi que una sola consulta
     * cubre todas las combinaciones posibles sin duplicar endpoints.
     */
    public List<Rutina> listar(Long usuarioId, NivelDificultad nivel, Boolean soloActivas) {
        return rutinaRepository.findAll().stream()
                .filter(r -> usuarioId == null
                        || (r.getUsuario() != null && usuarioId.equals(r.getUsuario().getId())))
                .filter(r -> nivel == null || nivel.equals(r.getNivel()))
                .filter(r -> soloActivas == null || !soloActivas || Boolean.TRUE.equals(r.getActivo()))
                .toList();
    }

    /**
     * Busca una rutina por su id.
     *
     * findById devuelve un Optional, que es una caja que puede venir
     * vacia. orElseThrow la abre: si trae la rutina la devuelve, y si
     * viene vacia lanza la excepcion que le pasamos.
     */
    public Rutina obtenerPorId(Long id) {
        return rutinaRepository.findById(id)
                .orElseThrow(() -> new ReglaNegocioException(
                        "No se encontro la rutina con ID: " + id));
    }

    /** Historial completo de un socio, de la mas reciente a la mas antigua. */
    public List<Rutina> historialDe(Long usuarioId) {
        return rutinaRepository.findByUsuario_IdOrderByFechaAsignacionDesc(usuarioId);
    }

    /**
     * REGLA DE NEGOCIO RN-03: un usuario solo puede tener una rutina activa.
     *
     * El orden de los pasos importa. Primero se valida todo y solo
     * despues se desactiva la rutina anterior. Si se hiciera al reves y
     * la validacion fallara, el socio quedaria sin ninguna rutina activa
     * por culpa de una peticion que ni siquiera prospero.
     *
     * @Transactional agrupa las escrituras en una sola operacion: se
     * aplican todas o no se aplica ninguna. Sin esto, un fallo a mitad
     * de camino podria dejar la rutina vieja apagada y la nueva sin guardar.
     */
    @Transactional
    public Rutina asignarRutina(Rutina rutina) {

        // Paso 1: validar que la peticion identifique a un socio.
        if (rutina.getUsuario() == null || rutina.getUsuario().getId() == null) {
            throw ReglaNegocioException.de("Usuario Requerido",
                    "Debe indicar el ID del usuario al que se le asigna la rutina.");
        }

        Long usuarioId = rutina.getUsuario().getId();

        // Paso 2: cargar el usuario real desde la base.
        // El que viene en el JSON es un objeto suelto que solo trae el id;
        // no tiene el estado ni el nombre, asi que no sirve para validar.
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> ReglaNegocioException.de("Usuario Inexistente",
                        "No se encontro el usuario con ID: " + usuarioId));

        // Paso 3: el socio debe estar ACTIVO.
        // Un socio suspendido o inactivo no puede recibir rutinas nuevas.
        if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
            throw ReglaNegocioException.de("Usuario No Activo",
                    "El usuario " + usuario.getNombreCompleto() + " se encuentra en estado "
                            + usuario.getEstado() + " y no puede recibir una rutina nueva.");
        }

        // Paso 4: validar los campos que la base exige obligatorios.
        // Si no se validan aqui, el fallo llegaria como un error crudo de
        // base de datos en vez de un mensaje de negocio entendible.
        if (rutina.getNombre() == null || rutina.getNombre().isBlank()) {
            throw ReglaNegocioException.de("Nombre Requerido",
                    "La rutina debe tener un nombre.");
        }
        if (rutina.getDiasPorSemana() == null
                || rutina.getDiasPorSemana() < 1 || rutina.getDiasPorSemana() > 7) {
            throw ReglaNegocioException.de("Dias Invalidos",
                    "Los dias por semana deben estar entre 1 y 7.");
        }

        // Paso 5: desactivar la rutina activa anterior, si la hay.
        // Es una lista y no una sola rutina por precaucion: si por algun
        // error previo quedaron dos activas, este recorrido las apaga todas.
        // No se borran, solo se marcan inactivas: el historial se conserva.
        List<Rutina> activas = rutinaRepository.findByUsuario_IdAndActivoTrue(usuarioId);
        for (Rutina anterior : activas) {
            anterior.setActivo(false);
            rutinaRepository.save(anterior);
        }

        // Paso 6: dejar lista la rutina nueva y guardarla.
        rutina.setUsuario(usuario);
        rutina.setActivo(true);
        if (rutina.getFechaAsignacion() == null) {
            rutina.setFechaAsignacion(LocalDate.now());
        }

        return rutinaRepository.save(rutina);
    }

    /**
     * Actualiza los datos de una rutina existente.
     *
     * No se reemplaza el objeto entero: se cargan los datos actuales y
     * se sobreescriben campo por campo. Asi se conservan el usuario, la
     * fecha de creacion y el estado activo, que no deben cambiar por una
     * edicion de nombre u objetivo.
     */
    @Transactional
    public Rutina actualizar(Long id, Rutina datos) {
        Rutina rutina = obtenerPorId(id);

        if (datos.getNombre() != null && !datos.getNombre().isBlank()) {
            rutina.setNombre(datos.getNombre());
        }
        if (datos.getObjetivo() != null) {
            rutina.setObjetivo(datos.getObjetivo());
        }
        if (datos.getNivel() != null) {
            rutina.setNivel(datos.getNivel());
        }
        if (datos.getDiasPorSemana() != null) {
            if (datos.getDiasPorSemana() < 1 || datos.getDiasPorSemana() > 7) {
                throw ReglaNegocioException.de("Dias Invalidos",
                        "Los dias por semana deben estar entre 1 y 7.");
            }
            rutina.setDiasPorSemana(datos.getDiasPorSemana());
        }
        if (datos.getFechaVencimiento() != null) {
            rutina.setFechaVencimiento(datos.getFechaVencimiento());
        }

        return rutinaRepository.save(rutina);
    }

    /**
     * Desactiva una rutina sin borrarla (RN-05 aplicada a rutinas).
     *
     * Es el mismo criterio de eliminacion logica que rige en todo el
     * sistema: el registro permanece en la base con activo = false.
     */
    @Transactional
    public Rutina desactivar(Long id) {
        Rutina rutina = obtenerPorId(id);

        if (!Boolean.TRUE.equals(rutina.getActivo())) {
            throw ReglaNegocioException.de("Rutina Ya Inactiva",
                    "La rutina '" + rutina.getNombre() + "' ya se encuentra inactiva.");
        }

        rutina.setActivo(false);
        return rutinaRepository.save(rutina);
    }
}
