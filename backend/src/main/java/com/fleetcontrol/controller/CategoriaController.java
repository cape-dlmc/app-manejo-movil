package com.fleetcontrol.controller;

import com.fleetcontrol.dto.Dtos.CategoriaResponse;
import com.fleetcontrol.service.MovimientoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
@RequiredArgsConstructor
public class CategoriaController {

    private final MovimientoService movimientoService;

    @GetMapping
    public List<CategoriaResponse> listar() {
        return movimientoService.categorias();
    }
}
