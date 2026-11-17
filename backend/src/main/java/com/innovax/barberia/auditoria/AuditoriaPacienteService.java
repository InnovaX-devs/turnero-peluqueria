package com.innovax.barberia.auditoria;

import com.innovax.barberia.auth.Usuario;
import com.innovax.barberia.auth.UsuarioActual;
import com.innovax.barberia.auth.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Deja constancia de cada acceso a datos de pacientes del panel (issue #73).
 *
 * <p>Se une a la transacción de quien lo llama: si la operación falla y se revierte, el registro
 * también. Por eso NO se puede llamar desde una transacción {@code readOnly = true} (Postgres rechaza
 * el INSERT): los métodos que auditan son {@code @Transactional} comunes.
 */
@Service
@RequiredArgsConstructor
public class AuditoriaPacienteService {

    private final AccesoPacienteRepository accesos;
    private final UsuarioActual usuarioActual;
    private final UsuarioRepository usuarios;

    @Transactional
    public void registrar(UUID clienteId, AccionPaciente accion) {
        UUID usuarioId = usuarioActual.id();
        AccesoPaciente acceso = new AccesoPaciente();
        acceso.setNegocioId(usuarioActual.negocioId());
        acceso.setUsuarioId(usuarioId);
        acceso.setUsuarioEmail(usuarios.findById(usuarioId).map(Usuario::getEmail).orElse("desconocido"));
        acceso.setClienteId(clienteId);
        acceso.setAccion(accion);
        accesos.save(acceso);
    }
}
