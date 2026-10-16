package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.entity.HorarioFuncionamento;
import com.umc.pediupatrao.service.HorarioFuncionamentoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/horarios")
@PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
public class HorarioFuncionamentoController {
    private final HorarioFuncionamentoService service;

    public HorarioFuncionamentoController(HorarioFuncionamentoService service) {
        this.service = service;
    }

    @GetMapping
    public String exibir(Model model) {
        model.addAttribute("horarios", service.obter());
        model.addAttribute("content", "horarios :: content");
        return "layout";
    }

    @PostMapping
    public String salvar(@ModelAttribute("horarios") HorarioFuncionamento horarios, Model model) {
        try {
            service.salvar(horarios);
            model.addAttribute("sucesso", "Horários de funcionamento salvos.");
            model.addAttribute("horarios", service.obter());
        } catch (ResponseStatusException exception) {
            model.addAttribute("erro", exception.getReason());
            model.addAttribute("horarios", horarios);
        }
        model.addAttribute("content", "horarios :: content");
        return "layout";
    }
}