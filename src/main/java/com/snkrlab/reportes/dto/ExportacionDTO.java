package com.snkrlab.reportes.dto;

import com.snkrlab.reportes.model.Exportacion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportacionDTO {

    private String id;
    private Exportacion.Tipo tipo;
    private Exportacion.Estado estado;
    private LocalDate desde;
    private LocalDate hasta;
    private LocalDateTime creada;
    private LocalDateTime finalizada;
    private String nombreArchivo;
    private String error;
}