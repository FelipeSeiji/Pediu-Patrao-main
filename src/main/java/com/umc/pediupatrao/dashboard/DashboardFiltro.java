package com.umc.pediupatrao.dashboard;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

/** Datas inclusivas no fuso do estabelecimento; nunca no fuso do navegador. */
public record DashboardFiltro(LocalDate inicio, LocalDate fim, String status, String agrupamento) {
    public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    public static final List<String> STATUS = List.of("RECEBIDO", "EM_PREPARACAO", "PRONTO",
            "SAIU_PARA_ENTREGA", "RETIRADO", "FINALIZADO", "CANCELADO");

    public DashboardFiltro {
        if (fim == null) fim = LocalDate.now(FUSO);
        if (inicio == null) inicio = fim.minusDays(29);
        if (inicio.isAfter(fim) || ChronoUnit.DAYS.between(inicio, fim) >= 366) {
            throw new IllegalArgumentException("Informe um período de 1 a 366 dias, com início anterior ou igual ao fim.");
        }
        status = status == null || status.isBlank() ? null : status.trim().toUpperCase(Locale.ROOT);
        if (status != null && !STATUS.contains(status)) throw new IllegalArgumentException("Status inválido.");
        agrupamento = agrupamento == null ? "dia" : agrupamento.toLowerCase(Locale.ROOT);
        if (!List.of("dia", "semana", "mes").contains(agrupamento)) {
            throw new IllegalArgumentException("Agrupamento deve ser dia, semana ou mes.");
        }
    }

    public long dias() { return ChronoUnit.DAYS.between(inicio, fim) + 1; }
    public DashboardFiltro anterior() {
        return new DashboardFiltro(inicio.minusDays(dias()), inicio.minusDays(1), status, agrupamento);
    }
}
