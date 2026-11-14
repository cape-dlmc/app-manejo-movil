package com.fleetcontrol.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Contratos de entrada y salida de la API. Nunca se expone una entidad directamente. */
public final class Dtos {
    private Dtos() {}

    // ---------- Autenticación ----------
    public record LoginRequest(@NotBlank @Email String correo, @NotBlank String password) {}
    public record LoginResponse(String token, String nombre, String correo, String rol) {}

    // ---------- Vehículos ----------
    public record VehiculoRequest(
            @NotBlank @Size(max = 10) String placa,
            @NotBlank @Size(max = 50) String marca,
            @NotBlank @Size(max = 50) String modelo,
            @NotNull @Min(1990) @Max(2100) Integer anio,
            @NotBlank String estado,
            String observaciones) {}
    public record VehiculoResponse(Long id, String placa, String marca, String modelo,
                                   Integer anio, String estado, String observaciones) {}

    // ---------- Conductores ----------
    public record ConductorRequest(
            @NotBlank String nombre,
            @NotBlank @Email String correo,
            @NotBlank @Size(min = 8) String passwordTemporal,
            @NotBlank String identificacion,
            String telefono) {}
    public record ConductorUpdateRequest(
            @NotBlank String nombre,
            @NotBlank @Email String correo,
            @NotBlank String identificacion,
            String telefono,
            @Size(min = 8) String passwordTemporal) {}
    public record ConductorResponse(Long id, Long idUsuario, String nombre, String correo,
                                    String identificacion, String telefono, String estado) {}

    // ---------- Viajes ----------
    public record ViajeRequest(
            @NotBlank String codigo,
            @NotNull Long vehiculoId,
            @NotNull Long conductorId,
            @NotBlank String tipoServicio,
            @NotBlank String origen,
            @NotBlank String destino,
            @NotNull LocalDateTime fechaSalida,
            @NotNull @Positive BigDecimal valorCobrado,
            String observaciones) {}
    public record ViajeResponse(Long id, String codigo, String tipoServicio, String origen,
                                String destino, LocalDateTime fechaSalida, BigDecimal valorCobrado,
                                String estado, Long vehiculoId, String placa,
                                Long conductorId, String conductor) {}
    public record EstadoRequest(@NotBlank String estado) {}

    // ---------- Movimientos (gastos) ----------
    public record GastoRequest(
            @NotNull Long categoriaId,
            @NotNull Long vehiculoId,
            Long viajeId,
            @NotNull @Positive BigDecimal valor,
            @NotBlank String descripcion,
            LocalDateTime fechaMovimiento,
            Boolean pagadoConRecaudo) {}
    public record MovimientoResponse(Long id, String categoria, Long vehiculoId, String placa,
                                     Long viajeId, String codigoViaje, String registradoPor,
                                     BigDecimal valor, String descripcion, LocalDateTime fecha,
                                     String estado, String observacionRevision,
                                     List<Long> comprobanteIds, boolean pagadoConRecaudo) {}
    public record RevisionRequest(@NotBlank String decision, String observacion) {}
    public record CategoriaResponse(Long id, String nombre) {}

    // ---------- Entregas de dinero ----------
    public record EntregaRequest(
            Long conductorId,
            Long viajeId,
            @NotNull @Positive BigDecimal valorEntregado,
            String referencia,
            String observaciones,
            LocalDateTime fechaEntrega) {}
    public record EntregaResponse(Long id, Long conductorId, String conductor, Long viajeId,
                                  String codigoViaje, LocalDateTime fecha, BigDecimal valorEntregado,
                                  String referencia, String observaciones, String estado,
                                  String recibidoPor) {}

    // ---------- Conciliación y dashboard ----------
    public record ConciliacionViaje(Long viajeId, String codigo, String conductor, String estadoViaje,
                                    BigDecimal ingreso, BigDecimal gastosAprobados,
                                    BigDecimal esperado, BigDecimal entregado, BigDecimal diferencia,
                                    BigDecimal gastosPagadosEmpresa) {}
    public record DashboardResponse(long vehiculosActivos, long viajesCompletados,
                                    BigDecimal ingresos, BigDecimal egresosAprobados,
                                    BigDecimal pendienteConciliar, long gastosPendientes) {}

    public record ReporteGrupo(String nombre, BigDecimal ingresos, BigDecimal gastosReportados,
                               BigDecimal gastosAprobados, BigDecimal entregas,
                               BigDecimal entregasPendientes, BigDecimal saldo) {}
    public record ReporteResponse(LocalDate desde, LocalDate hasta, long viajesCompletados,
                                  BigDecimal ingresos, BigDecimal gastosReportados,
                                  BigDecimal gastosAprobados, BigDecimal gastosPendientes,
                                  BigDecimal dineroEntregado, BigDecimal entregasPendientes,
                                  BigDecimal saldoPendiente,
                                  List<ReporteGrupo> porVehiculo, List<ReporteGrupo> porConductor,
                                  List<ReporteGrupo> porCategoria) {}
}
