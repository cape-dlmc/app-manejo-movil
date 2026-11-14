package com.fleetcontrol.controller;

import com.fleetcontrol.dto.Dtos.LoginRequest;
import com.fleetcontrol.dto.Dtos.LoginResponse;
import com.fleetcontrol.entity.Usuario;
import com.fleetcontrol.exception.ApiException;
import com.fleetcontrol.repository.UsuarioRepository;
import com.fleetcontrol.security.JwtService;
import com.fleetcontrol.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UsuarioService usuarioService;

    /** RF-01: el mensaje de error es genérico para no revelar si el correo existe. */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        Usuario u = usuarioRepository.findByCorreo(req.correo())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos"));
        if (!Boolean.TRUE.equals(u.getEstado()) || !passwordEncoder.matches(req.password(), u.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos");
        }
        return new LoginResponse(jwtService.generar(u.getCorreo(), u.getRol()),
                u.getNombre(), u.getCorreo(), u.getRol());
    }

    @GetMapping("/me")
    public LoginResponse me(Authentication auth) {
        Usuario u = usuarioService.actual(auth);
        return new LoginResponse(null, u.getNombre(), u.getCorreo(), u.getRol());
    }
}
