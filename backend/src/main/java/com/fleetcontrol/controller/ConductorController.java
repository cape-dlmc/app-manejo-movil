package com.fleetcontrol.controller;

import com.fleetcontrol.dto.Dtos.ConductorRequest;
import com.fleetcontrol.dto.Dtos.ConductorResponse;
import com.fleetcontrol.dto.Dtos.ConductorUpdateRequest;
import com.fleetcontrol.dto.Dtos.EstadoRequest;
import com.fleetcontrol.service.ConductorService;
import com.fleetcontrol.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conductores")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ConductorController {

    private final ConductorService conductorService;
    private final UsuarioService usuarioService;

    @GetMapping
    public List<ConductorResponse> listar() {
        return conductorService.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConductorResponse crear(@Valid @RequestBody ConductorRequest req, Authentication auth) {
        return conductorService.crear(usuarioService.actual(auth), req);
    }

    @PutMapping("/{id}")
    public ConductorResponse actualizar(@PathVariable Long id, @Valid @RequestBody ConductorUpdateRequest req,
                                        Authentication auth) {
        return conductorService.actualizar(usuarioService.actual(auth), id, req);
    }

    @PatchMapping("/{id}/estado")
    public ConductorResponse estado(@PathVariable Long id, @Valid @RequestBody EstadoRequest req,
                                    Authentication auth) {
        return conductorService.cambiarEstado(usuarioService.actual(auth), id, req.estado());
    }
}
