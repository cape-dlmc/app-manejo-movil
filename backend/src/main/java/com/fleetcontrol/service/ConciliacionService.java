package com.fleetcontrol.service;

import com.fleetcontrol.dto.Dtos.ConciliacionViaje;
import com.fleetcontrol.dto.Dtos.DashboardResponse;
import com.fleetcontrol.entity.EntregaDinero;
import com.fleetcontrol.entity.MovimientoFinanciero;
import com.fleetcontrol.entity.Usuario;
import com.fleetcontrol.entity.Viaje;
import com.fleetcontrol.repository.EntregaRepository;
import com.fleetcontrol.repository.MovimientoRepository;
import com.fleetcontrol.repository.VehiculoRepository;
import com.fleetcontrol.repository.ViajeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cálculos financieros del documento (sección 12):
 *   esperado   = ingreso recaudado − gastos APROBADOS del viaje   (RN-07, RN-08)
 *   diferencia = esperado − entregado                              (RN-09)
 * Positivo = queda dinero por entregar; cero = conciliado; negativo = entrega superior, revisar.
 */
@Service
@RequiredArgsConstructor
public class ConciliacionService {

    private static final BigDecimal CERO = BigDecimal.ZERO;

    private final ViajeRepository viajeRepository;
    private final MovimientoRepository movimientoRepository;
    private final EntregaRepository entregaRepository;
    private final VehiculoRepository vehiculoRepository;
    private final ConductorService conductorService;

    public List<ConciliacionViaje> porViaje(Usuario actual, boolean esAdmin) {
        List<Viaje> viajes = viajeRepository.findAllByOrderByFechaSalidaDesc();
        if (!esAdmin) {
            Long idConductor = conductorService.delUsuario(actual).getIdConductor();
            viajes = viajes.stream()
                    .filter(v -> v.getConductor().getIdConductor().equals(idConductor))
                    .toList();
        }
        return viajes.stream().map(this::conciliar).toList();
    }

    public ConciliacionViaje conciliar(Viaje v) {
        // Solo un viaje completado genera ingreso (RN-13)
        BigDecimal ingreso = "COMPLETADO".equals(v.getEstado()) ? v.getValorCobrado() : CERO;

        List<MovimientoFinanciero> aprobados = movimientoRepository.findByViajeIdViaje(v.getIdViaje()).stream()
                .filter(m -> "EGRESO".equals(m.getTipo()) && "APROBADO".equals(m.getEstadoRevision()))
                .toList();
        BigDecimal gastosAprobados = aprobados.stream()
                .filter(MovimientoFinanciero::descontableDeEntrega)
                .map(MovimientoFinanciero::getValor)
                .reduce(CERO, BigDecimal::add);
        BigDecimal gastosPagadosEmpresa = aprobados.stream()
                .filter(m -> !m.descontableDeEntrega())
                .map(MovimientoFinanciero::getValor)
                .reduce(CERO, BigDecimal::add);

        BigDecimal entregado = entregaRepository.findByViajeIdViaje(v.getIdViaje()).stream()
                .filter(e -> "VERIFICADA".equals(e.getEstado()))
                .map(EntregaDinero::getValorEntregado)
                .reduce(CERO, BigDecimal::add);

        BigDecimal esperado = ingreso.subtract(gastosAprobados);
        BigDecimal diferencia = esperado.subtract(entregado);

        return new ConciliacionViaje(v.getIdViaje(), v.getCodigo(), v.getConductor().getUsuario().getNombre(),
                v.getEstado(), ingreso, gastosAprobados, esperado, entregado, diferencia, gastosPagadosEmpresa);
    }

    /** Indicadores del dashboard del administrador (módulo 8). */
    public DashboardResponse dashboard() {
        long vehiculosActivos = vehiculoRepository.findAll().stream()
                .filter(v -> !"INACTIVO".equals(v.getEstado())).count();

        List<Viaje> viajes = viajeRepository.findAll();
        List<ConciliacionViaje> conciliaciones = viajes.stream().map(this::conciliar).toList();

        long completados = viajes.stream().filter(v -> "COMPLETADO".equals(v.getEstado())).count();
        BigDecimal ingresos = conciliaciones.stream().map(ConciliacionViaje::ingreso).reduce(CERO, BigDecimal::add);

        // Dinero pendiente: solo diferencias positivas (por entregar). Las negativas se revisan aparte.
        BigDecimal pendiente = conciliaciones.stream()
                .map(ConciliacionViaje::diferencia)
                .filter(d -> d.signum() > 0)
                .reduce(CERO, BigDecimal::add);

        BigDecimal egresosAprobados = movimientoRepository.findAll().stream()
                .filter(m -> "EGRESO".equals(m.getTipo()) && "APROBADO".equals(m.getEstadoRevision()))
                .map(MovimientoFinanciero::getValor)
                .reduce(CERO, BigDecimal::add);

        long pendientesRevision = movimientoRepository.findAll().stream()
                .filter(m -> "PENDIENTE".equals(m.getEstadoRevision())).count();

        return new DashboardResponse(vehiculosActivos, completados, ingresos, egresosAprobados,
                pendiente, pendientesRevision);
    }
}
