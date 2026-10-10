package com.snkrlab.reportes.rabbitmq;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String COLA_VENTAS = "reportes.ventas.queue";

    public static final String COLA_EXPORTACIONES = "reportes.exportaciones.queue";

    @Bean
    public Queue ventasQueue() {
        return new Queue(COLA_VENTAS, true);
    }

    @Bean
    public Queue exportacionesQueue() {
        return new Queue(COLA_EXPORTACIONES, true);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}