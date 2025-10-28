package com.wallet_service.be.health;

import com.rabbitmq.client.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthRoute {

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;
    private final ConnectionFactory rabbitConnectionFactory;
    private final RedisConnectionFactory redisConnectionFactory;
    private final String rabbitHost;

    public HealthRoute(
            JdbcTemplate jdbcTemplate,
            StringRedisTemplate redisTemplate,
            ConnectionFactory rabbitConnectionFactory,
            RedisConnectionFactory redisConnectionFactory,
            @Value("${spring.rabbitmq.host:}") String rabbitHost
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
        this.rabbitConnectionFactory = rabbitConnectionFactory;
        this.redisConnectionFactory = redisConnectionFactory;
        this.rabbitHost = rabbitHost;
    }

    @GetMapping("/health")
    public Map<String, Object> healthCheck() {
        Map<String, Object> status = new HashMap<>();

        // 🔹 Cek Database
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            status.put("database", "UP");
        } catch (Exception e) {
            status.put("database", "DOWN: " + e.getMessage());
        }

        // 🔹 Cek Redis
        try (var conn = redisConnectionFactory.getConnection()) {
            conn.ping();
            status.put("redis", "UP");
        } catch (Exception e) {
            status.put("redis", "DOWN: " + e.getMessage());
        }

        // 🔹 Cek RabbitMQ
        try (var connection = rabbitConnectionFactory.newConnection()) {
            if (connection != null && connection.isOpen()) {
                status.put("rabbitmq", "UP (" + rabbitHost + ")");
            } else {
                status.put("rabbitmq", "DOWN: Not connected");
            }
        } catch (Exception e) {
            status.put("rabbitmq", "DOWN: " + e.getMessage());
        }

        // 🔹 Kesimpulan Akhir
        boolean allUp = status.values().stream().allMatch(v -> v.toString().startsWith("UP"));
        status.put("status", allUp ? "UP" : "DEGRADED");

        return status;
    }
}
