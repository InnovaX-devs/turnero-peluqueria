package com.innovax.barberia.auth;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/** Token de un solo uso para restablecer la contraseña. Se guarda hasheado (ver RecuperacionService). */
@Entity
@Table(name = "token_recuperacion")
@Getter
@NoArgsConstructor
public class TokenRecuperacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private Instant expiraEn;

    private Instant usadoEn;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public TokenRecuperacion(Usuario usuario, String tokenHash, Instant expiraEn) {
        this.usuario = usuario;
        this.tokenHash = tokenHash;
        this.expiraEn = expiraEn;
    }

    public boolean estaUsado() {
        return usadoEn != null;
    }

    public boolean estaVencido(Instant ahora) {
        return !ahora.isBefore(expiraEn);
    }

    public void marcarUsado(Instant ahora) {
        this.usadoEn = ahora;
    }
}