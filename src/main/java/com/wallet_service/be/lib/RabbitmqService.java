package com.wallet_service.be.lib;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.wallet_service.be.utils.enums.ExchangeType;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
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
    // routing key = nama queue tujuan (pada exchange type direct dan topic, routing key harus sesuai dengan nama queue tujuan, pada exchange type fanout dan headers, routing key bisa diisi sembarang)
    // exchange = tempat pengiriman pesan, bisa diisi "" untuk default exchange (Direct) kalo default exchange, routing key harus sama dengan nama queue tujuannya, kalo bukan default exchange, routing key bisa diisi sesuai kebutuhan (Direct, Fanout, Topic, Headers)
    // exchangeType = tipe exchange, bisa diisi "direct", "fanout", "topic", "headers"
    // type direct = pesan dikirim ke queue sesuai routing key
    // type fanout = pesan dikirim ke semua queue yang terhubung ke exchange ini
    // type topic = pesan dikirim ke queue sesuai pola routing key (misal: "user.*" akan mengirim pesan ke queue dengan routing key "user.create", "user.delete", dll)
    // type headers = pesan dikirim ke queue sesuai header yang ditentukan
    // properties = properti pesan, bisa diisi null
    // message = isi pesan
    public void sendMessage(
            String queueName,
            String routingKey,
            String exchange,
            ExchangeType exchangeType,
            AMQP.BasicProperties properties,
            String message,
            boolean durable,
            boolean exclusive,
            boolean autoDelete,
            Map<String, Object> headers
    ) throws Exception {
        // 🔹 Declare exchange lengkap
        // Argumen: name, type, durable, autoDelete, internal, arguments
        channel.exchangeDeclare(exchange, exchangeType.getValue(), durable, autoDelete, false, headers);

        switch (exchangeType) {
            case FANOUT:
            case HEADERS:
                // FANOUT/HEADERS: tidak perlu queue dari sisi producer
                channel.basicPublish(exchange, "", properties, message.getBytes());
                break;

            case DIRECT:
            case TOPIC:
                // DIRECT/TOPIC: deklarasi queue dan bind ke exchange
                channel.queueDeclare(queueName, durable, exclusive, autoDelete, headers);
                channel.queueBind(queueName, exchange, routingKey);
                channel.basicPublish(exchange, routingKey, properties, message.getBytes());
                break;
        }

        log.info(" [x] Sent '{}' to exchange '{}' (type='{}', queue='{}', routingKey='{}')",
                message, exchange, exchangeType.getValue(), queueName, routingKey);
    }

    public void sendToExchange(
            String exchange,
            ExchangeType exchangeType,
            String routingKey,
            String message,
            boolean durable,
            boolean autoDelete,
            AMQP.BasicProperties properties
    ) throws Exception {
        channel.exchangeDeclare(exchange, exchangeType.getValue(), durable, autoDelete, false, null);
        channel.basicPublish(exchange, routingKey, properties, message.getBytes());
        log.info("Published to exchange '{}' with routingKey='{}': {}", exchange, routingKey, message);
    }


    @PreDestroy
    public void close() throws Exception {
        if (channel != null) channel.close();
        if (connection != null) connection.close();
    }
}