package com.innovax.barberia.cobertura;

import com.innovax.barberia.negocio.Negocio;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Obra social o prepaga que trabaja un negocio (PAMI, OSDE, Swiss Medical...). Issue #67.
 * No se borra: se desactiva, para no perder las atenciones que ya la usaron (reporte de facturación).
 */
@Entity
@Table(name = "cobertura")
@Getter
@Setter
@NoArgsConstructor
public class Cobertura {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "negocio_id", nullable = false)
    private Negocio negocio;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false)
    private boolean activa = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
