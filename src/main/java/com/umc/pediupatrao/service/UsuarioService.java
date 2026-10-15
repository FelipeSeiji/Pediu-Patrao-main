package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Optional<Usuario> buscarPorId(String id) {
        return usuarioRepository.findById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Optional<Usuario> buscarPorUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void salvarUsuario(Usuario usuario) {
        validarPerfil(usuario.getRole());
        if (usuario.getUsername() == null || usuario.getUsername().isBlank()
                || usuario.getPassword() == null || usuario.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuário e senha são obrigatórios.");
        }
        if (usuarioRepository.findByUsername(usuario.getUsername()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nome de usuário já cadastrado.");
        }
        usuario.setRole(usuario.getRole().toUpperCase());
        usuario.setId(null);
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        usuarioRepository.save(usuario);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void deletarUsuario(String id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && usuario.getUsername().equals(authentication.getName())) {
            throw new AccessDeniedException("Não é permitido excluir a própria conta autenticada.");
        }
        long administradores = usuarioRepository.findAll().stream()
                .filter(item -> "ADMIN".equalsIgnoreCase(item.getRole())).count();
        if ("ADMIN".equalsIgnoreCase(usuario.getRole()) && administradores <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O último administrador não pode ser removido.");
        }
        usuarioRepository.deleteById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Iterable<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void atualizarUsuario(String id, Usuario usuarioAtualizado) {
        Usuario existente = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        validarPerfil(usuarioAtualizado.getRole());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String novoPerfil = usuarioAtualizado.getRole().toUpperCase();
        if (authentication != null && existente.getUsername().equals(authentication.getName())
                && !novoPerfil.equalsIgnoreCase(existente.getRole())) {
            throw new AccessDeniedException("Não é permitido alterar o próprio perfil.");
        }
        if (usuarioAtualizado.getUsername() == null || usuarioAtualizado.getUsername().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome de usuário é obrigatório.");
        }
        usuarioRepository.findByUsername(usuarioAtualizado.getUsername()).ifPresent(outro -> {
            if (!outro.getId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Nome de usuário já cadastrado.");
            }
        });
        existente.setUsername(usuarioAtualizado.getUsername());
        existente.setRole(novoPerfil);
        if (usuarioAtualizado.getPassword() != null && !usuarioAtualizado.getPassword().isBlank()) {
            existente.setPassword(passwordEncoder.encode(usuarioAtualizado.getPassword()));
        }
        usuarioRepository.save(existente);
    }

    private void validarPerfil(String role) {
        if (role == null || !(role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("GERENTE")
                || role.equalsIgnoreCase("ATENDENTE"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Perfil inválido.");
        }
    }
}
