package com.innovax.barberia.barberia;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "barberia")
@Getter
@Setter
@NoArgsConstructor
public class Barberia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, unique = true, length = 80)
    private String slug;

    @Column(length = 500)
    private String logoUrl;

    @Column(nullable = false)
    private boolean reservasOnline = false;

    @Column(nullable = false, length = 60)
    private String zonaHoraria = "America/Argentina/Cordoba";

    @Column(nullable = false)
    private int anticipacionMinimaMin = 60;

    @Column(nullable = false)
    private int anticipacionMaximaDias = 30;

    @Column(nullable = false)
    private int intervaloSlotsMin = 30;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
