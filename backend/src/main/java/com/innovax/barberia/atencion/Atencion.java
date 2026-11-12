package com.innovax.barberia.atencion;

import com.innovax.barberia.auth.Usuario;
import com.innovax.barberia.negocio.Negocio;
import com.innovax.barberia.profesional.Profesional;
import com.innovax.barberia.cliente.Cliente;
import com.innovax.barberia.consultorio.Consultorio;
import com.innovax.barberia.servicio.Servicio;
import com.innovax.barberia.sucursal.Sucursal;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Un corte: puede ser un turno reservado online o una atención cargada a mano.
 * La base impide dos atenciones RESERVADA/COMPLETADA superpuestas del mismo barbero (salvo sobreturnos),
 * y dos de profesionales distintos en el mismo consultorio a la misma hora.
 */
@Entity
@Table(name = "atencion")
@Getter
@Setter
@NoArgsConstructor
public class Atencion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "negocio_id", nullable = false)
    private Negocio negocio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profesional_id", nullable = false)
    private Profesional profesional;

    /** Null = corte anónimo. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servicio_id", nullable = false)
    private Servicio servicio;

    @Column(nullable = false)
    private Instant inicio;

    @Column(nullable = false)
    private Instant fin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoAtencion estado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrigenAtencion origen;

    /** En centavos. Se copia del servicio al crear y se puede ajustar al cobrar. */
    @Column(nullable = false)
    private long precioFinal;

    /** Null hasta que se cobra. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MetodoPago metodoPago;

    /** Va en el link para que el cliente cancele o reprograme sin login. */
    @Column(nullable = false, unique = true, updatable = false)
    private UUID tokenGestion = UUID.randomUUID();

    @Column(length = 2000)
    private String notas;

    /** Quién marcó el turno como completado, ausente o cancelado. Null: nunca se cambió. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_cambiado_por")
    private Usuario estadoCambiadoPor;

    /** Cuándo se hizo ese último cambio de estado. */
    private Instant estadoCambiadoEn;

    /** Cuándo llegó el cliente al local (sala de espera, issue #71). Null = todavía no llegó. */
    private Instant llegadaEn;

    /**
     * Sobreturno (issue #71): cargado a propósito encima de otro turno o fuera del horario. Queda afuera
     * de la regla de la base que impide turnos superpuestos. Solo lo crea admin o recepción desde el panel;
     * la reserva online nunca.
     */
    @Column(nullable = false)
    private boolean sobreturno = false;

    /**
     * Consultorio donde se atiende (issue #69). Se copia del tramo del horario al dar el turno; null si el
     * tramo no tiene consultorio o el turno cae fuera del horario. La base impide dos turnos activos de
     * profesionales distintos en el mismo consultorio a la misma hora ({@code excl_atencion_consultorio}).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultorio_id")
    private Consultorio consultorio;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}