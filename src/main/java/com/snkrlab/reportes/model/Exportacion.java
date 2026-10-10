package com.snkrlab.reportes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "exportaciones")
@Getter
@Setter
@NoArgsConstructor
public class Exportacion {

    public enum Tipo {
        VENTAS,
        RESUMEN
    }

    public enum Estado {
        PENDIENTE,
        LISTO,
        ERROR
    }

    @Id
    private String id;

    @Column(nullable = false)
    private String usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Tipo tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado;

    private LocalDate desde;

    private LocalDate hasta;

    @Column(nullable = false)
    private LocalDateTime creada;

    private LocalDateTime finalizada;

    private String nombreArchivo;

    @Column(columnDefinition = "LONGTEXT")
    private String contenido;

    private String error;
}