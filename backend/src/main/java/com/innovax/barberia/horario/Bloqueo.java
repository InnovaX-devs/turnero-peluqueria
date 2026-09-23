package com.innovax.barberia.horario;

import com.innovax.barberia.barbero.Barbero;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bloqueo")
@Getter
@Setter
@NoArgsConstructor
public class Bloqueo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "barbero_id", nullable = false)
    private Barbero barbero;

    @Column(nullable = false)
    private Instant inicio;

    @Column(nullable = false)
    private Instant fin;

    @Column(length = 200)
    private String motivo;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
