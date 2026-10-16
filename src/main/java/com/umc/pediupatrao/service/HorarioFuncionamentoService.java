package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.HorarioFuncionamento;
import com.umc.pediupatrao.repository.HorarioFuncionamentoRepository;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
public class HorarioFuncionamentoService {
    private static final String ID_SEMANAL = "semanal";
    private static final List<String> DIAS = List.of(
            "Segunda-feira", "Terça-feira", "Quarta-feira", "Quinta-feira",
            "Sexta-feira", "Sábado", "Domingo");

    private final HorarioFuncionamentoRepository repository;

    public HorarioFuncionamentoService(HorarioFuncionamentoRepository repository) {
        this.repository = repository;
    }

    public HorarioFuncionamento obter() {
        return repository.findById(ID_SEMANAL).orElseGet(this::horariosFechados);
    }

    public HorarioFuncionamento salvar(HorarioFuncionamento horarios) {
        if (horarios.getDias() == null || horarios.getDias().size() != DIAS.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe os horários dos sete dias da semana.");
        }
        Map<String, HorarioFuncionamento.Dia> diasPorNome = new LinkedHashMap<>();
        for (HorarioFuncionamento.Dia dia : horarios.getDias()) {
            if (dia.getDia() == null || !DIAS.contains(dia.getDia())
                    || diasPorNome.putIfAbsent(dia.getDia(), dia) != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dia da semana inválido ou repetido.");
            }
            boolean fechado = Boolean.TRUE.equals(dia.getFechado());
            dia.setFechado(fechado);
            if (!fechado && !horarioValido(dia.getAbertura(), dia.getFechamento())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Para dias abertos, informe horários válidos e o fechamento após a abertura.");
            }
            if (fechado) {
                dia.setAbertura(null);
                dia.setFechamento(null);
            }
        }
        if (!diasPorNome.keySet().containsAll(DIAS)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe cada dia da semana uma única vez.");
        }
        horarios.setId(ID_SEMANAL);
        horarios.setDias(DIAS.stream().map(diasPorNome::get).toList());
        return repository.save(horarios);
    }

    private boolean horarioValido(LocalTime abertura, LocalTime fechamento) {
        return abertura != null && fechamento != null && fechamento.isAfter(abertura);
    }

    private HorarioFuncionamento horariosFechados() {
        HorarioFuncionamento horarios = new HorarioFuncionamento();
        List<HorarioFuncionamento.Dia> dias = new ArrayList<>();
        for (String nome : DIAS) {
            HorarioFuncionamento.Dia dia = new HorarioFuncionamento.Dia();
            dia.setDia(nome);
            dia.setFechado(true);
            dias.add(dia);
        }
        horarios.setDias(dias);
        return horarios;
    }
}