package com.time_tracker.be.config;

import com.midtrans.Config;
import com.midtrans.ConfigFactory;
import com.midtrans.service.MidtransSnapApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class MidtransConfig {
    @Value("${midtrans.server-key}")
    private String serverKey;
    @Value("${midtrans.client-key}")
    private String clientKey;
    @Value("${midtrans.is-production}")
    private boolean isProduction;


    @Bean
    public MidtransSnapApi snapApi() {
        Config config = Config.builder()
                .setServerKey(serverKey)
                .setClientKey(clientKey)
                .setIsProduction(isProduction)
                .build();
        return new ConfigFactory(config).getSnapApi();
    }
}