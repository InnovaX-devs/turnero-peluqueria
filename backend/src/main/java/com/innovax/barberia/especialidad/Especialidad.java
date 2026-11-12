package com.innovax.barberia.especialidad;

import com.innovax.barberia.negocio.Negocio;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/** Especialidad de un profesional de salud (Kinesiología, Traumatología...). Cada negocio arma su lista. */
@Entity
@Table(name = "especialidad")
@Getter
@Setter
@NoArgsConstructor
public class Especialidad {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "negocio_id", nullable = false)
    private Negocio negocio;

    @Column(nullable = false, length = 80)
    private String nombre;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
