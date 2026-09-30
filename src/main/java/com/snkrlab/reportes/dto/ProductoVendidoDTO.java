package com.snkrlab.reportes.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoVendidoDTO {

    private Long productoId;
    private String nombreProducto;
    private long unidadesVendidas;
    private long totalVentas;
}