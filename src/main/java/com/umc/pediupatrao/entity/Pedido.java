package com.umc.pediupatrao.entity;

import jakarta.persistence.Entity;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Document(collection = "pedidos")
@Entity
@Getter
@Setter
public class Pedido {
    @Id
    private String id;
    private String clienteId;
    private String tipoPizza;
    private String status;
    private int quantidade;
    private String sabor;
    private List<Item> itens = new ArrayList<>();
    private BigDecimal subtotal = BigDecimal.ZERO;
    private BigDecimal descontoPercentual = BigDecimal.ZERO;
    private BigDecimal valorDesconto = BigDecimal.ZERO;
    private BigDecimal valorTotal = BigDecimal.ZERO;
    private String responsavelRegistro;
    private Instant dataHoraEntrada;
    private String responsavelSaida;
    private Instant dataHoraSaida;
    private String justificativaCancelamento;

    @Getter
    @Setter
    public static class Item {
        private String produtoId;
        private String nomeProduto;
        private int quantidade;
        private BigDecimal precoUnitario;
        private BigDecimal total;
    }

}
