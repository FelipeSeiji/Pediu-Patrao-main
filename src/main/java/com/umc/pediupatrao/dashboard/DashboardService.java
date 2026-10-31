package com.umc.pediupatrao.dashboard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
public class DashboardService {
    private final DashboardRepository repository;
    public DashboardService(DashboardRepository repository) { this.repository = repository; }

    public record Indicadores(long pedidos, long finalizados, long emAndamento, long cancelados,
            BigDecimal faturamento, BigDecimal ticketMedio, BigDecimal descontos, BigDecimal taxaCancelamento) {}
    public record Comparacao(LocalDate inicio, LocalDate fim, BigDecimal faturamento, BigDecimal variacaoFaturamento) {}
    public record Venda(LocalDate data, long pedidos, BigDecimal faturamento) {}
    public record Situacao(String status, long quantidade) {}
    public record ProdutoVendido(String produtoId, String nome, long quantidade, BigDecimal receitaBruta) {}
    public record Resumo(DashboardFiltro filtros, String fusoHorario, Instant atualizadoEm, Indicadores indicadores,
            Comparacao comparacao, List<Venda> vendas, List<Situacao> situacoes, List<ProdutoVendido> produtos) {}
    public record PedidoResumo(String id, String status, Instant dataHoraEntrada, BigDecimal valorTotal, long quantidadeItens) {}
    public record Pagina(List<PedidoResumo> itens, long total, int pagina, int tamanho, long totalPaginas) {}

    public Resumo consultar(DashboardFiltro filtro) {
        Document dados = repository.analisar(filtro);
        Indicadores indicadores = indicadores(dados);
        DashboardFiltro anterior = filtro.anterior();
        BigDecimal receitaAnterior = indicadores(repository.analisar(anterior)).faturamento();
        BigDecimal variacao = receitaAnterior.signum() == 0 ? null : indicadores.faturamento().subtract(receitaAnterior)
                .multiply(BigDecimal.valueOf(100)).divide(receitaAnterior, 2, RoundingMode.HALF_UP);
        List<ProdutoVendido> produtos = documentos(dados, "produtos").stream().map(p -> new ProdutoVendido(
                String.valueOf(p.get("_id")), p.getString("nome") == null ? "Produto sem nome" : p.getString("nome"),
                numero(p, "quantidade"), dinheiro(p.get("receitaBruta")))).toList();
        Map<String, Long> contagem = new LinkedHashMap<>();
        DashboardFiltro.STATUS.forEach(s -> contagem.put(s, 0L));
        documentos(dados, "situacoes").forEach(s -> contagem.merge(s.getString("_id"), numero(s, "quantidade"), Long::sum));
        List<Situacao> situacoes = contagem.entrySet().stream().map(e -> new Situacao(e.getKey(), e.getValue())).toList();
        return new Resumo(filtro, DashboardFiltro.FUSO.getId(), Instant.now(), indicadores,
                new Comparacao(anterior.inicio(), anterior.fim(), receitaAnterior, variacao),
                preencherSerie(filtro, documentos(dados, "vendas")), situacoes, produtos);
    }

    public Pagina pedidos(DashboardFiltro filtro, int pagina, int tamanho) {
        if (pagina < 0 || pagina > 100000 || tamanho < 1 || tamanho > 50) {
            throw new IllegalArgumentException("Página deve estar entre 0 e 100000; tamanho entre 1 e 50.");
        }
        Document dados = repository.pedidos(filtro, pagina, tamanho);
        long total = documentos(dados, "contagem").stream().findFirst().map(d -> numero(d, "total")).orElse(0L);
        List<PedidoResumo> itens = documentos(dados, "itens").stream().map(p -> new PedidoResumo(p.getString("id"),
                p.getString("status"), p.getDate("dataHoraEntrada").toInstant(), dinheiro(p.get("valorTotal")),
                numero(p, "quantidadeItens"))).toList();
        return new Pagina(itens, total, pagina, tamanho, (total + tamanho - 1) / tamanho);
    }

    private Indicadores indicadores(Document dados) {
        Document d = documentos(dados, "resumo").stream().findFirst().orElse(new Document());
        long pedidos = numero(d, "pedidos"), finalizados = numero(d, "finalizados"), cancelados = numero(d, "cancelados");
        BigDecimal receita = dinheiro(d.get("faturamento"));
        return new Indicadores(pedidos, finalizados, pedidos - finalizados - cancelados, cancelados, receita,
                finalizados == 0 ? dinheiro(0) : receita.divide(BigDecimal.valueOf(finalizados), 2, RoundingMode.HALF_UP),
                dinheiro(d.get("descontos")), pedidos == 0 ? dinheiro(0) : BigDecimal.valueOf(cancelados * 100)
                        .divide(BigDecimal.valueOf(pedidos), 2, RoundingMode.HALF_UP));
    }

    private List<Venda> preencherSerie(DashboardFiltro filtro, List<Document> documentos) {
        Map<LocalDate, Venda> encontrados = new LinkedHashMap<>();
        for (Document d : documentos) {
            LocalDate data = ((Date) d.get("_id")).toInstant().atZone(DashboardFiltro.FUSO).toLocalDate();
            encontrados.put(data, new Venda(data, numero(d, "pedidos"), dinheiro(d.get("faturamento"))));
        }
        LocalDate data = switch (filtro.agrupamento()) {
            case "semana" -> filtro.inicio().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case "mes" -> filtro.inicio().withDayOfMonth(1);
            default -> filtro.inicio();
        };
        List<Venda> serie = new ArrayList<>();
        while (!data.isAfter(filtro.fim())) {
            serie.add(encontrados.getOrDefault(data, new Venda(data, 0, dinheiro(0))));
            data = switch (filtro.agrupamento()) { case "semana" -> data.plusWeeks(1); case "mes" -> data.plusMonths(1); default -> data.plusDays(1); };
        }
        return serie;
    }
    private static List<Document> documentos(Document d, String key) { return d.getList(key, Document.class, List.of()); }
    private static long numero(Document d, String key) { return d.get(key) instanceof Number n ? n.longValue() : 0; }
    private static BigDecimal dinheiro(Object value) {
        BigDecimal decimal = value instanceof Decimal128 d ? d.bigDecimalValue() : value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
        return decimal.setScale(2, RoundingMode.HALF_UP);
    }
}
