package com.innovax.barberia.servicio;

import com.innovax.barberia.barberia.Barberia;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "servicio")
@Getter
@Setter
@NoArgsConstructor
public class Servicio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "barberia_id", nullable = false)
    private Barberia barberia;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false)
    private int duracionMin;

    /** En centavos: $15.000,00 = 1500000. */
    @Column(nullable = false)
    private long precio;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(nullable = false)
    private int orden = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
