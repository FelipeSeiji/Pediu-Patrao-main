package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.service.PedidoService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/pedidos")
public class PedidoPageController {
    private final PedidoService pedidoService;

    public PedidoPageController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping("/salvar")
    public String criar(@org.springframework.web.bind.annotation.ModelAttribute Pedido pedido,
            RedirectAttributes attributes) {
        return executar(attributes, () -> pedidoService.criarPedido(pedido), "Pedido registrado com sucesso!");
    }

    @PostMapping("/{id}/status")
    public String atualizarStatus(@PathVariable String id, @RequestParam String status,
            RedirectAttributes attributes) {
        return executar(attributes, () -> pedidoService.atualizarStatus(id, status), "Status atualizado com sucesso!");
    }

    @PostMapping("/{id}/desconto")
    public String aplicarDesconto(@PathVariable String id, @RequestParam java.math.BigDecimal percentual,
            @RequestParam(required = false) String justificativa, RedirectAttributes attributes) {
        return executar(attributes, () -> pedidoService.aplicarDesconto(id, percentual, justificativa), "Desconto aplicado!");
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable String id, @RequestParam String justificativa,
            RedirectAttributes attributes) {
        return executar(attributes, () -> pedidoService.cancelarPedido(id, justificativa), "Pedido cancelado.");
    }

    private String executar(RedirectAttributes attributes, Runnable operacao, String sucesso) {
        try {
            operacao.run();
            attributes.addFlashAttribute("sucesso", sucesso);
        } catch (org.springframework.web.server.ResponseStatusException exception) {
            attributes.addFlashAttribute("erro", exception.getReason());
        }
        return "redirect:/pedidos";
    }
}