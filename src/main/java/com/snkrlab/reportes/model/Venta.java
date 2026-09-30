package com.snkrlab.reportes.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ventas")
@Data
@NoArgsConstructor
public class Venta {

    @Id
    private Long ordenId;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false)
    private Integer total;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "venta_detalles",
            joinColumns = @JoinColumn(name = "orden_id")
    )
    @OrderColumn(name = "posicion")
    private List<DetalleVenta> items = new ArrayList<>();
}