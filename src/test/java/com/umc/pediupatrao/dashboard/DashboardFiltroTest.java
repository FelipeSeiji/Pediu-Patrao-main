package com.umc.pediupatrao.dashboard;

import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DashboardFiltroTest {
    @Test void padraoEComparacaoTemMesmaDuracao() {
        var f = new DashboardFiltro(null, LocalDate.of(2026, 10, 31), null, null);
        assertEquals(LocalDate.of(2026, 10, 2), f.inicio()); assertEquals(30, f.dias());
        assertEquals(30, f.anterior().dias()); assertEquals(f.inicio().minusDays(1), f.anterior().fim());
    }
    @Test void aceitaUmDiaEAnoBissexto() {
        assertEquals(1, new DashboardFiltro(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1), " finalizado ", "DIA").dias());
        assertEquals(366, new DashboardFiltro(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), null, "mes").dias());
    }
    @Test void rejeitaPeriodoInvertidoOuExcessivo() {
        assertThrows(IllegalArgumentException.class, () -> new DashboardFiltro(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1), null, "dia"));
        assertThrows(IllegalArgumentException.class, () -> new DashboardFiltro(LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 1), null, "dia"));
    }
    @ParameterizedTest @ValueSource(strings={"ENTREGUE", "EM_PREPARO", "INVALIDO", "$ne"})
    void rejeitaStatusForaDoContrato(String status) {
        assertThrows(IllegalArgumentException.class, () -> new DashboardFiltro(null, null, status, "dia"));
    }
    @Test void rejeitaAgrupamentoInvalido() { assertThrows(IllegalArgumentException.class, () -> new DashboardFiltro(null, null, null, "hora")); }
}
