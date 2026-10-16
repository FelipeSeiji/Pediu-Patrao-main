package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Entregador;
import com.umc.pediupatrao.repository.EntregadorRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
public class EntregadorService {
    private final EntregadorRepository repository;

    public EntregadorService(EntregadorRepository repository) {
        this.repository = repository;
    }

    public List<Entregador> listar() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "nome"));
    }

    public Optional<Entregador> buscarPorId(String id) {
        return repository.findById(id);
    }

    public Entregador salvar(Entregador entregador) {
        if (entregador.getNome() == null || entregador.getNome().isBlank()
                || entregador.getTelefone() == null || entregador.getTelefone().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome e telefone são obrigatórios.");
        }
        // Thymeleaf can submit the hidden ID as an empty string on the create form.
        // Treat that value as a new record instead of looking it up in MongoDB.
        if (entregador.getId() != null && entregador.getId().isBlank()) {
            entregador.setId(null);
        }
        if (entregador.getId() != null && !repository.existsById(entregador.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Entregador não encontrado.");
        }
        entregador.setNome(entregador.getNome().trim());
        entregador.setTelefone(entregador.getTelefone().trim());
        if (entregador.getAtivo() == null) {
            entregador.setAtivo(true);
        }
        return repository.save(entregador);
    }

    public Entregador alternarAtivo(String id) {
        Entregador entregador = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entregador não encontrado."));
        entregador.setAtivo(!Boolean.TRUE.equals(entregador.getAtivo()));
        return repository.save(entregador);
    }
}
