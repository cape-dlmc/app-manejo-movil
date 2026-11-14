package com.fleetcontrol.service;

import com.fleetcontrol.entity.Usuario;
import com.fleetcontrol.exception.ApiException;
import com.fleetcontrol.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    /** Usuario que hace la petición, identificado por el correo del token. */
    public Usuario actual(Authentication auth) {
        if (auth == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Debe iniciar sesión");
        }
        return usuarioRepository.findByCorreo(auth.getName())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Sesión no válida"));
    }

    public boolean esAdmin(Usuario u) {
        return "ADMIN".equals(u.getRol());
    }
}
