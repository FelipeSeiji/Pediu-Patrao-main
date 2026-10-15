package com.umc.pediupatrao.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import com.umc.pediupatrao.service.AuditoriaService;
import com.umc.pediupatrao.service.PedidoService;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.assertThrows;

@WebMvcTest(PedidoController.class)
@Import({com.umc.pediupatrao.config.SecurityConfig.class, PedidoService.class})
class PedidoSecurityIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PedidoRepository pedidoRepository;
    @MockBean
    private ProdutoRepository produtoRepository;
    @MockBean
    private ClienteRepository clienteRepository;
    @MockBean
    private AuditoriaService auditoriaService;
    @MockBean
    private UsuarioDetailsService usuarioDetailsService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminNaoPodeAplicarDescontoNemCancelar() throws Exception {
        mockMvc.perform(put("/api/pedidos/pedido-1/desconto").param("percentual", "10").with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/pedidos/pedido-1/cancelar").param("justificativa", "Teste").with(csrf()))
                .andExpect(status().isForbidden());

        verify(pedidoRepository, never()).findById(any());
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    void atendenteNaoPodeCancelarPedido() throws Exception {
        mockMvc.perform(put("/api/pedidos/pedido-1/desconto").param("percentual", "10").with(csrf()))
            .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/pedidos/pedido-1/cancelar").param("justificativa", "Teste").with(csrf()))
                .andExpect(status().isForbidden());
        verify(pedidoRepository, never()).findById(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void gerentePodeAplicarDescontoPelaApi() throws Exception {
        Pedido pedido = new Pedido();
        pedido.setId("pedido-1");
        pedido.setStatus("PRONTO");
        pedido.setSubtotal(new BigDecimal("50.00"));
        pedido.setValorTotal(new BigDecimal("50.00"));
        when(pedidoRepository.findById("pedido-1")).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/pedidos/pedido-1/desconto").param("percentual", "10").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorTotal").value(45.0));
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void gerentePodeCancelarPedidoPelaApi() throws Exception {
        Pedido pedido = new Pedido();
        pedido.setId("pedido-1");
        pedido.setStatus("PRONTO");
        when(pedidoRepository.findById("pedido-1")).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/pedidos/pedido-1/cancelar")
                        .param("justificativa", "Solicitação do cliente").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void apiExigeTokenCsrfParaAlteracoes() throws Exception {
        mockMvc.perform(put("/api/pedidos/pedido-1/desconto").param("percentual", "10"))
                .andExpect(status().isForbidden());
    }
}