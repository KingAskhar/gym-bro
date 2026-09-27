package com.gymbro.backend.controller;

import com.gymbro.backend.exception.ReglaNegocioException;
import com.gymbro.backend.model.entity.Usuario;
import com.gymbro.backend.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public ResponseEntity<List<Usuario>> listarTodos() {
        // TODO 7: llamá a usuarioService.obtenerTodos() y envolvé el
        // resultado con ResponseEntity.ok(...). Es una sola línea.
        return ResponseEntity.ok(usuarioService.obtenerTodos());;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Usuario> obtenerPorId(@PathVariable Long id) {
        // TODO 8: igual que arriba, pero llamando a
        // usuarioService.obtenerPorId(id).
        return ResponseEntity.ok(usuarioService.obtenerPorId(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Usuario> eliminar(@PathVariable Long id) {
        // TODO 9: llamá a usuarioService.eliminarLogicamente(id) y
        // devolvé el resultado con ResponseEntity.ok(...).
        //
        // Nota curiosa: aunque el verbo HTTP se llame DELETE, por dentro
        // nunca borramos nada físicamente. Eso es justo lo que estás
        // demostrando con esta regla de negocio.
        return ResponseEntity.ok(usuarioService.eliminarLogicamente(id));
    }

    // Este método ya está completo, no hace falta tocarlo.
    // Convierte cualquier ReglaNegocioException en una respuesta HTTP 400
    // con un mensaje claro, en vez de un error 500 genérico.
    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<Map<String, Object>> manejarReglaNegocio(ReglaNegocioException ex) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.BAD_REQUEST.value());
        error.put("error", "Bad Request");
        error.put("message", ex.getMessage());
        error.put("path", "/api/usuarios");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
