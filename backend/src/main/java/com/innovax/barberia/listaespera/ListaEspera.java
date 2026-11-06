package com.innovax.barberia.listaespera;

import com.innovax.barberia.auth.Usuario;
import com.innovax.barberia.cliente.Cliente;
import com.innovax.barberia.negocio.Negocio;
import com.innovax.barberia.profesional.Profesional;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Cliente anotado para un día en que no había lugar (issue #71). */
@Entity
@Table(name = "lista_espera")
@Getter
@Setter
@NoArgsConstructor
public class ListaEspera {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "negocio_id", nullable = false)
    private Negocio negocio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    /** Null = con cualquier profesional. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profesional_id")
    private Profesional profesional;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(length = 500)
    private String nota;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoListaEspera estado = EstadoListaEspera.ESPERANDO;

    /** Cuándo se liberó un lugar que le sirve (se canceló o se movió un turno). Null = todavía no. */
    private Instant avisoEn;

    /** Ej. "Se liberó 17:00 con Andres Biassi". */
    @Column(length = 200)
    private String avisoDetalle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por")
    private Usuario creadoPor;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
