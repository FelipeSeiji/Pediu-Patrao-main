package com.umc.pediupatrao.entity;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "horarios_funcionamento")
@Getter
@Setter
public class HorarioFuncionamento {
    @Id
    private String id = "semanal";
    private List<Dia> dias = new ArrayList<>();

    @Getter
    @Setter
    public static class Dia {
        private String dia;
        private Boolean fechado = true;
        @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
        private LocalTime abertura;
        @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
        private LocalTime fechamento;
    }
}