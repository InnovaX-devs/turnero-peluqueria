package com.innovax.barberia.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    List<Usuario> findByNegocioIdAndRolOrderByEmailAsc(UUID negocioId, Rol rol);
}
