package com.fleetcontrol.service;

import com.fleetcontrol.dto.Dtos.ReporteGrupo;
import com.fleetcontrol.dto.Dtos.ReporteResponse;
import com.fleetcontrol.entity.EntregaDinero;
import com.fleetcontrol.entity.MovimientoFinanciero;
import com.fleetcontrol.entity.Viaje;
import com.fleetcontrol.exception.ApiException;
import com.fleetcontrol.repository.EntregaRepository;
import com.fleetcontrol.repository.MovimientoRepository;
import com.fleetcontrol.repository.ViajeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Reportes por periodo (RF-13, módulo 8).
 * Saldo pendiente de conciliación = ingresos − gastos aprobados pagados con dinero del viaje − entregas verificadas.
 * Los gastos pagados por la empresa no se descuentan (sección 12.2).
 */
@Service
@RequiredArgsConstructor
public class ReporteService {

    private static final BigDecimal CERO = BigDecimal.ZERO;

    private final ViajeRepository viajeRepository;
    private final MovimientoRepository movimientoRepository;
    private final EntregaRepository entregaRepository;

    public ReporteResponse generar(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La fecha inicial no puede ser posterior a la fecha final");
        }
        LocalDateTime inicio = desde != null ? desde.atStartOfDay() : null;
        LocalDateTime finExclusivo = hasta != null ? hasta.plusDays(1).atStartOfDay() : null;

        Map<Long, Viaje> viajesPorId = new HashMap<>();
        Map<Long, Grupo> vehiculos = new TreeMap<>();
        Map<Long, Grupo> conductores = new TreeMap<>();
        Map<String, Grupo> categorias = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        BigDecimal ingresos = CERO;
        long viajesCompletados = 0;
        for (Viaje viaje : viajeRepository.findAll()) {
            viajesPorId.put(viaje.getIdViaje(), viaje);
            if (!"COMPLETADO".equals(viaje.getEstado()) || !incluye(viaje.getFechaSalida(), inicio, finExclusivo)) {
                continue;
            }
            viajesCompletados++;
            ingresos = ingresos.add(viaje.getValorCobrado());
            Grupo vehiculo = grupo(vehiculos, viaje.getVehiculo().getIdVehiculo(), viaje.getVehiculo().getPlaca());
            vehiculo.ingresos = vehiculo.ingresos.add(viaje.getValorCobrado());
            var usuario = viaje.getConductor().getUsuario();
            Grupo conductor = grupo(conductores, usuario.getIdUsuario(), usuario.getNombre());
            conductor.ingresos = conductor.ingresos.add(viaje.getValorCobrado());
        }

        BigDecimal gastosReportados = CERO;
        BigDecimal gastosAprobados = CERO;
        BigDecimal gastosPendientes = CERO;
        BigDecimal descontables = CERO;
        for (MovimientoFinanciero m : movimientoRepository.findAll()) {
            if (!"EGRESO".equals(m.getTipo()) || !incluye(m.getFechaMovimiento(), inicio, finExclusivo)) {
                continue;
            }
            BigDecimal valor = m.getValor();
            boolean aprobado = "APROBADO".equals(m.getEstadoRevision());
            boolean descontable = aprobado && m.descontableDeEntrega();
            gastosReportados = gastosReportados.add(valor);
            if (aprobado) gastosAprobados = gastosAprobados.add(valor);
            if ("PENDIENTE".equals(m.getEstadoRevision())) gastosPendientes = gastosPendientes.add(valor);
            if (descontable) descontables = descontables.add(valor);

            registrarGasto(grupo(vehiculos, m.getVehiculo().getIdVehiculo(), m.getVehiculo().getPlaca()), valor, aprobado, descontable);
            var registra = m.getUsuarioRegistra();
            registrarGasto(grupo(conductores, registra.getIdUsuario(), registra.getNombre()), valor, aprobado, descontable);
            registrarGasto(categorias.computeIfAbsent(m.getCategoria().getNombre(), Grupo::new), valor, aprobado, descontable);
        }

        BigDecimal entregas = CERO;
        BigDecimal entregasPendientes = CERO;
        for (EntregaDinero e : entregaRepository.findAll()) {
            if (!incluye(e.getFechaEntrega(), inicio, finExclusivo)) continue;
            BigDecimal valor = e.getValorEntregado();
            boolean verificada = "VERIFICADA".equals(e.getEstado());
            if (verificada) entregas = entregas.add(valor);
            else entregasPendientes = entregasPendientes.add(valor);

            var usuario = e.getConductor().getUsuario();
            registrarEntrega(grupo(conductores, usuario.getIdUsuario(), usuario.getNombre()), valor, verificada);
            if (e.getViaje() != null) {
                Viaje viaje = viajesPorId.get(e.getViaje().getIdViaje());
                if (viaje != null) {
                    registrarEntrega(grupo(vehiculos, viaje.getVehiculo().getIdVehiculo(), viaje.getVehiculo().getPlaca()),
                            valor, verificada);
                }
            }
        }

        return new ReporteResponse(desde, hasta, viajesCompletados, ingresos, gastosReportados,
                gastosAprobados, gastosPendientes, entregas, entregasPendientes,
                ingresos.subtract(descontables).subtract(entregas),
                aDto(vehiculos), aDto(conductores), categorias.values().stream().map(Grupo::aDto).toList());
    }

    private static boolean incluye(LocalDateTime fecha, LocalDateTime inicio, LocalDateTime finExclusivo) {
        return (inicio == null || !fecha.isBefore(inicio)) && (finExclusivo == null || fecha.isBefore(finExclusivo));
    }

    private static <K> Grupo grupo(Map<K, Grupo> grupos, K clave, String nombre) {
        return grupos.computeIfAbsent(clave, ignorada -> new Grupo(nombre));
    }

    private static void registrarGasto(Grupo g, BigDecimal valor, boolean aprobado, boolean descontable) {
        g.gastosReportados = g.gastosReportados.add(valor);
        if (aprobado) g.gastosAprobados = g.gastosAprobados.add(valor);
        if (descontable) g.descontables = g.descontables.add(valor);
    }

    private static void registrarEntrega(Grupo g, BigDecimal valor, boolean verificada) {
        if (verificada) g.entregas = g.entregas.add(valor);
        else g.entregasPendientes = g.entregasPendientes.add(valor);
    }

    private static List<ReporteGrupo> aDto(Map<?, Grupo> grupos) {
        return grupos.values().stream().map(Grupo::aDto).toList();
    }

    private static final class Grupo {
        private final String nombre;
        private BigDecimal ingresos = CERO;
        private BigDecimal gastosReportados = CERO;
        private BigDecimal gastosAprobados = CERO;
        private BigDecimal descontables = CERO;
        private BigDecimal entregas = CERO;
        private BigDecimal entregasPendientes = CERO;

        private Grupo(String nombre) {
            this.nombre = nombre;
        }

        private ReporteGrupo aDto() {
            return new ReporteGrupo(nombre, ingresos, gastosReportados, gastosAprobados,
                    entregas, entregasPendientes, ingresos.subtract(descontables).subtract(entregas));
        }
    }
}
