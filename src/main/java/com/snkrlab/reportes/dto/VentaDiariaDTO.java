package com.snkrlab.reportes.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VentaDiariaDTO {

    private LocalDate fecha;
    private long cantidadOrdenes;
    private long total;
}