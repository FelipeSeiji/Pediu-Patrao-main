package com.umc.pediupatrao.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Document(collection = "usuarios") // Define a coleção no MongoDB
@Getter
@Setter
public class Usuario {
    @Id
    private String id;
    private String username; // Nome de usuário para login
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    private String role;     // Papel do usuário, como ADMIN ou USER

}
