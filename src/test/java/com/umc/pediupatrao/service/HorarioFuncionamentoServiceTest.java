package com.umc.pediupatrao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.entity.HorarioFuncionamento;
import com.umc.pediupatrao.repository.HorarioFuncionamentoRepository;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class HorarioFuncionamentoServiceTest {
    private static final List<String> DIAS = List.of(
            "Segunda-feira", "Terça-feira", "Quarta-feira", "Quinta-feira",
            "Sexta-feira", "Sábado", "Domingo");

    @Mock
    private HorarioFuncionamentoRepository repository;
    private HorarioFuncionamentoService service;

    @BeforeEach
    void setUp() {
        service = new HorarioFuncionamentoService(repository);
    }

    @Test
    void salvaSemanaCompletaEApagaFaixasDeDiasFechados() {
        HorarioFuncionamento horarios = semanaValida();
        horarios.getDias().get(6).setFechado(true);
        horarios.getDias().get(6).setAbertura(LocalTime.of(10, 0));
        horarios.getDias().get(6).setFechamento(LocalTime.of(12, 0));
        when(repository.save(any(HorarioFuncionamento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HorarioFuncionamento salvo = service.salvar(horarios);

        assertEquals("semanal", salvo.getId());
        assertEquals(DIAS, salvo.getDias().stream().map(HorarioFuncionamento.Dia::getDia).toList());
        assertNull(salvo.getDias().get(6).getAbertura());
        assertNull(salvo.getDias().get(6).getFechamento());
    }

    @Test
    void rejeitaDiaAbertoSemIntervaloValido() {
        HorarioFuncionamento horarios = semanaValida();
        horarios.getDias().get(0).setFechamento(LocalTime.of(17, 0));

        assertThrows(ResponseStatusException.class, () -> service.salvar(horarios));
        verify(repository, never()).save(any(HorarioFuncionamento.class));
    }

    @Test
    void rejeitaSemanaIncompleta() {
        HorarioFuncionamento horarios = semanaValida();
        horarios.setDias(new ArrayList<>(horarios.getDias().subList(0, 6)));

        assertThrows(ResponseStatusException.class, () -> service.salvar(horarios));
        verify(repository, never()).save(any(HorarioFuncionamento.class));
    }

    private HorarioFuncionamento semanaValida() {
        HorarioFuncionamento horarios = new HorarioFuncionamento();
        List<HorarioFuncionamento.Dia> dias = new ArrayList<>();
        for (String nome : DIAS) {
            HorarioFuncionamento.Dia dia = new HorarioFuncionamento.Dia();
            dia.setDia(nome);
            dia.setFechado(false);
            dia.setAbertura(LocalTime.of(18, 0));
            dia.setFechamento(LocalTime.of(23, 0));
            dias.add(dia);
        }
        horarios.setDias(dias);
        return horarios;
    }
}