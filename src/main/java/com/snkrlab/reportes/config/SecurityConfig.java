package com.snkrlab.reportes.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.tenant-id}")
    private String tenantId;

    @Value("${AZURE_CLIENT_ID:706b4543-805e-4c53-8da4-aaf4ff4a4706}")
    private String azureClientId;

    @Bean
    public JwtDecoder jwtDecoder() {
        String claves = "https://login.microsoftonline.com/"
                + tenantId + "/discovery/v2.0/keys";

        List<String> emisores = List.of(
                "https://sts.windows.net/" + tenantId + "/",
                "https://login.microsoftonline.com/" + tenantId + "/v2.0"
        );

        NimbusJwtDecoder decoder =
                NimbusJwtDecoder.withJwkSetUri(claves).build();

        OAuth2TokenValidator<Jwt> validarEmisor = token -> {
            String emisor = token.getClaimAsString("iss");

            if (emisor != null && emisores.contains(emisor)) {
                return OAuth2TokenValidatorResult.success();
            }

            return errorToken("El emisor del token no es válido.");
        };

        OAuth2TokenValidator<Jwt> validarAudiencia = token -> {
            List<String> audiencias = token.getAudience();

            if (audiencias != null
                    && (audiencias.contains(azureClientId)
                    || audiencias.contains("api://" + azureClientId))) {
                return OAuth2TokenValidatorResult.success();
            }

            return errorToken("El token no está destinado a esta API.");
        };

        OAuth2TokenValidator<Jwt> validarPermiso = token -> {
            String permisos = token.getClaimAsString("scp");

            if (permisos != null
                    && Arrays.asList(permisos.split("\\s+"))
                    .contains("write-read")) {
                return OAuth2TokenValidatorResult.success();
            }

            return errorToken("El token no contiene el permiso write-read.");
        };

        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        JwtValidators.createDefault(),
                        validarEmisor,
                        validarAudiencia,
                        validarPermiso
                )
        );

        return decoder;
    }

    private OAuth2TokenValidatorResult errorToken(String mensaje) {
        return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", mensaje, null)
        );
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter roles =
                new JwtGrantedAuthoritiesConverter();

        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(roles);

        return converter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of("*"));
        config.setAllowedMethods(
                List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")
        );
        config.setAllowedHeaders(
                List.of("Authorization", "Content-Type")
        );

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors ->
                        cors.configurationSource(corsConfigurationSource())
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().hasRole("Administrador")
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder())
                                .jwtAuthenticationConverter(
                                        jwtAuthenticationConverter()
                                )
                        )
                );

        return http.build();
    }
}