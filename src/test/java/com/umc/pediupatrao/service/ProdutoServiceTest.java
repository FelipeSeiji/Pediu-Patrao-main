package com.umc.pediupatrao.service;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.repository.ProdutoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {
    @Mock
    private ProdutoRepository repository;

    @InjectMocks
    private ProdutoService service;

    @Test
    void criaProdutoQuandoFormularioEnviaIdVazio() {
        Produto produto = new Produto();
        produto.setId("");
        produto.setNome("Pizza teste");
        produto.setTipo("Pizza");
        produto.setPreco(25.0);
        produto.setAtivo(true);
        when(repository.save(any(Produto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Produto salvo = service.salvar(produto);

        assertNull(salvo.getId());
        verify(repository, never()).existsById("");
        verify(repository).save(produto);
    }
}
