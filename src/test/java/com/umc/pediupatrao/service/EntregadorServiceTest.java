package com.umc.pediupatrao.service;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.entity.Entregador;
import com.umc.pediupatrao.repository.EntregadorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EntregadorServiceTest {
    @Mock
    private EntregadorRepository repository;

    @InjectMocks
    private EntregadorService service;

    @Test
    void criaEntregadorQuandoFormularioEnviaIdVazio() {
        Entregador entregador = new Entregador();
        entregador.setId("");
        entregador.setNome("João");
        entregador.setTelefone("12345678");
        when(repository.save(any(Entregador.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Entregador salvo = service.salvar(entregador);

        assertNull(salvo.getId());
        verify(repository, never()).existsById("");
        verify(repository).save(entregador);
    }
}
