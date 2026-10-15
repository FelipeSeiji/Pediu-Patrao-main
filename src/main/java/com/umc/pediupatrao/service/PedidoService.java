package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import com.umc.pediupatrao.repository.ClienteRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PedidoService {
    private static final BigDecimal CEM = new BigDecimal("100");
    private final PedidoRepository pedidoRepository;
    private final ProdutoRepository produtoRepository;
    private final ClienteRepository clienteRepository;
    private final AuditoriaService auditoriaService;

    public PedidoService(PedidoRepository pedidoRepository, ProdutoRepository produtoRepository,
            ClienteRepository clienteRepository, AuditoriaService auditoriaService) {
        this.pedidoRepository = pedidoRepository;
        this.produtoRepository = produtoRepository;
        this.clienteRepository = clienteRepository;
        this.auditoriaService = auditoriaService;
    }

    @PreAuthorize("hasAnyRole('ATENDENTE', 'GERENTE')")
    public Pedido criarPedido(Pedido pedido) {
        if (pedido.getClienteId() == null || pedido.getClienteId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cliente é obrigatório.");
        }
        if (!clienteRepository.existsById(pedido.getClienteId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cliente não encontrado.");
        }
        if (pedido.getItens() == null || pedido.getItens().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe ao menos um item.");
        }
        List<Pedido.Item> itensCalculados = new ArrayList<>();
        for (Pedido.Item itemRecebido : pedido.getItens()) {
            if (itemRecebido.getProdutoId() == null || itemRecebido.getProdutoId().isBlank()
                    || itemRecebido.getQuantidade() < 1 || itemRecebido.getQuantidade() > 99) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Produto e quantidade válida são obrigatórios.");
            }
            Produto produto = produtoRepository.findById(itemRecebido.getProdutoId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Produto não encontrado."));
            if (Boolean.FALSE.equals(produto.getAtivo()) || produto.getPreco() == null || produto.getPreco() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Produto indisponível para pedidos.");
            }
            BigDecimal preco = BigDecimal.valueOf(produto.getPreco()).setScale(2, RoundingMode.HALF_UP);
            Pedido.Item item = new Pedido.Item();
            item.setProdutoId(produto.getId());
            item.setNomeProduto(produto.getNome());
            item.setQuantidade(itemRecebido.getQuantidade());
            item.setPrecoUnitario(preco);
            item.setTotal(preco.multiply(BigDecimal.valueOf(item.getQuantidade())).setScale(2, RoundingMode.HALF_UP));
            itensCalculados.add(item);
        }
        pedido.setId(null);
        pedido.setItens(itensCalculados);
        pedido.setStatus("RECEBIDO");
        pedido.setSubtotal(calcularSubtotal(itensCalculados));
        pedido.setDescontoPercentual(BigDecimal.ZERO.setScale(2));
        pedido.setValorDesconto(BigDecimal.ZERO.setScale(2));
        pedido.setValorTotal(pedido.getSubtotal());
        pedido.setResponsavelRegistro(usuarioAtual());
        pedido.setDataHoraEntrada(Instant.now());
        pedido.setResponsavelSaida(null);
        pedido.setDataHoraSaida(null);
        pedido.setJustificativaCancelamento(null);
        Pedido salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar("INCLUSAO DE PEDIDO", "PEDIDO", salvo.getId(), Map.of(), snapshot(salvo), null);
        return salvo;
    }

    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll();
    }

    @PreAuthorize("hasAnyRole('ATENDENTE', 'GERENTE')")
    public Pedido atualizarStatus(String id, String status) {
        Pedido pedido = buscarPedido(id);
        String statusAtual = normalizarStatus(pedido.getStatus());
        String novoStatus = status == null ? "" : status.trim().toUpperCase();
        if ("CANCELADO".equals(statusAtual)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Pedido cancelado não pode ser alterado.");
        }
        if (!transicaoValida(statusAtual, novoStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Transição de status inválida.");
        }
        Map<String, Object> anteriores = snapshot(pedido);
        pedido.setStatus(novoStatus);
        if ("SAIU_PARA_ENTREGA".equals(novoStatus) || "RETIRADO".equals(novoStatus)) {
            pedido.setDataHoraSaida(Instant.now());
            pedido.setResponsavelSaida(usuarioAtual());
        }
        Pedido salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar("ALTERACAO DE STATUS", "PEDIDO", id, anteriores, snapshot(salvo), null);
        return salvo;
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Pedido aplicarDesconto(String id, BigDecimal percentual, String justificativa) {
        Pedido pedido = buscarPedido(id);
        validarAntesDaSaida(pedido);
        if (percentual == null || percentual.compareTo(BigDecimal.ZERO) <= 0
                || percentual.compareTo(new BigDecimal("20")) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O desconto deve ser maior que zero e limitado a 20%.");
        }
        Map<String, Object> anteriores = snapshot(pedido);
        BigDecimal descontoPercentual = percentual.setScale(2, RoundingMode.HALF_UP);
        BigDecimal valorDesconto = pedido.getSubtotal().multiply(descontoPercentual)
                .divide(CEM, 2, RoundingMode.HALF_UP);
        pedido.setDescontoPercentual(descontoPercentual);
        pedido.setValorDesconto(valorDesconto);
        pedido.setValorTotal(pedido.getSubtotal().subtract(valorDesconto).setScale(2, RoundingMode.HALF_UP));
        Pedido salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar("APLICACAO DE DESCONTO", "PEDIDO", id, anteriores, snapshot(salvo), justificativa);
        return salvo;
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Pedido cancelarPedido(String id, String justificativa) {
        Pedido pedido = buscarPedido(id);
        validarAntesDaSaida(pedido);
        if (justificativa == null || justificativa.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A justificativa do cancelamento é obrigatória.");
        }
        Map<String, Object> anteriores = snapshot(pedido);
        pedido.setStatus("CANCELADO");
        pedido.setJustificativaCancelamento(justificativa.trim());
        Pedido salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar("CANCELAMENTO DE PEDIDO", "PEDIDO", id, anteriores, snapshot(salvo), justificativa.trim());
        return salvo;
    }

    private void validarAntesDaSaida(Pedido pedido) {
        String status = normalizarStatus(pedido.getStatus());
        if ("CANCELADO".equals(status) || "FINALIZADO".equals(status)
                || "SAIU_PARA_ENTREGA".equals(status) || "RETIRADO".equals(status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A operação só é permitida antes da saída do pedido.");
        }
    }

    private boolean transicaoValida(String atual, String novo) {
        return switch (atual) {
            case "RECEBIDO" -> "EM_PREPARACAO".equals(novo);
            case "EM_PREPARACAO" -> "PRONTO".equals(novo);
            case "PRONTO" -> "SAIU_PARA_ENTREGA".equals(novo) || "RETIRADO".equals(novo);
            case "SAIU_PARA_ENTREGA", "RETIRADO" -> "FINALIZADO".equals(novo);
            default -> false;
        };
    }

    private String normalizarStatus(String status) {
        if ("EM_PREPARO".equals(status)) return "EM_PREPARACAO";
        if ("ENTREGUE".equals(status)) return "FINALIZADO";
        return status == null ? "" : status;
    }

    private Pedido buscarPedido(String id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado."));
    }

    private String usuarioAtual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticação necessária.");
        }
        return authentication.getName();
    }

    private BigDecimal calcularSubtotal(List<Pedido.Item> itens) {
        return itens.stream().map(Pedido.Item::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, Object> snapshot(Pedido pedido) {
        Map<String, Object> valores = new LinkedHashMap<>();
        valores.put("clienteId", pedido.getClienteId());
        valores.put("itens", pedido.getItens());
        valores.put("status", pedido.getStatus());
        valores.put("subtotal", pedido.getSubtotal());
        valores.put("descontoPercentual", pedido.getDescontoPercentual());
        valores.put("valorDesconto", pedido.getValorDesconto());
        valores.put("valorTotal", pedido.getValorTotal());
        valores.put("responsavelRegistro", pedido.getResponsavelRegistro());
        valores.put("dataHoraEntrada", pedido.getDataHoraEntrada());
        valores.put("responsavelSaida", pedido.getResponsavelSaida());
        valores.put("dataHoraSaida", pedido.getDataHoraSaida());
        valores.put("justificativaCancelamento", pedido.getJustificativaCancelamento());
        return valores;
    }
}