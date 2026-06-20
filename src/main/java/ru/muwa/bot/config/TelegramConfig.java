package ru.muwa.bot.config;

import jakarta.annotation.PostConstruct;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Configuration
public class TelegramConfig {

    @Value("${telegram.bot.token}")
    private String token;

    @Bean
    public OkHttpClient okHttpClient() {

        return new OkHttpClient.Builder()
                .build();
    }

    @Bean
    public TelegramClient telegramClient(OkHttpClient okHttpClient) {
        return new OkHttpTelegramClient(okHttpClient, token);
    }

}
