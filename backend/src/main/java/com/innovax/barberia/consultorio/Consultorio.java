package com.innovax.barberia.consultorio;

import com.innovax.barberia.sucursal.Sucursal;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Consultorio de una sucursal (issue #69): un recurso físico que comparten varios profesionales en
 * distintos horarios. Dos profesionales no pueden usar el mismo consultorio a la misma hora.
 *
 * <p>Nunca se borra (los turnos pasados guardan en qué consultorio fueron): se desactiva.
 */
@Entity
@Table(name = "consultorio")
@Getter
@Setter
@NoArgsConstructor
public class Consultorio {

    /** Regla de la base: dos turnos activos de profesionales distintos en el mismo consultorio a la misma hora. */
    public static final String RESTRICCION_TURNOS = "excl_atencion_consultorio";

    /** Regla de la base: dos horarios en el mismo consultorio, el mismo día, en tramos superpuestos. */
    public static final String RESTRICCION_HORARIOS = "excl_horario_consultorio";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false)
    private boolean activo = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** Si la excepción de la base (un DataIntegrityViolationException) es por la regla de turnos en el mismo consultorio. */
    public static boolean esChoqueDeTurnos(Throwable e) {
        return e.getMessage() != null && e.getMessage().contains(RESTRICCION_TURNOS);
    }
}
