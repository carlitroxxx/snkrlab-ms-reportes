package com.snkrlab.reportes.repository;

import com.snkrlab.reportes.model.Exportacion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ExportacionRepository
        extends JpaRepository<Exportacion, String> {

    Optional<Exportacion> findByIdAndUsuarioId(
            String id,
            String usuarioId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Exportacion e where e.id = :id")
    Optional<Exportacion> buscarParaActualizar(
            @Param("id") String id
    );
}