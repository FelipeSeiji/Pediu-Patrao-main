package com.umc.pediupatrao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.repository.ClienteRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private AuditoriaService auditoriaService;

    private ClienteService clienteService;

    @BeforeEach
    void setUp() {
        clienteService = new ClienteService(clienteRepository, auditoriaService);
    }

    @AfterEach
    void limparAutenticacao() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void alteracaoAuditaTelefoneEEnderecoAnteriorENovo() {
        autenticar("atendente", "ATENDENTE");
        Cliente existente = new Cliente();
        existente.setId("cliente-1");
        existente.setNome("Maria");
        existente.setTelefone("1111-1111");
        existente.setLogradouro("Rua antiga");
        when(clienteRepository.findById("cliente-1")).thenReturn(Optional.of(existente));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cliente alterado = new Cliente();
        alterado.setId("cliente-1");
        alterado.setNome("Maria");
        alterado.setTelefone("2222-2222");
        alterado.setLogradouro("Rua nova");
        clienteService.salvar(alterado);

        ArgumentCaptor<Map> anteriores = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map> novos = ArgumentCaptor.forClass(Map.class);
        verify(auditoriaService).registrar(eq("ALTERACAO DE CLIENTE"), eq("CLIENTE"), eq("cliente-1"),
                anteriores.capture(), novos.capture(), isNull());
        assertEquals("1111-1111", anteriores.getValue().get("telefone"));
        assertEquals("2222-2222", novos.getValue().get("telefone"));
        assertEquals("Rua antiga", anteriores.getValue().get("logradouro"));
        assertEquals("Rua nova", novos.getValue().get("logradouro"));
    }

    @Test
    void exclusaoAuditaCamposRemovidosAntesDeExcluir() {
        autenticar("admin", "ADMIN");
        Cliente cliente = new Cliente();
        cliente.setId("cliente-1");
        cliente.setTelefone("1111-1111");
        cliente.setLogradouro("Rua antiga");
        when(clienteRepository.findById("cliente-1")).thenReturn(Optional.of(cliente));

        clienteService.excluir("cliente-1");

        ArgumentCaptor<Map> anteriores = ArgumentCaptor.forClass(Map.class);
        verify(auditoriaService).registrar(eq("EXCLUSAO DE CLIENTE"), eq("CLIENTE"), eq("cliente-1"),
                anteriores.capture(), anyMap(), isNull());
        assertEquals("1111-1111", anteriores.getValue().get("telefone"));
        assertEquals("Rua antiga", anteriores.getValue().get("logradouro"));
        verify(clienteRepository).deleteById("cliente-1");
    }

    private void autenticar(String usuario, String perfil) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                usuario, "n/a", List.of(new SimpleGrantedAuthority("ROLE_" + perfil))));
    }
}