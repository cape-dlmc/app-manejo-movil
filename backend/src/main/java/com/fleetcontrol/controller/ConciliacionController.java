package com.fleetcontrol.controller;

import com.fleetcontrol.dto.Dtos.ConciliacionViaje;
import com.fleetcontrol.dto.Dtos.DashboardResponse;
import com.fleetcontrol.entity.Usuario;
import com.fleetcontrol.service.ConciliacionService;
import com.fleetcontrol.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ConciliacionController {

    private final ConciliacionService conciliacionService;
    private final UsuarioService usuarioService;

    @GetMapping("/conciliacion/viajes")
    public List<ConciliacionViaje> porViaje(Authentication auth) {
        Usuario u = usuarioService.actual(auth);
        return conciliacionService.porViaje(u, usuarioService.esAdmin(u));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public DashboardResponse dashboard() {
        return conciliacionService.dashboard();
    }
}
