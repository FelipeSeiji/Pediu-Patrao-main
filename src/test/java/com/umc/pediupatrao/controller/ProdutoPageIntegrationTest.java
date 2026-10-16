package com.umc.pediupatrao.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.umc.pediupatrao.service.AuditoriaService;
import com.umc.pediupatrao.service.ClienteService;
import com.umc.pediupatrao.service.PedidoService;
import com.umc.pediupatrao.service.ProdutoService;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import com.umc.pediupatrao.service.UsuarioService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import com.umc.pediupatrao.config.SecurityConfig;

@WebMvcTest(HomeController.class)
@Import(SecurityConfig.class)
class ProdutoPageIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PedidoService pedidoService;
    @MockBean
    private ClienteService clienteService;
    @MockBean
    private ProdutoService produtoService;
    @MockBean
    private UsuarioService usuarioService;
    @MockBean
    private AuditoriaService auditoriaService;
    @MockBean
    private UsuarioDetailsService usuarioDetailsService;

    @Test
    void produtosRenderizaFragmentoDentroDoLayout() throws Exception {
        when(produtoService.listarProdutos()).thenReturn(List.of());

        mockMvc.perform(get("/produtos").with(user("atendente").roles("ATENDENTE")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nenhum produto cadastrado.")));
    }
}