package com.innovax.barberia.sucursal;

import com.innovax.barberia.barberia.Barberia;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sucursal")
@Getter
@Setter
@NoArgsConstructor
public class Sucursal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "barberia_id", nullable = false)
    private Barberia barberia;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 255)
    private String direccion;

    @Column(nullable = false, length = 120)
    private String ciudad;

    @Column(length = 20)
    private String telefono;

    @Column(nullable = false)
    private boolean activa = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
