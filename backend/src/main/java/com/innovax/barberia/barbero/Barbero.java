package com.innovax.barberia.barbero;

import com.innovax.barberia.auth.Usuario;
import com.innovax.barberia.barberia.Barberia;
import com.innovax.barberia.servicio.Servicio;
import com.innovax.barberia.sucursal.Sucursal;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "barbero")
@Getter
@Setter
@NoArgsConstructor
public class Barbero {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "barberia_id", nullable = false)
    private Barberia barberia;

    /** Null si el barbero no tiene login propio. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", unique = true)
    private Usuario usuario;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, length = 80)
    private String apellido;

    @Column(length = 500)
    private String fotoUrl;

    @Column(nullable = false)
    private boolean activo = true;

    @ManyToMany
    @JoinTable(
            name = "barbero_sucursal",
            joinColumns = @JoinColumn(name = "barbero_id"),
            inverseJoinColumns = @JoinColumn(name = "sucursal_id"))
    private Set<Sucursal> sucursales = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "barbero_servicio",
            joinColumns = @JoinColumn(name = "barbero_id"),
            inverseJoinColumns = @JoinColumn(name = "servicio_id"))
    private Set<Servicio> servicios = new HashSet<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
