package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.RegistroAuditoria;
import com.umc.pediupatrao.repository.RegistroAuditoriaRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuditoriaService {
    private final RegistroAuditoriaRepository repository;

    public AuditoriaService(RegistroAuditoriaRepository repository) {
        this.repository = repository;
    }

    public void registrar(String operacao, String entidade, String entidadeId,
            Map<String, Object> anteriores, Map<String, Object> novos, String justificativa) {
        RegistroAuditoria registro = new RegistroAuditoria();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        registro.setUsuario(authentication == null ? "sistema" : authentication.getName());
        registro.setPerfil(authentication == null ? "SISTEMA" : authentication.getAuthorities().stream()
                .findFirst().map(authority -> authority.getAuthority().replaceFirst("^ROLE_", ""))
                .orElse("DESCONHECIDO"));
        registro.setDataHora(Instant.now());
        registro.setOperacao(operacao);
        registro.setEntidade(entidade);
        registro.setEntidadeId(entidadeId);
        registro.setJustificativa(justificativa);

        Map<String, Object> valoresAnteriores = new LinkedHashMap<>();
        Map<String, Object> valoresNovos = new LinkedHashMap<>();
        Map<String, Object> valoresOriginais = anteriores == null ? Map.of() : anteriores;
        Map<String, Object> valoresAtualizados = novos == null ? Map.of() : novos;
        valoresAtualizados.forEach((campo, valorNovo) -> {
            if (valoresOriginais.containsKey(campo) && Objects.equals(valoresOriginais.get(campo), valorNovo)) {
                return;
            }
            if (campo.toLowerCase().contains("password") || campo.toLowerCase().contains("token")
                    || campo.toLowerCase().contains("secret")) {
                return;
            }
            if (valoresOriginais.containsKey(campo)) {
                valoresAnteriores.put(campo, valoresOriginais.get(campo));
            }
            valoresNovos.put(campo, valorNovo);
        });
        valoresOriginais.forEach((campo, valorAntigo) -> {
            if (!valoresAtualizados.containsKey(campo) && !campo.toLowerCase().contains("password")
                    && !campo.toLowerCase().contains("token") && !campo.toLowerCase().contains("secret")) {
                valoresAnteriores.put(campo, valorAntigo);
            }
        });
        registro.setValoresAnteriores(valoresAnteriores);
        registro.setValoresNovos(valoresNovos);
        repository.save(registro);
    }

    public List<RegistroAuditoria> listar() {
        return repository.findAllByOrderByDataHoraDesc();
    }
}