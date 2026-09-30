package com.snkrlab.reportes.rabbitmq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VentaRegistradaMensaje {

    private Long ordenId;
    private LocalDateTime fecha;
    private Integer total;
    private List<DetalleVentaMensaje> items;
}