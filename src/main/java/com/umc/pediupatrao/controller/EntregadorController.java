package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.entity.Entregador;
import com.umc.pediupatrao.service.EntregadorService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/entregadores")
@PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
public class EntregadorController {
    private final EntregadorService service;

    public EntregadorController(EntregadorService service) {
        this.service = service;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("entregadores", service.listar());
        model.addAttribute("content", "entregadores/lista :: content");
        return "layout";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("entregador", new Entregador());
        model.addAttribute("content", "entregadores/form :: content");
        return "layout";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable String id, Model model, RedirectAttributes attributes) {
        return service.buscarPorId(id).map(entregador -> {
            model.addAttribute("entregador", entregador);
            model.addAttribute("content", "entregadores/form :: content");
            return "layout";
        }).orElseGet(() -> {
            attributes.addFlashAttribute("erro", "Entregador não encontrado.");
            return "redirect:/entregadores";
        });
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute Entregador entregador, RedirectAttributes attributes) {
        try {
            service.salvar(entregador);
            attributes.addFlashAttribute("sucesso", "Entregador salvo com sucesso.");
        } catch (ResponseStatusException exception) {
            attributes.addFlashAttribute("erro", exception.getReason());
        }
        return "redirect:/entregadores";
    }

    @PostMapping("/{id}/status")
    public String alternarAtivo(@PathVariable String id, RedirectAttributes attributes) {
        try {
            service.alternarAtivo(id);
            attributes.addFlashAttribute("sucesso", "Situação do entregador atualizada.");
        } catch (ResponseStatusException exception) {
            attributes.addFlashAttribute("erro", exception.getReason());
        }
        return "redirect:/entregadores";
    }
}