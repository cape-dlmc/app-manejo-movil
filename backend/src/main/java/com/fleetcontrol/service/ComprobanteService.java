package com.fleetcontrol.service;

import com.fleetcontrol.entity.Comprobante;
import com.fleetcontrol.entity.MovimientoFinanciero;
import com.fleetcontrol.entity.Usuario;
import com.fleetcontrol.exception.ApiException;
import com.fleetcontrol.repository.ComprobanteRepository;
import com.fleetcontrol.repository.MovimientoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

/** Guarda los soportes fuera del código y con nombre aleatorio (RNF-03, sección 10.7). */
@Service
@RequiredArgsConstructor
public class ComprobanteService {

    private static final Map<String, String> EXTENSIONES = Map.of(
            "application/pdf", ".pdf", "image/jpeg", ".jpg", "image/png", ".png");

    private final ComprobanteRepository comprobanteRepository;
    private final MovimientoRepository movimientoRepository;
    private final AuditoriaService auditoria;

    @Value("${app.storage.dir}")
    private String carpeta;

    public Long guardar(Usuario actual, Long idMovimiento, MultipartFile archivo) {
        MovimientoFinanciero m = movimientoRepository.findById(idMovimiento)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Movimiento no encontrado"));
        puedeVer(actual, m);   // admin o dueño del gasto
        String tipo = archivo.getContentType();
        if (archivo.isEmpty() || tipo == null || !EXTENSIONES.containsKey(tipo)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Solo se aceptan archivos PDF, JPG o PNG");
        }

        try {
            Path dir = Paths.get(carpeta).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            Path destino = dir.resolve(UUID.randomUUID() + EXTENSIONES.get(tipo));
            try (InputStream in = archivo.getInputStream()) {
                Files.copy(in, destino, StandardCopyOption.REPLACE_EXISTING);
            }
            Comprobante c = new Comprobante();
            c.setMovimiento(m);
            c.setNombreArchivo(archivo.getOriginalFilename());
            c.setRutaArchivo(destino.toString());
            c.setTipoArchivo(tipo);
            c.setUsuarioCarga(actual);
            c = comprobanteRepository.save(c);
            auditoria.registrar(actual, "comprobantes", c.getIdComprobante(), "CREACION",
                    "Soporte para movimiento " + idMovimiento);
            return c.getIdComprobante();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar el comprobante");
        }
    }

    /** Solo el administrador o quien registró el gasto puede ver el soporte (RN-10, RF-08). */
    public Comprobante consultar(Usuario actual, Long idComprobante) {
        Comprobante c = comprobanteRepository.findById(idComprobante)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Comprobante no encontrado"));
        puedeVer(actual, c.getMovimiento());
        return c;
    }

    public Resource recurso(Comprobante c) {
        return new FileSystemResource(Paths.get(c.getRutaArchivo()));
    }

    private void puedeVer(Usuario actual, MovimientoFinanciero m) {
        boolean esAdmin = "ADMIN".equals(actual.getRol());
        boolean esDueno = m.getUsuarioRegistra().getIdUsuario().equals(actual.getIdUsuario());
        if (!esAdmin && !esDueno) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No puede acceder a este soporte");
        }
    }
}
