package com.innovax.barberia.auditoria;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Registro de auditoría (issue #73): quién tocó los datos de qué paciente y cuándo.
 * Sin relaciones JPA ni claves foráneas a propósito: tiene que sobrevivir a lo que describe.
 */
@Entity
@Table(name = "acceso_paciente")
@Getter
@Setter
@NoArgsConstructor
public class AccesoPaciente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID negocioId;

    @Column(nullable = false)
    private UUID usuarioId;

    /** Snapshot del email al momento del acceso: sigue sirviendo si después se da de baja al usuario. */
    @Column(nullable = false, length = 255)
    private String usuarioEmail;

    @Column(nullable = false)
    private UUID clienteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccionPaciente accion;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant creadoEn;
}
