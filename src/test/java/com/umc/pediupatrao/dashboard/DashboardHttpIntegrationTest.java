package com.umc.pediupatrao.dashboard;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.Instant;
import java.util.Date;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties="spring.data.mongodb.database=pediupatrao_dashboard_http_test")
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named="MONGODB_TEST_URI", matches=".+")
class DashboardHttpIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired MongoTemplate mongo;
    @Autowired DashboardService service;
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> System.getenv("MONGODB_TEST_URI"));
    }
    @BeforeEach void preparar() {
        mongo.getCollection("pedidos").deleteMany(new Document());
        mongo.getCollection("pedidos").insertOne(new Document("_id","http-teste")
                .append("status","FINALIZADO").append("valorTotal","13.45")
                .append("dataHoraEntrada", Date.from(Instant.parse("2026-10-31T22:00:00Z"))));
    }
    @Test void resumoExecutaSegurancaControllerServicoEBanco() throws Exception {
        mvc.perform(get("/api/dashboard?inicio=2026-10-31&fim=2026-10-31").with(user("g").roles("GERENTE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.indicadores.faturamento").value(13.45))
                .andExpect(jsonPath("$.indicadores.pedidos").value(1)).andExpect(jsonPath("$.vendas[0].pedidos").value(1))
                .andExpect(jsonPath("$.filtros.inicio").value("2026-10-31")).andExpect(jsonPath("$.comparacao.variacaoFaturamento").isEmpty());
    }
    @Test void paginaRetornaProjecaoSemCliente() throws Exception {
        mvc.perform(get("/api/dashboard/pedidos?inicio=2026-10-31&fim=2026-10-31").with(user("a").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.itens[0].id").value("http-teste"))
                .andExpect(jsonPath("$.itens[0].valorTotal").value(13.45)).andExpect(jsonPath("$.itens[0].clienteId").doesNotExist())
                .andExpect(jsonPath("$.total").value(1));
    }
    @Test @WithMockUser(roles="ATENDENTE") void servicoTambemProtegeAcessoDireto() {
        assertThrows(AccessDeniedException.class, () -> service.consultar(new DashboardFiltro(null,null,null,"dia")));
    }
}
