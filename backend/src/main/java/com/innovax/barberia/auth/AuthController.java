package com.innovax.barberia.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

        static final String RESPUESTA_RECUPERAR =
            "Si el email corresponde a una cuenta, te enviamos un link para restablecer la contraseña.";

    private final AuthService authService;
    private final RecuperacionService recuperacionService;

    /** Público. Devuelve el token para mandar en el header Authorization: Bearer ... */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** Datos del usuario logueado. El front lo usa al recargar la página. */
    @GetMapping("/yo")
    public UsuarioResponse yo() {
        return authService.yo();
    }

        /**
     * Público. Responde siempre lo mismo, exista o no la cuenta, para no revelar qué emails
     * están registrados. Si existe, manda un email con un link que vence en 1 hora.
     */
    @PostMapping("/recuperar")
    public MensajeResponse recuperar(@Valid @RequestBody RecuperarRequest request) {
        recuperacionService.solicitar(request.email());
        return new MensajeResponse(RESPUESTA_RECUPERAR);
    }

    /** Con sesión. El usuario cambia su propia contraseña ("Mi cuenta"); pide la actual. */
    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiarPassword(@Valid @RequestBody CambiarPasswordRequest request) {
        authService.cambiarPassword(request.actual(), request.nueva());
    }

    /** Público. Cambia la contraseña con el token del email y lo invalida. */
    @PostMapping("/restablecer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restablecer(@Valid @RequestBody RestablecerRequest request) {
        recuperacionService.restablecer(request.token(), request.password());
    }
}
