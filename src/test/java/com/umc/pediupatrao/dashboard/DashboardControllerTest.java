package com.umc.pediupatrao.dashboard;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({DashboardController.class, DashboardPageController.class})
@Import(SecurityConfig.class)
class DashboardControllerTest {
    @Autowired MockMvc mvc;
    @MockBean DashboardService service;
    @MockBean UsuarioDetailsService usuarios;

    @ParameterizedTest @ValueSource(strings={"/api/dashboard", "/api/dashboard/filtros", "/api/dashboard/pedidos"})
    void anonimoRecebeJson401(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isUnauthorized()).andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(401));verifyNoInteractions(service);
    }
    @ParameterizedTest @ValueSource(strings={"/api/dashboard", "/api/dashboard/filtros", "/api/dashboard/pedidos", "/dashboard"})
    void atendenteNaoAcessa(String path) throws Exception {
        mvc.perform(get(path).with(user("a").roles("ATENDENTE"))).andExpect(status().isForbidden());verifyNoInteractions(service);
    }
    @ParameterizedTest @ValueSource(strings={"ADMIN", "GERENTE"})
    void responsavelAcessaPaginaEFiltros(String role) throws Exception {
        mvc.perform(get("/dashboard").with(user("responsavel").roles(role))).andExpect(status().isOk())
                .andExpect(content().string(containsString("Evolução das vendas")))
                .andExpect(content().string(containsString("responsavel")));
        mvc.perform(get("/api/dashboard/filtros").with(user("r").roles(role))).andExpect(status().isOk())
                .andExpect(jsonPath("$.limiteDias").value(366)).andExpect(jsonPath("$.fusoHorario").value("America/Sao_Paulo"));
    }
    @ParameterizedTest @ValueSource(strings={"inicio=31-10-2026", "inicio=2026-11-01&fim=2026-10-01", "status=ERRADO", "agrupamento=hora"})
    void filtrosInvalidosRetornam400(String query) throws Exception {
        mvc.perform(get("/api/dashboard?"+query).with(user("r").roles("ADMIN"))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.detail").isString());
    }
    @Test void paginacaoNaoNumericaRetorna400() throws Exception {
        mvc.perform(get("/api/dashboard/pedidos?pagina=x").with(user("r").roles("GERENTE"))).andExpect(status().isBadRequest());
    }
    @Test void indisponibilidadeNaoExpoeDetalhesDoBanco() throws Exception {
        when(service.consultar(any())).thenThrow(new DataAccessResourceFailureException("mongodb://segredo:senha@host"));
        mvc.perform(get("/api/dashboard").with(user("r").roles("ADMIN"))).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("Dados temporariamente indisponíveis. Tente atualizar novamente."));
    }
    @Test void respostaNaoDeveSerArmazenadaEmCache() throws Exception {
        mvc.perform(get("/api/dashboard").with(user("r").roles("GERENTE"))).andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"));
    }
}
