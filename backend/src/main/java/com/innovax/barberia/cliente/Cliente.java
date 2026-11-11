package com.innovax.barberia.cliente;

import com.innovax.barberia.negocio.Negocio;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
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
    @JoinColumn(name = "negocio_id", nullable = false)
    private Negocio negocio;

    @Column(nullable = false, length = 120)
    private String nombre;

    /** Formato E.164, ej. +5493584111111. */
    @Column(nullable = false, length = 20)
    private String telefono;

    /** 7 u 8 dígitos, sin puntos. Obligatorio en salud (ahí identifica al paciente); en el resto no se usa. */
    @Column(length = 8)
    private String dni;

    @Column
    private LocalDate fechaNacimiento;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Sexo sexo;

    @Column(length = 2000)
    private String notas;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
