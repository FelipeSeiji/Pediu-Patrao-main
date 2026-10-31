package com.umc.pediupatrao.dashboard;

import java.time.LocalDate;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService service;
    public DashboardController(DashboardService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<DashboardService.Resumo> resumo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) String status, @RequestParam(defaultValue = "dia") String agrupamento) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.consultar(new DashboardFiltro(inicio, fim, status, agrupamento)));
    }

    @GetMapping("/pedidos")
    public ResponseEntity<DashboardService.Pagina> pedidos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) String status, @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "8") int tamanho) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.pedidos(new DashboardFiltro(inicio, fim, status, "dia"), pagina, tamanho));
    }

    @GetMapping("/filtros")
    public Map<String, Object> filtros() {
        return Map.of("status", DashboardFiltro.STATUS, "agrupamentos", java.util.List.of("dia", "semana", "mes"),
                "fusoHorario", DashboardFiltro.FUSO.getId(), "limiteDias", 366, "hoje", LocalDate.now(DashboardFiltro.FUSO));
    }
}
