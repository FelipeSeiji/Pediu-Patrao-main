package com.umc.pediupatrao.dashboard;

import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

/** Dados reproduzíveis apenas no perfil demo e em banco explicitamente separado. */
@Component
@Profile("demo")
public class DashboardDemoInitializer implements CommandLineRunner {
    private final MongoTemplate mongo;
    public DashboardDemoInitializer(MongoTemplate mongo) { this.mongo = mongo; }
    @Override
    public void run(String... args) {
        if (!mongo.getDb().getName().endsWith("_demo")) {
            throw new IllegalStateException("O perfil demo exige um banco com sufixo _demo.");
        }
        if (mongo.getCollection("pedidos").countDocuments() > 0) return;
        String[] nomes = {"Pizza de calabresa", "Pizza de mussarela", "X-bacon artesanal", "Pizza portuguesa", "Refrigerante 2L", "Batata frita"};
        String[] precos = {"49.90", "44.90", "28.50", "54.90", "12.00", "22.00"};
        for (int i = 0; i < nomes.length; i++) {
            Produto produto = new Produto(); produto.setId("demo-produto-" + i); produto.setNome(nomes[i]);
            produto.setPreco(Double.valueOf(precos[i])); produto.setTipo(i < 2 || i == 3 ? "Pizza" : "Lanche"); produto.setAtivo(true);
            if (!mongo.exists(org.springframework.data.mongodb.core.query.Query.query(
                    org.springframework.data.mongodb.core.query.Criteria.where("_id").is(produto.getId())), Produto.class)) mongo.save(produto);
        }
        Random random = new Random(42);
        LocalDate hoje = LocalDate.now(DashboardFiltro.FUSO);
        List<Pedido> pedidos = new ArrayList<>();
        for (int dia = 59; dia >= 0; dia--) {
            LocalDate data = hoje.minusDays(dia);
            int volume = 9 + (59 - dia) / 6 + random.nextInt(10) + (data.getDayOfWeek().getValue() >= 5 ? 9 : 0);
            for (int n = 0; n < volume; n++) {
                Pedido p = new Pedido();p.setId("demo-" + data + "-" + String.format("%03d", n));p.setClienteId("cliente-demonstracao");
                p.setDataHoraEntrada(data.atTime(11 + n % 12, (n * 7) % 60).atZone(DashboardFiltro.FUSO).toInstant());
                p.setStatus(n % 17 == 0 ? "CANCELADO" : dia == 0 && n < 10 ? DashboardFiltro.STATUS.get(n % 5) : "FINALIZADO");
                List<Pedido.Item> itens = new ArrayList<>();
                for (int j = 0; j < 1 + random.nextInt(3); j++) {
                    int idx = random.nextInt(nomes.length);Pedido.Item item = new Pedido.Item();
                    item.setProdutoId("demo-produto-" + idx);item.setNomeProduto(nomes[idx]);item.setQuantidade(1 + random.nextInt(2));
                    item.setPrecoUnitario(new BigDecimal(precos[idx]));item.setTotal(item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade())));itens.add(item);
                }
                p.setItens(itens);p.setSubtotal(itens.stream().map(Pedido.Item::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add));
                p.setDescontoPercentual(n % 7 == 0 ? BigDecimal.TEN : BigDecimal.ZERO);
                p.setValorDesconto(p.getSubtotal().multiply(p.getDescontoPercentual()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
                p.setValorTotal(p.getSubtotal().subtract(p.getValorDesconto()));p.setResponsavelRegistro("demonstracao");pedidos.add(p);
            }
        }
        mongo.insertAll(pedidos);
    }
}
