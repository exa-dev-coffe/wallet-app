package com.wallet_service.be.config;

import com.midtrans.Config;
import com.midtrans.ConfigFactory;
import com.midtrans.service.MidtransCoreApi;
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
    @Value("${midtrans.override-notification}")
    private String overrideNotification;

    private Config buildConfig() {
        return Config.builder()
                .setServerKey(serverKey)
                .setClientKey(clientKey)
                .setIsProduction(isProduction)
                .setPaymentOverrideNotification(overrideNotification)
                .build();
    }

    @Bean
    public MidtransCoreApi coreApi() {
        return new ConfigFactory(buildConfig()).getCoreApi();
    }

    @Bean
    public MidtransSnapApi snapApi() {
        return new ConfigFactory(buildConfig()).getSnapApi();
    }
}