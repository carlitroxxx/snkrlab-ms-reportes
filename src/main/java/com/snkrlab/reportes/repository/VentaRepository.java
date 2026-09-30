package com.snkrlab.reportes.repository;

import com.snkrlab.reportes.model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    @Query("""
            SELECT v FROM Venta v
            WHERE (:desde IS NULL OR v.fecha >= :desde)
              AND (:hasta IS NULL OR v.fecha < :hasta)
            ORDER BY v.fecha ASC, v.ordenId ASC
            """)
    List<Venta> buscarPorFechas(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );
}