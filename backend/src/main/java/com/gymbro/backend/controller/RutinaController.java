package com.gymbro.backend.controller;

import com.gymbro.backend.exception.ReglaNegocioException;
import com.gymbro.backend.model.entity.Rutina;
import com.gymbro.backend.model.enums.NivelDificultad;
import com.gymbro.backend.service.RutinaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST de rutinas de entrenamiento.
 *
 * El controlador no valida reglas de negocio: solo recibe la peticion
 * HTTP, extrae los datos y se los pasa a RutinaService. Si el servicio
 * detecta que se incumple una regla, lanza ReglaNegocioException y el
 * @ExceptionHandler de abajo la convierte en una respuesta 400.
 */
@RestController
@RequestMapping("/api/rutinas")
public class RutinaController {

    private final RutinaService rutinaService;

    public RutinaController(RutinaService rutinaService) {
        this.rutinaService = rutinaService;
    }

    /**
     * GET /api/rutinas
     *
     * Los tres filtros son opcionales y se combinan entre si:
     *   /api/rutinas
     *   /api/rutinas?usuarioId=2
     *   /api/rutinas?nivel=PRINCIPIANTE
     *   /api/rutinas?usuarioId=2&soloActivas=true
     *
     * required = false es lo que los hace opcionales. Sin eso, Spring
     * respondaria 400 cuando el parametro no viniera en la URL.
     */
    @GetMapping
    public ResponseEntity<List<Rutina>> listar(
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) NivelDificultad nivel,
            @RequestParam(required = false) Boolean soloActivas) {
        return ResponseEntity.ok(rutinaService.listar(usuarioId, nivel, soloActivas));
    }

    /**
     * GET /api/rutinas/{id}
     *
     * @PathVariable toma el valor que venga en esa posicion de la ruta.
     * En /api/rutinas/7 el id vale 7.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Rutina> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(rutinaService.obtenerPorId(id));
    }

    /**
     * GET /api/rutinas/usuario/{usuarioId}/historial
     *
     * Devuelve todas las rutinas que tuvo un socio, activas e inactivas,
     * de la mas reciente a la mas antigua. Sirve para demostrar que las
     * rutinas anteriores siguen existiendo despues de aplicar RN-03.
     */
    @GetMapping("/usuario/{usuarioId}/historial")
    public ResponseEntity<List<Rutina>> historial(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(rutinaService.historialDe(usuarioId));
    }

    /**
     * POST /api/rutinas
     *
     * Aqui se demuestra RN-03. El cuerpo JSON llega en @RequestBody y
     * Spring lo convierte en un objeto Rutina.
     *
     * Devuelve 201 Created, que es el codigo correcto cuando una
     * peticion crea un recurso nuevo (200 OK seria "todo bien" a secas).
     */
    @PostMapping
    public ResponseEntity<Rutina> asignar(@RequestBody Rutina rutina) {
        Rutina creada = rutinaService.asignarRutina(rutina);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    /**
     * PUT /api/rutinas/{id}
     *
     * Combina los dos mecanismos: el id viaja en la ruta y los datos
     * nuevos en el cuerpo JSON.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Rutina> actualizar(@PathVariable Long id, @RequestBody Rutina datos) {
        return ResponseEntity.ok(rutinaService.actualizar(id, datos));
    }

    /**
     * DELETE /api/rutinas/{id}
     *
     * El verbo HTTP se llama DELETE, pero por dentro no se borra nada:
     * la rutina queda con activo = false. Se devuelve el objeto
     * actualizado (y no 204 No Content) justamente para que en la
     * sustentacion se vea el campo activo en false en la respuesta.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Rutina> desactivar(@PathVariable Long id) {
        return ResponseEntity.ok(rutinaService.desactivar(id));
    }

    /**
     * Convierte cualquier ReglaNegocioException lanzada por el servicio
     * en una respuesta JSON uniforme con codigo 400 Bad Request, en vez
     * de un error 500 generico con el stack trace de Java.
     */
    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<Map<String, Object>> manejarReglaNegocio(ReglaNegocioException ex) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.BAD_REQUEST.value());
        error.put("error", "Bad Request");
        error.put("message", ex.getMessage());
        error.put("path", "/api/rutinas");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
