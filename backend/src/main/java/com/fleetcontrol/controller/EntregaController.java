package com.fleetcontrol.controller;

import com.fleetcontrol.dto.Dtos.EntregaRequest;
import com.fleetcontrol.dto.Dtos.EntregaResponse;
import com.fleetcontrol.entity.Usuario;
import com.fleetcontrol.service.EntregaService;
import com.fleetcontrol.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/entregas")
@RequiredArgsConstructor
public class EntregaController {

    private final EntregaService entregaService;
    private final UsuarioService usuarioService;

    @GetMapping
    public List<EntregaResponse> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long conductorId,
            @RequestParam(required = false) Long viajeId,
            @RequestParam(required = false) String estado,
            Authentication auth) {
        Usuario u = usuarioService.actual(auth);
        return entregaService.listar(u, usuarioService.esAdmin(u), desde, hasta, conductorId, viajeId, estado);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EntregaResponse registrar(@Valid @RequestBody EntregaRequest req, Authentication auth) {
        Usuario actual = usuarioService.actual(auth);
        return entregaService.registrar(actual, usuarioService.esAdmin(actual), req);
    }

    @PatchMapping("/{id}/verificar")
    @PreAuthorize("hasRole('ADMIN')")
    public EntregaResponse verificar(@PathVariable Long id, Authentication auth) {
        return entregaService.verificar(usuarioService.actual(auth), id);
    }
}
