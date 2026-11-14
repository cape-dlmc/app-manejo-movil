package com.fleetcontrol.controller;

import com.fleetcontrol.entity.Comprobante;
import com.fleetcontrol.entity.Usuario;
import com.fleetcontrol.service.ComprobanteService;
import com.fleetcontrol.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/comprobantes")
@RequiredArgsConstructor
public class ComprobanteController {

    private final ComprobanteService comprobanteService;
    private final UsuarioService usuarioService;

    /** Entrega el archivo solo si el usuario tiene permiso (RF-08). */
    @GetMapping("/{id}")
    public ResponseEntity<Resource> descargar(@PathVariable Long id, Authentication auth) {
        Usuario u = usuarioService.actual(auth);
        Comprobante c = comprobanteService.consultar(u, id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(c.getTipoArchivo()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + c.getNombreArchivo() + "\"")
                .body(comprobanteService.recurso(c));
    }
}
