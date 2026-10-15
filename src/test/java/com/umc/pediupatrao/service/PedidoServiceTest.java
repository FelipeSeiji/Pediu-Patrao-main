package com.umc.pediupatrao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import com.umc.pediupatrao.repository.ClienteRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {
    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private ProdutoRepository produtoRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private AuditoriaService auditoriaService;

    private PedidoService pedidoService;

    @BeforeEach
    void setUp() {
        pedidoService = new PedidoService(pedidoRepository, produtoRepository, clienteRepository, auditoriaService);
    }

    @AfterEach
    void limparAutenticacao() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void criarPedidoCalculaPrecoNoServidorERegistraEntrada() {
        autenticar("atendente", "ATENDENTE");
        Produto produto = new Produto();
        produto.setId("produto-1");
        produto.setNome("Pizza");
        produto.setPreco(12.5);
        produto.setAtivo(true);
        Produto bebida = new Produto();
        bebida.setId("produto-2");
        bebida.setNome("Bebida");
        bebida.setPreco(5.0);
        bebida.setAtivo(true);
        when(clienteRepository.existsById("cliente-1")).thenReturn(true);
        when(produtoRepository.findById("produto-1")).thenReturn(Optional.of(produto));
        when(produtoRepository.findById("produto-2")).thenReturn(Optional.of(bebida));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pedido solicitado = new Pedido();
        solicitado.setClienteId("cliente-1");
        solicitado.setStatus("CANCELADO");
        solicitado.setValorTotal(new BigDecimal("0.01"));
        solicitado.setSubtotal(new BigDecimal("0.01"));
        solicitado.setDescontoPercentual(new BigDecimal("20"));
        Pedido.Item item = new Pedido.Item();
        item.setProdutoId("produto-1");
        item.setQuantidade(2);
        item.setPrecoUnitario(new BigDecimal("0.01"));
        Pedido.Item segundoItem = new Pedido.Item();
        segundoItem.setProdutoId("produto-2");
        segundoItem.setQuantidade(1);
        solicitado.setItens(List.of(item, segundoItem));

        Pedido salvo = pedidoService.criarPedido(solicitado);

        assertEquals("RECEBIDO", salvo.getStatus());
        assertEquals(2, salvo.getItens().size());
        assertEquals(new BigDecimal("12.50"), salvo.getItens().get(0).getPrecoUnitario());
        assertEquals(new BigDecimal("30.00"), salvo.getValorTotal());
        assertEquals("atendente", salvo.getResponsavelRegistro());
        assertNotNull(salvo.getDataHoraEntrada());
        verify(auditoriaService).registrar(eq("INCLUSAO DE PEDIDO"), eq("PEDIDO"), any(), anyMap(), anyMap(), eq(null));
    }

    @Test
    void bloqueiaTransicaoInvalida() {
        autenticar("atendente", "ATENDENTE");
        Pedido pedido = new Pedido();
        pedido.setId("pedido-1");
        pedido.setStatus("RECEBIDO");
        when(pedidoRepository.findById("pedido-1")).thenReturn(Optional.of(pedido));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> pedidoService.atualizarStatus("pedido-1", "FINALIZADO"));

        assertEquals(409, erro.getStatusCode().value());
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    @Test
    void limitaDescontoAteVintePorCento() {
        autenticar("gerente", "GERENTE");
        Pedido pedido = new Pedido();
        pedido.setId("pedido-1");
        pedido.setStatus("PRONTO");
        pedido.setSubtotal(new BigDecimal("50.00"));
        pedido.setValorTotal(new BigDecimal("50.00"));
        when(pedidoRepository.findById("pedido-1")).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(ResponseStatusException.class,
                () -> pedidoService.aplicarDesconto("pedido-1", new BigDecimal("20.01"), null));
        Pedido atualizado = pedidoService.aplicarDesconto("pedido-1", new BigDecimal("20"), null);

        assertEquals(new BigDecimal("10.00"), atualizado.getValorDesconto());
        assertEquals(new BigDecimal("40.00"), atualizado.getValorTotal());
    }

    @Test
    void cancelamentoExigeJustificativaEDeveOcorrerAntesDaSaida() {
        autenticar("gerente", "GERENTE");
        Pedido recebido = new Pedido();
        recebido.setId("pedido-1");
        recebido.setStatus("RECEBIDO");
        when(pedidoRepository.findById("pedido-1")).thenReturn(Optional.of(recebido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(ResponseStatusException.class, () -> pedidoService.cancelarPedido("pedido-1", " "));
        Pedido cancelado = pedidoService.cancelarPedido("pedido-1", "Solicitação do cliente");

        assertEquals("CANCELADO", cancelado.getStatus());
        assertEquals("Solicitação do cliente", cancelado.getJustificativaCancelamento());
        verify(pedidoRepository).save(recebido);

        Pedido saiu = new Pedido();
        saiu.setId("pedido-2");
        saiu.setStatus("SAIU_PARA_ENTREGA");
        when(pedidoRepository.findById("pedido-2")).thenReturn(Optional.of(saiu));
        assertThrows(ResponseStatusException.class,
                () -> pedidoService.cancelarPedido("pedido-2", "Solicitação do cliente"));
        verify(pedidoRepository, never()).save(saiu);
    }

    private void autenticar(String usuario, String perfil) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                usuario, "n/a", List.of(new SimpleGrantedAuthority("ROLE_" + perfil))));
    }
}