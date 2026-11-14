package com.fleetcontrol.controller;

import com.fleetcontrol.dto.Dtos.EstadoRequest;
import com.fleetcontrol.dto.Dtos.ViajeRequest;
import com.fleetcontrol.dto.Dtos.ViajeResponse;
import com.fleetcontrol.entity.Usuario;
import com.fleetcontrol.service.UsuarioService;
import com.fleetcontrol.service.ViajeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/viajes")
@RequiredArgsConstructor
public class ViajeController {

    private final ViajeService viajeService;
    private final UsuarioService usuarioService;

    @GetMapping
    public List<ViajeResponse> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long vehiculoId,
            @RequestParam(required = false) Long conductorId,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String tipoServicio,
            Authentication auth) {
        Usuario u = usuarioService.actual(auth);
        return viajeService.listar(u, usuarioService.esAdmin(u), desde, hasta, vehiculoId, conductorId, estado,
                tipoServicio);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ViajeResponse crear(@Valid @RequestBody ViajeRequest req, Authentication auth) {
        return viajeService.crear(usuarioService.actual(auth), req);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public ViajeResponse estado(@PathVariable Long id, @Valid @RequestBody EstadoRequest req, Authentication auth) {
        return viajeService.cambiarEstado(usuarioService.actual(auth), id, req.estado());
    }
}
