package com.time_tracker.be.lib;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class RabbitmqService {

    private final ConnectionFactory factory;
    private Connection connection;
    private Channel channel;

    public RabbitmqService(ConnectionFactory factory) throws Exception {
        this.factory = factory;
        init();
    }

    private void init() throws Exception {
        this.connection = factory.newConnection();
        this.channel = connection.createChannel();
    }

    // durable = apakah queue bertahan saat RabbitMQ restart
    // exclusive = apakah hanya 1 client yang bisa mengakses queue ini
    // autoDelete = apakah queue dihapus saat tidak ada client yang mengakses
    // headers = properti tambahan dalam bentuk key-value
    // ruting key = nama queue
    // exchange = tempat pengiriman pesan, bisa diisi "" untuk default exchange (Direct) kalo default exchange, routing key harus sama dengan nama queue tujuannya, kalo bukan default exchange, routing key bisa diisi sesuai kebutuhan (Direct, Fanout, Topic, Headers)
    // properties = properti pesan, bisa diisi null
    // message = isi pesan
    public void sendMessage(String routingKey, String exchange, AMQP.BasicProperties properties, String message, Boolean durable, boolean exclusive, boolean autoDelete, Map<String, Object> headers) throws Exception {
        channel.queueDeclare(routingKey, durable, exclusive, autoDelete, headers);
        channel.basicPublish(exchange, routingKey, properties, message.getBytes());
        System.out.println(" [x] Sent '" + message + "' to queue: " + routingKey);
    }

    @PreDestroy
    public void close() throws Exception {
        if (channel != null) channel.close();
        if (connection != null) connection.close();
    }
}