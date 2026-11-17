package com.innovax.barberia.cliente;

import com.innovax.barberia.cobertura.Cobertura;
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

    /** Obra social / prepaga del paciente (issue #67). Null = particular. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cobertura_id")
    private Cobertura cobertura;

    @Column(length = 40)
    private String numeroAfiliado;

    @Column(length = 60)
    private String plan;

    /** Cuándo aceptó el tratamiento de sus datos al reservar online (issue #73, solo salud). Null = sin registro. */
    private Instant consentimientoEn;

    /** Versión de los términos que aceptó ({@code TerminosSalud.VERSION}). */
    @Column(length = 20)
    private String consentimientoVersion;

    /** Cuándo se borraron sus datos personales a pedido suyo (issue #73). Null = datos vigentes. */
    private Instant anonimizadoEn;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public boolean isAnonimizado() {
        return anonimizadoEn != null;
    }

    public void registrarConsentimiento(String version, Instant cuando) {
        this.consentimientoEn = cuando;
        this.consentimientoVersion = version;
    }

    /**
     * Borra todo lo que identifica a la persona (issue #73, derecho de supresión). La fila queda para que sus
     * turnos y cobros sigan existiendo en caja. El teléfono es obligatorio, con formato E.164 y único por negocio
     * entre los clientes sin DNI: se pone uno inválido para la vida real (código de país +999 no existe) y
     * distinto para cada paciente.
     */
    public void anonimizar(Instant cuando) {
        this.nombre = "Paciente anonimizado";
        this.telefono = "+999" + "%010d".formatted(Math.floorMod(id.getLeastSignificantBits(), 10_000_000_000L));
        this.dni = null;
        this.fechaNacimiento = null;
        this.sexo = null;
        this.notas = null;
        this.cobertura = null;
        this.numeroAfiliado = null;
        this.plan = null;
        this.consentimientoEn = null;
        this.consentimientoVersion = null;
        this.anonimizadoEn = cuando;
    }
}
