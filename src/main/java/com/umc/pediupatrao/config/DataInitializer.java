package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(UsuarioRepository repo, PasswordEncoder passwordEncoder,
            @Value("${APP_BOOTSTRAP_ADMIN_PASSWORD:}") String senhaInicial) {
        return args -> {
            if (repo.findByUsername("admin").isEmpty() && !senhaInicial.isBlank()) {
                Usuario user = new Usuario();
                user.setUsername("admin");
                user.setPassword(passwordEncoder.encode(senhaInicial));
                user.setRole("ADMIN");

                repo.save(user);
            }
            repo.findByUsername("admin").ifPresent(user -> {
                if (user.getRole() == null || user.getRole().isBlank() || "USER".equalsIgnoreCase(user.getRole())) {
                    user.setRole("ADMIN");
                    repo.save(user);
                }
            });
        };
    }
}