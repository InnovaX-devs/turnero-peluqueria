package com.innovax.barberia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Configuración del token: app.jwt.secret y app.jwt.expiracion. */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration expiracion) {
}
