package com.snkrlab.reportes.controller;

import com.snkrlab.reportes.dto.ExportacionDTO;
import com.snkrlab.reportes.model.Exportacion;
import com.snkrlab.reportes.rabbitmq.RabbitMQConfig;
import com.snkrlab.reportes.service.ExportacionService;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reportes/exportaciones")
public class ExportacionController {

    private final ExportacionService exportacionService;
    private final RabbitTemplate rabbitTemplate;

    public ExportacionController(
            ExportacionService exportacionService,
            RabbitTemplate rabbitTemplate
    ) {
        this.exportacionService = exportacionService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostMapping
    public ResponseEntity<ExportacionDTO> solicitar(
            @AuthenticationPrincipal Jwt jwt,

            @RequestParam(name = "tipo", defaultValue = "VENTAS")
            Exportacion.Tipo tipo,

            @RequestParam(name = "desde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate desde,

            @RequestParam(name = "hasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate hasta
    ) {
        Exportacion exportacion = exportacionService.crear(
                usuarioId(jwt),
                tipo,
                desde,
                hasta
        );

        try {
            rabbitTemplate.convertAndSend(
                    "",
                    RabbitMQConfig.COLA_EXPORTACIONES,
                    exportacion.getId(),
                    mensaje -> {
                        mensaje.getMessageProperties().setDeliveryMode(
                                MessageDeliveryMode.PERSISTENT
                        );

                        return mensaje;
                    }
            );
        } catch (AmqpException e) {
            exportacionService.marcarError(exportacion.getId());

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo encolar la exportación. Intenta nuevamente.",
                    e
            );
        }

        return ResponseEntity.accepted()
                .body(convertirDTO(exportacion));
    }

    @GetMapping("/{id}")
    public ExportacionDTO estado(
            @PathVariable("id") String id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Exportacion exportacion = exportacionService.obtener(
                id,
                usuarioId(jwt)
        );

        return convertirDTO(exportacion);
    }

    @GetMapping("/{id}/archivo")
    public ResponseEntity<byte[]> descargar(
            @PathVariable("id") String id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Exportacion exportacion = exportacionService.obtener(
                id,
                usuarioId(jwt)
        );

        if (exportacion.getEstado() != Exportacion.Estado.LISTO) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El archivo todavía no está disponible."
            );
        }

        byte[] contenido = exportacion.getContenido()
                .getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .contentType(
                        MediaType.parseMediaType("text/csv;charset=UTF-8")
                )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(exportacion.getNombreArchivo())
                                .build()
                                .toString()
                )
                .cacheControl(CacheControl.noStore())
                .contentLength(contenido.length)
                .body(contenido);
    }

    private ExportacionDTO convertirDTO(Exportacion exportacion) {
        return new ExportacionDTO(
                exportacion.getId(),
                exportacion.getTipo(),
                exportacion.getEstado(),
                exportacion.getDesde(),
                exportacion.getHasta(),
                exportacion.getCreada(),
                exportacion.getFinalizada(),
                exportacion.getNombreArchivo(),
                exportacion.getError()
        );
    }

    private String usuarioId(Jwt jwt) {
        String oid = jwt.getClaimAsString("oid");

        if (oid == null || oid.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "El token no contiene el identificador del usuario."
            );
        }

        return oid;
    }

    @GetMapping("/ultima")
    public ResponseEntity<ExportacionDTO> ultima(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Exportacion exportacion = exportacionService.obtenerUltima(
                usuarioId(jwt)
        );

        if (exportacion == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(convertirDTO(exportacion));
    }
}