package com.innovax.barberia.auditoria;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AccesoPacienteRepository extends JpaRepository<AccesoPaciente, UUID> {

    /** Los últimos 100 accesos a ese paciente, del más reciente al más antiguo. */
    List<AccesoPaciente> findTop100ByNegocioIdAndClienteIdOrderByCreadoEnDesc(UUID negocioId, UUID clienteId);
}
