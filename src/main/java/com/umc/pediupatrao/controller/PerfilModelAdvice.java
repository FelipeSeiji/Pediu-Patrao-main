package com.umc.pediupatrao.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = Controller.class)
public class PerfilModelAdvice {
    @ModelAttribute
    public void adicionarPerfis(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("perfilAdmin", temPerfil(authentication, "ADMIN"));
        model.addAttribute("perfilGerente", temPerfil(authentication, "GERENTE"));
        model.addAttribute("perfilAtendente", temPerfil(authentication, "ATENDENTE"));
    }

    private boolean temPerfil(Authentication authentication, String perfil) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + perfil));
    }
}