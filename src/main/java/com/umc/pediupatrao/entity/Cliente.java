package com.umc.pediupatrao.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Getter;
import lombok.Setter;

@Document(collection = "clientes")
@Getter
@Setter
public class Cliente {

    @Id
    private String id;
    private String nome;
    private String telefone;

    // Endereço completo
    private String cep;
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String estado;

    // Mantido para compatibilidade com dados antigos
    private String endereco;

}