package com.gymbro.backend.service;

import com.gymbro.backend.exception.ReglaNegocioException;
import com.gymbro.backend.model.entity.Usuario;
import com.gymbro.backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public List<Usuario> obtenerTodos() {
        return usuarioRepository.findByActivoTrue();
    }

    public Usuario obtenerPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ReglaNegocioException("No se encontró el usuario con ID: " + id));
    }

    @Transactional
    public Usuario eliminarLogicamente(Long id) {
        Usuario usuario = obtenerPorId(id);

        if (!usuario.getActivo()) {
            throw ReglaNegocioException.de("Usuario Ya Eliminado",
                    "El usuario " + usuario.getNombreCompleto() + " ya se encuentra inactivo.");
        }

        usuario.setActivo(false);
        return usuarioRepository.save(usuario);
    }
}