package com.snkrlab.reportes.rabbitmq;

import com.snkrlab.reportes.service.ExportacionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ExportacionListener {

    private final ExportacionService exportacionService;

    public ExportacionListener(
            ExportacionService exportacionService
    ) {
        this.exportacionService = exportacionService;
    }

    @RabbitListener(queues = RabbitMQConfig.COLA_EXPORTACIONES)
    public void recibir(String id) {
        try {
            exportacionService.generar(id);
        } catch (RuntimeException e) {
            log.error(
                    "Error generando la exportación {}",
                    id,
                    e
            );

            exportacionService.marcarError(id);
        }
    }
}