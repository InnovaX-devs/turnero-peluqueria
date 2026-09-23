package com.innovax.barberia.cliente;

import com.innovax.barberia.barberia.Barberia;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cliente")
@Getter
@Setter
@NoArgsConstructor
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "barberia_id", nullable = false)
    private Barberia barberia;

    @Column(nullable = false, length = 120)
    private String nombre;

    /** Formato E.164, ej. +5493584111111. */
    @Column(nullable = false, length = 20)
    private String telefono;

    @Column(length = 2000)
    private String notas;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
