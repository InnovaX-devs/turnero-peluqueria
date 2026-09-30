package com.innovax.barberia.atencion.gestion;

import com.innovax.barberia.atencion.Atencion;
import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

public interface GestionTurnoRepository extends Repository<Atencion, UUID> {

    Optional<Atencion> findByTokenGestion(UUID tokenGestion);
}
