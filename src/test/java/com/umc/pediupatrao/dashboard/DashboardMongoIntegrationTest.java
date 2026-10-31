package com.umc.pediupatrao.dashboard;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Usa MongoDB real e banco exclusivo. Habilite MONGODB_TEST_URI para executar. */
@DataMongoTest(properties="spring.data.mongodb.database=pediupatrao_dashboard_test")
@Import({DashboardRepository.class, DashboardService.class})
@EnabledIfEnvironmentVariable(named="MONGODB_TEST_URI", matches=".+")
class DashboardMongoIntegrationTest {
    @Autowired MongoTemplate mongo;
    @Autowired DashboardService service;
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> System.getenv("MONGODB_TEST_URI"));
    }
    @BeforeEach void limpar() { mongo.getCollection("pedidos").deleteMany(new Document()); }
    private DashboardFiltro filtro(String inicio, String fim, String status, String agrupamento) {
        return new DashboardFiltro(LocalDate.parse(inicio), LocalDate.parse(fim), status, agrupamento);
    }
    private void pedido(String id, String data, String status, String total, int quantidade) {
        Document p = new Document("_id", id).append("dataHoraEntrada", Date.from(Instant.parse(data))).append("status", status)
                .append("valorTotal", total).append("valorDesconto", "10.00").append("clienteId", "dado-privado")
                .append("itens", List.of(new Document("produtoId", "p1").append("nomeProduto", "Pizza")
                        .append("quantidade", quantidade).append("total", "100.00")));
        mongo.getCollection("pedidos").insertOne(p);
    }
    @Test void calculaReceitaLiquidaTicketCancelamentosERankingSemDuplicarPedidos() {
        pedido("a", "2026-10-20T22:00:00Z", "FINALIZADO", "90.10", 2);
        pedido("b", "2026-10-21T22:00:00Z", "ENTREGUE", "40.20", 1);
        pedido("c", "2026-10-22T22:00:00Z", "CANCELADO", "500.00", 20);
        pedido("d", "2026-10-23T22:00:00Z", "EM_PREPARO", "700.00", 40);
        var d=service.consultar(filtro("2026-10-01","2026-10-31",null,"dia"));
        assertEquals(4,d.indicadores().pedidos());assertEquals(2,d.indicadores().finalizados());assertEquals(1,d.indicadores().emAndamento());
        assertEquals(new BigDecimal("130.30"),d.indicadores().faturamento());assertEquals(new BigDecimal("65.15"),d.indicadores().ticketMedio());
        assertEquals(new BigDecimal("25.00"),d.indicadores().taxaCancelamento());assertEquals(new BigDecimal("20.00"),d.indicadores().descontos());
        assertEquals(3,d.produtos().getFirst().quantidade());assertEquals(new BigDecimal("200.00"),d.produtos().getFirst().receitaBruta());
        assertEquals(31,d.vendas().size());assertNull(d.comparacao().variacaoFaturamento());
    }
    @Test void respeitaMeiaNoiteLocalEFimInclusivo() {
        pedido("foraAntes","2026-10-01T02:59:59Z","FINALIZADO","99",1);
        pedido("inicio","2026-10-01T03:00:00Z","FINALIZADO","10",1);
        pedido("fim","2026-11-01T02:59:59Z","FINALIZADO","20",1);
        pedido("foraDepois","2026-11-01T03:00:00Z","FINALIZADO","99",1);
        var d=service.consultar(filtro("2026-10-01","2026-10-31",null,"dia"));
        assertEquals(2,d.indicadores().pedidos());assertEquals(new BigDecimal("30.00"),d.indicadores().faturamento());
        assertEquals(1,d.vendas().getFirst().pedidos());assertEquals(1,d.vendas().getLast().pedidos());
    }
    @Test void aplicaStatusNormalizadoEmTodosOsBlocos() {
        pedido("a","2026-10-20T22:00:00Z","ENTREGUE","50",2);pedido("b","2026-10-20T22:00:00Z","CANCELADO","99",1);
        var f=filtro("2026-10-20","2026-10-20","FINALIZADO","dia");var d=service.consultar(f);
        assertEquals(1,d.indicadores().pedidos());assertEquals(1,d.vendas().getFirst().pedidos());assertEquals(2,d.produtos().getFirst().quantidade());
        assertEquals(1,service.pedidos(f,0,8).total());
    }
    @Test void comparaPeriodoAnteriorDoMesmoTamanho() {
        pedido("a","2026-10-10T22:00:00Z","FINALIZADO","120",1);pedido("b","2026-10-09T22:00:00Z","FINALIZADO","80",1);
        var d=service.consultar(filtro("2026-10-10","2026-10-10",null,"dia"));
        assertEquals(new BigDecimal("50.00"),d.comparacao().variacaoFaturamento());assertEquals(LocalDate.of(2026,10,9),d.comparacao().inicio());
    }
    @Test void preencheSemanasEMesesSemMovimento() {
        var semana=service.consultar(filtro("2026-10-07","2026-10-20",null,"semana"));
        assertEquals(LocalDate.of(2026,10,5),semana.vendas().getFirst().data());assertEquals(3,semana.vendas().size());
        var mes=service.consultar(filtro("2026-09-15","2026-11-02",null,"mes"));assertEquals(3,mes.vendas().size());
        assertEquals(0,mes.indicadores().pedidos());assertEquals(new BigDecimal("0.00"),mes.indicadores().ticketMedio());assertTrue(mes.produtos().isEmpty());
    }
    @Test void paginaComOrdenacaoEstavelEProjecaoReduzida() {
        for(int i=0;i<12;i++)pedido("pedido-"+String.format("%02d",i),"2026-10-20T22:00:00Z","RECEBIDO","20",1);
        var f=filtro("2026-10-20","2026-10-20",null,"dia");
        var p1=service.pedidos(f,0,8);var p2=service.pedidos(f,1,8);
        assertEquals(12,p1.total());assertEquals(2,p1.totalPaginas());assertEquals(8,p1.itens().size());assertEquals(4,p2.itens().size());
        assertEquals("pedido-11",p1.itens().getFirst().id());assertEquals("pedido-03",p2.itens().getFirst().id());
        assertTrue(service.pedidos(f,2,8).itens().isEmpty());
    }
    @Test void rejeitaPaginacaoForaDosLimites() {
        var f=filtro("2026-10-20","2026-10-20",null,"dia");
        assertThrows(IllegalArgumentException.class,()->service.pedidos(f,-1,8));assertThrows(IllegalArgumentException.class,()->service.pedidos(f,0,51));
    }
    @Test void legadoSemValorOuItensNaoQuebraConsulta() {
        mongo.getCollection("pedidos").insertOne(new Document("dataHoraEntrada",Date.from(Instant.parse("2026-10-20T22:00:00Z"))));
        var d=service.consultar(filtro("2026-10-20","2026-10-20",null,"dia"));assertEquals(1,d.indicadores().pedidos());
        assertEquals(new BigDecimal("0.00"),d.indicadores().faturamento());assertTrue(d.produtos().isEmpty());
        assertEquals("DESCONHECIDO",service.pedidos(filtro("2026-10-20","2026-10-20",null,"dia"),0,8).itens().getFirst().status());
    }
    @Test void limitaRankingATopCincoSemMultiplicarContagemDePedidos() {
        var itens=new java.util.ArrayList<Document>();
        for(int i=0;i<7;i++)itens.add(new Document("produtoId","p"+i).append("nomeProduto","Produto "+i).append("quantidade",i+1).append("total","10.00"));
        mongo.getCollection("pedidos").insertOne(new Document("_id","multiplos-itens").append("status","FINALIZADO")
                .append("dataHoraEntrada",Date.from(Instant.parse("2026-10-20T22:00:00Z"))).append("valorTotal","70.00").append("itens",itens));
        var d=service.consultar(filtro("2026-10-20","2026-10-20",null,"dia"));
        assertEquals(1,d.indicadores().pedidos());assertEquals(new BigDecimal("70.00"),d.indicadores().faturamento());
        assertEquals(5,d.produtos().size());assertEquals("p6",d.produtos().getFirst().produtoId());assertEquals(7,d.produtos().getFirst().quantidade());
    }
}
