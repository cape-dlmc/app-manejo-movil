package com.fleetcontrol.controller;

import com.fleetcontrol.dto.Dtos.GastoRequest;
import com.fleetcontrol.dto.Dtos.MovimientoResponse;
import com.fleetcontrol.dto.Dtos.RevisionRequest;
import com.fleetcontrol.entity.Usuario;
import com.fleetcontrol.service.ComprobanteService;
import com.fleetcontrol.service.MovimientoService;
import com.fleetcontrol.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;

@RestController
@RequestMapping("/api/movimientos")
@RequiredArgsConstructor
public class MovimientoController {

    private final MovimientoService movimientoService;
    private final ComprobanteService comprobanteService;
    private final UsuarioService usuarioService;

    @GetMapping
    public List<MovimientoResponse> listar(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long vehiculoId,
            @RequestParam(required = false) Long viajeId,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long conductorId,
            Authentication auth) {
        Usuario u = usuarioService.actual(auth);
        return movimientoService.listar(u, usuarioService.esAdmin(u), estado, desde, hasta, vehiculoId, viajeId,
                categoriaId, conductorId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MovimientoResponse registrar(@Valid @RequestBody GastoRequest req, Authentication auth) {
        return movimientoService.registrarGasto(usuarioService.actual(auth), req);
    }

    @PatchMapping("/{id}/revision")
    @PreAuthorize("hasRole('ADMIN')")
    public MovimientoResponse revisar(@PathVariable Long id, @Valid @RequestBody RevisionRequest req,
                                      Authentication auth) {
        return movimientoService.revisar(usuarioService.actual(auth), id, req);
    }

    @PostMapping(value = "/{id}/comprobantes", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> adjuntar(@PathVariable Long id, @RequestParam("archivo") MultipartFile archivo,
                                      Authentication auth) {
        Long idComprobante = comprobanteService.guardar(usuarioService.actual(auth), id, archivo);
        return Map.of("idComprobante", idComprobante);
    }
}
