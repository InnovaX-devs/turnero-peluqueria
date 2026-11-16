package com.innovax.barberia.cobertura;

import com.innovax.barberia.cobertura.dto.CoberturaResponse;
import com.innovax.barberia.common.RecursoNoEncontradoException;
import com.innovax.barberia.negocio.Negocio;
import com.innovax.barberia.negocio.NegocioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

/** Coberturas activas para el paso "¿Cómo vas a pagar?" de la reserva pública. Sin login. */
@RestController
@RequestMapping("/api/public/negocios/{slug}/coberturas")
@RequiredArgsConstructor
public class CoberturaPublicoController {

    private final NegocioRepository negocios;
    private final CoberturaRepository coberturas;

    @GetMapping
    @Transactional(readOnly = true)
    public List<CoberturaResponse> listar(@PathVariable String slug) {
        Negocio negocio = negocios.findBySlug(slug.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new RecursoNoEncontradoException("Negocio no encontrado"));
        return coberturas.findByNegocioIdAndActivaTrueOrderByNombreAsc(negocio.getId()).stream()
                .map(CoberturaResponse::de)
                .toList();
    }
}
