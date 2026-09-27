package com.gymbro.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuracion de seguridad para la etapa de desarrollo.
 *
 * Sin esta clase, Spring Security bloquea todas las peticiones (401).
 * Aqui se dejan abiertos los endpoints /api/** para poder probarlos
 * desde Postman. Cuando se implemente el login con JWT, esta clase
 * es la que se cambia.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF protege formularios web con sesion. Una API REST que se
            // prueba con Postman no lo usa; si se deja activo, los POST,
            // PUT y DELETE responden 403.
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/**").permitAll()
                .anyRequest().authenticated());
        return http.build();
    }
}
