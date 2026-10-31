package com.umc.pediupatrao.dashboard;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.stereotype.Repository;

/** Agrega no banco: nenhum findAll de pedidos ou de clientes é necessário ao painel. */
@Repository
public class DashboardRepository {
    private final MongoTemplate mongo;
    public DashboardRepository(MongoTemplate mongo) { this.mongo = mongo; }

    public Document analisar(DashboardFiltro filtro) {
        List<Document> pipeline = base(filtro);
        Document finalizados = new Document("$match", new Document("situacao", "FINALIZADO"));
        Document resumo = new Document("_id", null)
                .append("pedidos", sum(1))
                .append("finalizados", sum(cond(eq("$situacao", "FINALIZADO"), 1, 0)))
                .append("cancelados", sum(cond(eq("$situacao", "CANCELADO"), 1, 0)))
                .append("faturamento", sum(cond(eq("$situacao", "FINALIZADO"), "$valor", 0)))
                .append("descontos", sum(cond(eq("$situacao", "FINALIZADO"), decimal("$valorDesconto"), 0)));
        Object data = new Document("$dateTrunc", new Document("date", "$dataHoraEntrada")
                .append("unit", switch (filtro.agrupamento()) { case "semana" -> "week"; case "mes" -> "month"; default -> "day"; })
                .append("timezone", DashboardFiltro.FUSO.getId()).append("startOfWeek", "monday"));
        Document serie = new Document("_id", data).append("pedidos", sum(1))
                .append("faturamento", sum(cond(eq("$situacao", "FINALIZADO"), "$valor", 0)));
        Document produto = new Document("_id", "$itens.produtoId")
                .append("nome", new Document("$first", "$itens.nomeProduto"))
                .append("quantidade", sum("$itens.quantidade"))
                .append("receitaBruta", sum(decimal("$itens.total")));
        Document facets = new Document("resumo", List.of(new Document("$group", resumo)))
                .append("situacoes", List.of(new Document("$group", new Document("_id", "$situacao").append("quantidade", sum(1)))))
                .append("vendas", List.of(new Document("$group", serie), new Document("$sort", new Document("_id", 1))))
                .append("produtos", List.of(finalizados, new Document("$sort", new Document("dataHoraEntrada", -1).append("_id", -1)),
                        new Document("$unwind", "$itens"),
                        new Document("$match", new Document("itens.produtoId", new Document("$ne", null))
                                .append("itens.quantidade", new Document("$gt", 0))),
                        new Document("$group", produto),
                        new Document("$sort", new Document("quantidade", -1).append("_id", 1)), new Document("$limit", 5)));
        pipeline.add(new Document("$facet", facets));
        return executar(pipeline);
    }

    public Document pedidos(DashboardFiltro filtro, int pagina, int tamanho) {
        List<Document> pipeline = base(filtro);
        pipeline.add(new Document("$sort", new Document("dataHoraEntrada", -1).append("_id", -1)));
        pipeline.add(new Document("$facet", new Document("contagem", List.of(new Document("$count", "total")))
                .append("itens", List.of(new Document("$skip", (long) pagina * tamanho), new Document("$limit", tamanho),
                        new Document("$project", new Document("_id", 0).append("id", new Document("$toString", "$_id"))
                                .append("status", "$situacao").append("dataHoraEntrada", 1).append("valorTotal", "$valor")
                                .append("quantidadeItens", new Document("$sum", "$itens.quantidade")))))));
        return executar(pipeline);
    }

    private List<Document> base(DashboardFiltro filtro) {
        Document intervalo = new Document("$gte", Date.from(filtro.inicio().atStartOfDay(DashboardFiltro.FUSO).toInstant()))
                .append("$lt", Date.from(filtro.fim().plusDays(1).atStartOfDay(DashboardFiltro.FUSO).toInstant()));
        List<Document> pipeline = new ArrayList<>();
        // Primeiro estágio usa o índice de data, antes do facet e da normalização de registros legados.
        pipeline.add(new Document("$match", new Document("dataHoraEntrada", intervalo)));
        Object situacao = new Document("$switch", new Document("branches", List.of(
                new Document("case", eq("$status", "EM_PREPARO")).append("then", "EM_PREPARACAO"),
                new Document("case", eq("$status", "ENTREGUE")).append("then", "FINALIZADO")))
                .append("default", new Document("$ifNull", List.of("$status", "DESCONHECIDO"))));
        pipeline.add(new Document("$addFields", new Document("situacao", situacao)
                .append("valor", new Document("$max", List.of(0, decimal("$valorTotal"))))));
        if (filtro.status() != null) pipeline.add(new Document("$match", new Document("situacao", filtro.status())));
        return pipeline;
    }

    private Document executar(List<Document> pipeline) {
        List<AggregationOperation> operacoes = pipeline.stream()
                .map(stage -> (AggregationOperation) context -> stage).toList();
        Document result = mongo.aggregate(Aggregation.newAggregation(operacoes), "pedidos", Document.class).getUniqueMappedResult();
        return result == null ? new Document() : result;
    }
    private static Document sum(Object value) { return new Document("$sum", value); }
    private static Document eq(Object a, Object b) { return new Document("$eq", List.of(a, b)); }
    private static Document cond(Object condition, Object yes, Object no) { return new Document("$cond", List.of(condition, yes, no)); }
    private static Document decimal(String campo) {
        return new Document("$convert", new Document("input", campo).append("to", "decimal").append("onError", 0).append("onNull", 0));
    }
}
