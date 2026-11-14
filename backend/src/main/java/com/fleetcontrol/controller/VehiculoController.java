package com.fleetcontrol.controller;

import com.fleetcontrol.dto.Dtos.VehiculoRequest;
import com.fleetcontrol.dto.Dtos.VehiculoResponse;
import com.fleetcontrol.service.UsuarioService;
import com.fleetcontrol.service.VehiculoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehiculos")
@RequiredArgsConstructor
public class VehiculoController {

    private final VehiculoService vehiculoService;
    private final UsuarioService usuarioService;

    @GetMapping
    public List<VehiculoResponse> listar() {
        return vehiculoService.listar();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public VehiculoResponse crear(@Valid @RequestBody VehiculoRequest req, Authentication auth) {
        return vehiculoService.crear(usuarioService.actual(auth), req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public VehiculoResponse actualizar(@PathVariable Long id, @Valid @RequestBody VehiculoRequest req,
                                       Authentication auth) {
        return vehiculoService.actualizar(usuarioService.actual(auth), id, req);
    }
}
