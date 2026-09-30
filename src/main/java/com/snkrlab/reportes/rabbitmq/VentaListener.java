package com.snkrlab.reportes.rabbitmq;

import com.snkrlab.reportes.service.ReporteService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class VentaListener {

    private final ReporteService reporteService;

    public VentaListener(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @RabbitListener(queues = RabbitMQConfig.COLA_VENTAS)
    public void recibirVenta(VentaRegistradaMensaje mensaje) {
        reporteService.registrarVenta(mensaje);
    }
}