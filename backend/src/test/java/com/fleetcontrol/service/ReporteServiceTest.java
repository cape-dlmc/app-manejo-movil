package com.fleetcontrol.service;

import com.fleetcontrol.entity.CategoriaMovimiento;
import com.fleetcontrol.entity.Conductor;
import com.fleetcontrol.entity.EntregaDinero;
import com.fleetcontrol.entity.MovimientoFinanciero;
import com.fleetcontrol.entity.Usuario;
import com.fleetcontrol.entity.Vehiculo;
import com.fleetcontrol.entity.Viaje;
import com.fleetcontrol.repository.EntregaRepository;
import com.fleetcontrol.repository.MovimientoRepository;
import com.fleetcontrol.repository.ViajeRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.lang.reflect.Proxy;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReporteServiceTest {

    @Test
    void reportaIngresosGastosPendientesYEntregasSinDescontarPendientes() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(3L);
        usuario.setNombre("Camila");
        Conductor conductor = new Conductor();
        conductor.setIdConductor(4L);
        conductor.setUsuario(usuario);
        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setIdVehiculo(5L);
        vehiculo.setPlaca("ABC123");

        Viaje viaje = new Viaje();
        viaje.setIdViaje(6L);
        viaje.setCodigo("VJ-1");
        viaje.setConductor(conductor);
        viaje.setVehiculo(vehiculo);
        viaje.setEstado("COMPLETADO");
        viaje.setFechaSalida(LocalDateTime.of(2026, 6, 10, 9, 0));
        viaje.setValorCobrado(new BigDecimal("100.00"));

        CategoriaMovimiento categoria = new CategoriaMovimiento();
        categoria.setNombre("Combustible");
        MovimientoFinanciero aprobado = movimiento(viaje, usuario, categoria, "APROBADO", "10.00");
        MovimientoFinanciero pendiente = movimiento(viaje, usuario, categoria, "PENDIENTE", "7.00");

        EntregaDinero entrega = new EntregaDinero();
        entrega.setConductor(conductor);
        entrega.setViaje(viaje);
        entrega.setFechaEntrega(LocalDateTime.of(2026, 6, 11, 12, 0));
        entrega.setValorEntregado(new BigDecimal("30.00"));
        entrega.setEstado("VERIFICADA");
        EntregaDinero entregaPendiente = new EntregaDinero();
        entregaPendiente.setConductor(conductor);
        entregaPendiente.setViaje(viaje);
        entregaPendiente.setFechaEntrega(LocalDateTime.of(2026, 6, 12, 12, 0));
        entregaPendiente.setValorEntregado(new BigDecimal("5.00"));
        entregaPendiente.setEstado("REGISTRADA");

        var reporte = servicio(List.of(viaje), List.of(aprobado, pendiente), List.of(entrega, entregaPendiente))
                .generar(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertThat(reporte.ingresos()).isEqualByComparingTo("100.00");
        assertThat(reporte.gastosReportados()).isEqualByComparingTo("17.00");
        assertThat(reporte.gastosAprobados()).isEqualByComparingTo("10.00");
        assertThat(reporte.gastosPendientes()).isEqualByComparingTo("7.00");
        assertThat(reporte.dineroEntregado()).isEqualByComparingTo("30.00");
        assertThat(reporte.entregasPendientes()).isEqualByComparingTo("5.00");
        assertThat(reporte.saldoPendiente()).isEqualByComparingTo("60.00");
        assertThat(reporte.porVehiculo()).singleElement()
                .satisfies(grupo -> assertThat(grupo.saldo()).isEqualByComparingTo("60.00"));
    }

    @Test
    void gastoPagadoPorLaEmpresaNoSeDescuentaDeLaEntrega() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(3L);
        usuario.setNombre("Camila");
        Conductor conductor = new Conductor();
        conductor.setIdConductor(4L);
        conductor.setUsuario(usuario);
        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setIdVehiculo(5L);
        vehiculo.setPlaca("ABC123");
        Viaje viaje = new Viaje();
        viaje.setIdViaje(6L);
        viaje.setConductor(conductor);
        viaje.setVehiculo(vehiculo);
        viaje.setEstado("COMPLETADO");
        viaje.setFechaSalida(LocalDateTime.of(2026, 6, 10, 9, 0));
        viaje.setValorCobrado(new BigDecimal("100.00"));

        CategoriaMovimiento categoria = new CategoriaMovimiento();
        categoria.setNombre("Mantenimiento");
        MovimientoFinanciero pagadoEmpresa = movimiento(viaje, usuario, categoria, "APROBADO", "40.00");
        pagadoEmpresa.setPagadoConRecaudo(false);

        var reporte = servicio(List.of(viaje), List.of(pagadoEmpresa), List.of())
                .generar(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertThat(reporte.gastosAprobados()).isEqualByComparingTo("40.00");
        assertThat(reporte.saldoPendiente()).isEqualByComparingTo("100.00");
    }

    @Test
    void excluyeRegistrosFueraDelPeriodo() {
        Viaje viaje = new Viaje();
        viaje.setIdViaje(9L);
        viaje.setEstado("COMPLETADO");
        viaje.setFechaSalida(LocalDateTime.of(2026, 5, 31, 23, 59));
        viaje.setValorCobrado(new BigDecimal("50.00"));
        var reporte = servicio(List.of(viaje), List.of(), List.of())
                .generar(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertThat(reporte.viajesCompletados()).isZero();
        assertThat(reporte.ingresos()).isEqualByComparingTo("0");
    }

    private static MovimientoFinanciero movimiento(Viaje viaje, Usuario usuario, CategoriaMovimiento categoria,
                                                    String estado, String valor) {
        MovimientoFinanciero movimiento = new MovimientoFinanciero();
        movimiento.setViaje(viaje);
        movimiento.setVehiculo(viaje.getVehiculo());
        movimiento.setUsuarioRegistra(usuario);
        movimiento.setCategoria(categoria);
        movimiento.setTipo("EGRESO");
        movimiento.setFechaMovimiento(LocalDateTime.of(2026, 6, 10, 10, 0));
        movimiento.setEstadoRevision(estado);
        movimiento.setValor(new BigDecimal(valor));
        return movimiento;
    }

    private static ReporteService servicio(List<Viaje> viajes, List<MovimientoFinanciero> movimientos,
                                           List<EntregaDinero> entregas) {
        return new ReporteService(repositorio(ViajeRepository.class, viajes),
                repositorio(MovimientoRepository.class, movimientos),
                repositorio(EntregaRepository.class, entregas));
    }

    private static <T> T repositorio(Class<T> tipo, List<?> registros) {
        Object proxy = Proxy.newProxyInstance(tipo.getClassLoader(), new Class<?>[]{tipo},
                (instancia, metodo, argumentos) -> metodo.getName().equals("findAll") ? registros : null);
        return tipo.cast(proxy);
    }
}
