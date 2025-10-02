package com.time_tracker.be.lib;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.time_tracker.be.utils.enums.ExchangeType;
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

        // Declare exchange (aman kalau dipanggil berkali2)
        channel.exchangeDeclare(exchange, exchangeType.getValue(), durable);

        switch (exchangeType) {
            case FANOUT:
                // FANOUT: kirim ke semua queue yg sudah bind, producer ga perlu declare queue
                channel.basicPublish(exchange, "", properties, message.getBytes());
                break;

            case HEADERS:
                // HEADERS: sama, producer hanya publish
                channel.basicPublish(exchange, "", properties, message.getBytes());
                break;

            case DIRECT:
            case TOPIC:
                // DIRECT/TOPIC: kalau memang 1 queue fixed, producer bisa declare
                channel.queueDeclare(queueName, durable, exclusive, autoDelete, headers);
                channel.queueBind(queueName, exchange, routingKey);
                channel.basicPublish(exchange, routingKey, properties, message.getBytes());
                break;
        }

        log.info(" [x] Sent '{}' to exchange '{}' with queue '{}' (routingKey='{}')",
                message, exchange, queueName, routingKey);
    }

    @PreDestroy
    public void close() throws Exception {
        if (channel != null) channel.close();
        if (connection != null) connection.close();
    }
}