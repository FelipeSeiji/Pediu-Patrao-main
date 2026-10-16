package com.umc.pediupatrao.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.entity.HorarioFuncionamento;
import com.umc.pediupatrao.service.EntregadorService;
import com.umc.pediupatrao.service.HorarioFuncionamentoService;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({EntregadorController.class, HorarioFuncionamentoController.class})
@Import(SecurityConfig.class)
class ConfiguracoesPageIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private EntregadorService entregadorService;
    @MockBean
    private HorarioFuncionamentoService horarioFuncionamentoService;
    @MockBean
    private UsuarioDetailsService usuarioDetailsService;

    @Test
    void entregadoresRenderizaParaGerente() throws Exception {
        when(entregadorService.listar()).thenReturn(List.of());

        mockMvc.perform(get("/entregadores").with(user("gerente").roles("GERENTE")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nenhum entregador cadastrado.")));
    }

    @Test
    void formularioDeEntregadorRenderizaParaGerente() throws Exception {
        mockMvc.perform(get("/entregadores/novo").with(user("gerente").roles("GERENTE")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Novo entregador")));
    }

    @Test
    void horariosRenderizaParaGerente() throws Exception {
        HorarioFuncionamento horarios = new HorarioFuncionamento();
        HorarioFuncionamento.Dia segunda = new HorarioFuncionamento.Dia();
        segunda.setDia("Segunda-feira");
        segunda.setFechado(true);
        horarios.setDias(List.of(segunda));
        when(horarioFuncionamentoService.obter()).thenReturn(horarios);

        mockMvc.perform(get("/horarios").with(user("gerente").roles("GERENTE")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Horário de Funcionamento")));
    }

    @Test
    void atendenteNaoPodeAcessarConfiguracoes() throws Exception {
        mockMvc.perform(get("/entregadores").with(user("atendente").roles("ATENDENTE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/horarios").with(user("atendente").roles("ATENDENTE")))
                .andExpect(status().isForbidden());
    }
}