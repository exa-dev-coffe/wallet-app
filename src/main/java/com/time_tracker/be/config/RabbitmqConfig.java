package com.time_tracker.be.config;

import com.rabbitmq.client.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitmqConfig {

    @Value("${spring.rabbitmq.host}")
    private String host;

    @Value("${spring.rabbitmq.username}")
    private String username;

    @Value("${spring.rabbitmq.password}")
    private String password;

    @Bean
    public ConnectionFactory rabbitConnectionFactory() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(this.host); // ganti sesuai host RabbitMQ-mu
        factory.setUsername(this.username);
        factory.setPassword(this.password);
        return factory;
    }
}
