package com.snkrlab.reportes.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumenReporteDTO {

    private long totalVentas;
    private long cantidadOrdenes;
    private long unidadesVendidas;

    private List<VentaDiariaDTO> ventasPorDia;
    private List<ProductoVendidoDTO> productosMasVendidos;
}