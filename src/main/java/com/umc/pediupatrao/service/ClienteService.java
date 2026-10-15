package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.repository.ClienteRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ClienteService {
    private final ClienteRepository clienteRepository;
    private final AuditoriaService auditoriaService;

    public ClienteService(ClienteRepository clienteRepository, AuditoriaService auditoriaService) {
        this.clienteRepository = clienteRepository;
        this.auditoriaService = auditoriaService;
    }

    @PreAuthorize("hasRole('ATENDENTE')")
    public Cliente novoCliente(Cliente cliente) {
        cliente.setId(null);
        return salvar(cliente);
    }

    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    public List<Cliente> listarClientes() {
        return clienteRepository.findAll();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void excluir(String id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado."));
        auditoriaService.registrar("EXCLUSAO DE CLIENTE", "CLIENTE", id, snapshot(cliente), Map.of(), null);
        clienteRepository.deleteById(id);
    }

    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    public Optional<Cliente> buscarPorId(String id) {
        return clienteRepository.findById(id);
    }

    @PreAuthorize("hasRole('ATENDENTE')")
    public Cliente salvar(Cliente cliente) {
        if (cliente.getNome() == null || cliente.getNome().isBlank()
                || cliente.getTelefone() == null || cliente.getTelefone().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome e telefone são obrigatórios.");
        }
        if (cliente.getId() == null) {
            Cliente novo = clienteRepository.save(cliente);
            auditoriaService.registrar("INCLUSAO DE CLIENTE", "CLIENTE", novo.getId(), Map.of(), snapshot(novo), null);
            return novo;
        }
        Cliente existente = clienteRepository.findById(cliente.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado para atualização."));
        Map<String, Object> anteriores = snapshot(existente);
        existente.setNome(cliente.getNome());
        existente.setTelefone(cliente.getTelefone());
        existente.setCep(cliente.getCep());
        existente.setLogradouro(cliente.getLogradouro());
        existente.setNumero(cliente.getNumero());
        existente.setComplemento(cliente.getComplemento());
        existente.setBairro(cliente.getBairro());
        existente.setCidade(cliente.getCidade());
        existente.setEstado(cliente.getEstado());
        existente.setEndereco(cliente.getEndereco());
        Cliente atualizado = clienteRepository.save(existente);
        auditoriaService.registrar("ALTERACAO DE CLIENTE", "CLIENTE", atualizado.getId(), anteriores, snapshot(atualizado), null);
        return atualizado;
    }

    private Map<String, Object> snapshot(Cliente cliente) {
        Map<String, Object> valores = new LinkedHashMap<>();
        valores.put("nome", cliente.getNome());
        valores.put("telefone", cliente.getTelefone());
        valores.put("cep", cliente.getCep());
        valores.put("logradouro", cliente.getLogradouro());
        valores.put("numero", cliente.getNumero());
        valores.put("complemento", cliente.getComplemento());
        valores.put("bairro", cliente.getBairro());
        valores.put("cidade", cliente.getCidade());
        valores.put("estado", cliente.getEstado());
        valores.put("endereco", cliente.getEndereco());
        return valores;
    }
}