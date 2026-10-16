package com.umc.pediupatrao.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "entregadores")
@Getter
@Setter
public class Entregador {
    @Id
    private String id;
    private String nome;
    private String telefone;
    private Boolean ativo = true;
}