package com.innovax.barberia.horario;

import com.innovax.barberia.profesional.Profesional;
import com.innovax.barberia.common.DiaSemanaConverter;
import com.innovax.barberia.consultorio.Consultorio;
import com.innovax.barberia.sucursal.Sucursal;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

/** Horario semanal. Las horas están en la zona horaria de la barbería. */
@Entity
@Table(name = "horario_laboral")
@Getter
@Setter
@NoArgsConstructor
public class HorarioLaboral {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profesional_id", nullable = false)
    private Profesional profesional;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @Convert(converter = DiaSemanaConverter.class)
    @Column(nullable = false)
    private DayOfWeek diaSemana;

    @Column(nullable = false)
    private LocalTime horaInicio;

    @Column(nullable = false)
    private LocalTime horaFin;

    /**
     * Consultorio donde atiende en este tramo (issue #69). Null = sin consultorio asignado.
     * Tiene que ser de la misma sucursal (la base lo exige) y no puede estar ocupado por otro
     * profesional el mismo día en un tramo superpuesto.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultorio_id")
    private Consultorio consultorio;
}
