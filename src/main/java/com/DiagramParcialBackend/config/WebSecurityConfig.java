package com.DiagramParcialBackend.config;

import com.DiagramParcialBackend.Security.JwtRequestFilter;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@AllArgsConstructor // Lombok genera el constructor con todos los argumentos
public class WebSecurityConfig {

    private final AuthenticationProvider authenticationProvider;
    private final JwtRequestFilter jwtRequestFilter;

    @Bean
    protected SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable()) // Deshabilitar CSRF para APIs REST
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/auth/**", "/registro**", "/js/**", "/css/**", "/img/**","/user/**").permitAll() // Permitir acceso sin autenticación
                        .requestMatchers("/session/**").authenticated() // Requiere autenticación para estas rutas
                        .requestMatchers("/user-session/**").authenticated()
                        //Modificar esta configuracion
                     // Cualquier otra solicitud requiere autenticación
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // No manejar sesiones, usar JWT
                )
                .authenticationProvider(authenticationProvider) // Proveedor de autenticación personalizado
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class) // Filtro JWT antes del filtro de autenticación predeterminado
                .build();
    }
}
