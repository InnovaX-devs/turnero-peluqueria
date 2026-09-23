package com.innovax.barberia.atencion;

import com.innovax.barberia.barberia.Barberia;
import com.innovax.barberia.barbero.Barbero;
import com.innovax.barberia.cliente.Cliente;
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
 * La base impide dos atenciones RESERVADA/COMPLETADA superpuestas del mismo barbero.
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
    @JoinColumn(name = "barberia_id", nullable = false)
    private Barberia barberia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "barbero_id", nullable = false)
    private Barbero barbero;

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

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
