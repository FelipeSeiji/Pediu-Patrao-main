package com.umc.pediupatrao.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Document(collection = "auditorias")
@Getter
@Setter
public class RegistroAuditoria {
    @Id
    private String id;
    private String usuario;
    private String perfil;
    private Instant dataHora;
    private String operacao;
    private String entidade;
    private String entidadeId;
    private Map<String, Object> valoresAnteriores = new LinkedHashMap<>();
    private Map<String, Object> valoresNovos = new LinkedHashMap<>();
    private String justificativa;

}