package com.gymbro.backend.config;

import com.gymbro.backend.model.embeddable.Direccion;
import com.gymbro.backend.model.entity.Ejercicio;
import com.gymbro.backend.model.entity.Membresia;
import com.gymbro.backend.model.entity.Rutina;
import com.gymbro.backend.model.entity.Suscripcion;
import com.gymbro.backend.model.entity.Usuario;
import com.gymbro.backend.model.enums.EstadoSuscripcion;
import com.gymbro.backend.model.enums.EstadoUsuario;
import com.gymbro.backend.model.enums.GrupoMuscular;
import com.gymbro.backend.model.enums.NivelDificultad;
import com.gymbro.backend.model.enums.TipoDocumento;
import com.gymbro.backend.repository.EjercicioRepository;
import com.gymbro.backend.repository.MembresiaRepository;
import com.gymbro.backend.repository.RutinaRepository;
import com.gymbro.backend.repository.SuscripcionRepository;
import com.gymbro.backend.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Siembra datos de prueba al arrancar la aplicacion.
 *
 * Existe para que la demostracion de las reglas de negocio sea
 * reproducible: cada caso de exito y de error tiene de antemano un
 * registro preparado, en vez de tener que crearlo a mano.
 *
 * CommandLineRunner es una interfaz de Spring Boot: el metodo run()
 * se ejecuta una sola vez, justo despues de que la aplicacion termina
 * de arrancar y antes de atender peticiones.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    /**
     * Hash BCrypt de la contrasena "password123".
     * Nunca se guarda la contrasena en texto plano, ni siquiera en
     * datos de prueba. BCrypt siempre produce 60 caracteres.
     */
    private static final String PASSWORD_DEMO =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private final UsuarioRepository usuarioRepository;
    private final MembresiaRepository membresiaRepository;
    private final SuscripcionRepository suscripcionRepository;
    private final EjercicioRepository ejercicioRepository;
    private final RutinaRepository rutinaRepository;

    public DataInitializer(UsuarioRepository usuarioRepository,
                           MembresiaRepository membresiaRepository,
                           SuscripcionRepository suscripcionRepository,
                           EjercicioRepository ejercicioRepository,
                           RutinaRepository rutinaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.membresiaRepository = membresiaRepository;
        this.suscripcionRepository = suscripcionRepository;
        this.ejercicioRepository = ejercicioRepository;
        this.rutinaRepository = rutinaRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {

        /*
         * Verificacion imprescindible: la base de datos es persistente,
         * asi que sin este control se duplicarian los datos en cada
         * arranque y el correo unico haria fallar la aplicacion.
         */
        if (usuarioRepository.count() > 0) {
            log.info("La base ya tiene datos. No se siembra nada.");
            return;
        }

        log.info("Base vacia. Sembrando datos de prueba...");

        sembrarMembresias();
        sembrarEjercicios();
        sembrarUsuarios();

        log.info("Datos de prueba listos.");
    }

    // ---------------------------------------------------------------
    //  CATALOGOS
    // ---------------------------------------------------------------

    private void sembrarMembresias() {
        Membresia mensual = Membresia.builder()
                .nombre("Plan Mensual")
                .descripcion("Acceso a sala de maquinas durante 30 dias.")
                .precio(new BigDecimal("80000.00"))
                .duracionDias(30)
                .incluyeClasesGrupales(false)
                .incluyeEntrenador(false)
                .build();

        Membresia anual = Membresia.builder()
                .nombre("Plan Anual Premium")
                .descripcion("Acceso completo, clases grupales y entrenador asignado.")
                .precio(new BigDecimal("750000.00"))
                .duracionDias(365)
                .incluyeClasesGrupales(true)
                .incluyeEntrenador(true)
                .build();

        membresiaRepository.saveAll(List.of(mensual, anual));
        log.info("  2 membresias creadas");
    }

    private void sembrarEjercicios() {
        Ejercicio sentadilla = Ejercicio.builder()
                .nombre("Sentadilla con barra")
                .descripcion("Ejercicio compuesto para tren inferior.")
                .grupoMuscular(GrupoMuscular.PIERNAS)
                .nivel(NivelDificultad.INTERMEDIO)
                .seriesSugeridas(4)
                .repeticionesSugeridas(10)
                .requiereMaquina(true)
                .build();

        Ejercicio pressBanca = Ejercicio.builder()
                .nombre("Press de banca")
                .descripcion("Ejercicio principal de pecho.")
                .grupoMuscular(GrupoMuscular.PECHO)
                .nivel(NivelDificultad.INTERMEDIO)
                .seriesSugeridas(4)
                .repeticionesSugeridas(8)
                .requiereMaquina(true)
                .build();

        Ejercicio flexiones = Ejercicio.builder()
                .nombre("Flexiones de pecho")
                .descripcion("Ejercicio de peso corporal, no requiere equipo.")
                .grupoMuscular(GrupoMuscular.PECHO)
                .nivel(NivelDificultad.PRINCIPIANTE)
                .seriesSugeridas(3)
                .repeticionesSugeridas(15)
                .requiereMaquina(false)
                .build();

        Ejercicio plancha = Ejercicio.builder()
                .nombre("Plancha abdominal")
                .descripcion("Ejercicio isometrico de core.")
                .grupoMuscular(GrupoMuscular.ABDOMEN)
                .nivel(NivelDificultad.PRINCIPIANTE)
                .seriesSugeridas(3)
                .repeticionesSugeridas(1)
                .requiereMaquina(false)
                .build();

        ejercicioRepository.saveAll(
                List.of(sentadilla, pressBanca, flexiones, plancha));
        log.info("  4 ejercicios creados");
    }

    // ---------------------------------------------------------------
    //  USUARIOS PREPARADOS PARA CADA CASO DE LA DEMOSTRACION
    // ---------------------------------------------------------------

    private void sembrarUsuarios() {

        Membresia planAnual = membresiaRepository
                .findByNombreIgnoreCase("Plan Anual Premium")
                .orElseThrow();

        /*
         * CASO RN-03, exito 1: socio activo que todavia no tiene rutina.
         * Al asignarle una, debe quedar activa sin desactivar nada.
         */
        Usuario sinRutina = crear("Laura", "Gomez", "1010101010",
                "laura.gomez@correo.com", EstadoUsuario.ACTIVO, true);

        /*
         * CASO RN-03, exito 2: socio que YA tiene una rutina activa.
         * Al asignarle una segunda, la primera debe desactivarse sola.
         * CASO RN-05, exito: ademas tiene una suscripcion, para poder
         * comprobar que al eliminarlo logicamente no se pierde.
         */
        Usuario conRutina = crear("Carlos", "Mejia", "2020202020",
                "carlos.mejia@correo.com", EstadoUsuario.ACTIVO, true);

        /*
         * CASO RN-03, error 2: socio suspendido.
         * No se le puede asignar ninguna rutina.
         */
        Usuario suspendido = crear("Andres", "Torres", "3030303030",
                "andres.torres@correo.com", EstadoUsuario.SUSPENDIDO, true);

        /*
         * CASO RN-05, error 2: usuario ya eliminado logicamente.
         * Intentar eliminarlo otra vez debe rechazarse.
         */
        Usuario yaEliminado = crear("Sofia", "Ramirez", "4040404040",
                "sofia.ramirez@correo.com", EstadoUsuario.INACTIVO, false);

        usuarioRepository.saveAll(
                List.of(sinRutina, conRutina, suspendido, yaEliminado));

        // Rutina activa para Carlos (necesaria para el caso de exito 2)
        Rutina rutinaInicial = Rutina.builder()
                .nombre("Rutina de adaptacion")
                .objetivo("Acondicionamiento general para retomar el entrenamiento.")
                .nivel(NivelDificultad.PRINCIPIANTE)
                .diasPorSemana(3)
                .fechaAsignacion(LocalDate.now().minusMonths(1))
                .fechaVencimiento(LocalDate.now().plusMonths(1))
                .usuario(conRutina)
                .build();

        rutinaRepository.save(rutinaInicial);

        // Suscripcion vigente para Carlos (para comprobar la RN-05)
        Suscripcion suscripcion = Suscripcion.builder()
                .usuario(conRutina)
                .membresia(planAnual)
                .fechaInicio(LocalDate.now().minusMonths(2))
                .fechaFin(LocalDate.now().plusMonths(10))
                .precioPagado(planAnual.getPrecio())
                .estado(EstadoSuscripcion.VIGENTE)
                .build();

        suscripcionRepository.save(suscripcion);

        log.info("  4 usuarios, 1 rutina y 1 suscripcion creados");
        log.info("  Laura Gomez    -> ACTIVO, sin rutina  (RN-03 exito 1)");
        log.info("  Carlos Mejia   -> ACTIVO, con rutina  (RN-03 exito 2 / RN-05 exito)");
        log.info("  Andres Torres  -> SUSPENDIDO          (RN-03 error 2)");
        log.info("  Sofia Ramirez  -> ya eliminada        (RN-05 error 2)");
    }

    /** Arma un usuario con los datos minimos y una direccion de ejemplo. */
    private Usuario crear(String nombres, String apellidos, String documento,
                          String correo, EstadoUsuario estado, boolean activo) {

        Direccion direccion = Direccion.builder()
                .calle("Calle 50 #45-20")
                .barrio("Niquia")
                .ciudad("Bello")
                .departamento("Antioquia")
                .codigoPostal("051050")
                .build();

        return Usuario.builder()
                .nombres(nombres)
                .apellidos(apellidos)
                .tipoDocumento(TipoDocumento.CEDULA_CIUDADANIA)
                .numeroDocumento(documento)
                .correo(correo)
                .passwordHash(PASSWORD_DEMO)
                .telefono("3001234567")
                .fechaNacimiento(LocalDate.of(1998, 5, 20))
                .direccion(direccion)
                .estado(estado)
                .activo(activo)
                .build();
    }
}