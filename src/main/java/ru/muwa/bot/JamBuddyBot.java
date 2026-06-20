package ru.muwa.bot;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
@Getter
public class JamBuddyBot implements LongPollingSingleThreadUpdateConsumer {

    @Value("${telegram.bot.token}")
    private String botToken;

    private final TelegramClient telegramClient;

    @PostConstruct
    private void register(){

        try {

            TelegramBotsLongPollingApplication application =
                    new TelegramBotsLongPollingApplication();

            application.registerBot(botToken, this);
            log.info("Бот успешно запущен.");

        } catch (Exception e){

            log.error("Ошибка при регистрации бота в telegram API.");
            log.error(e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void consume(Update update) {

        log.info("Update получен!");

        if (!update.hasMessage()) return;

        var message = update.getMessage();
        long chatId = message.getChatId();
        int messageId = message.getMessageId();

        // Удаляем входящие сообщения
        try{
            telegramClient
                    .execute(DeleteMessage
                    .builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .build());
        } catch (TelegramApiException e){
            log.error("Ошибка telegram API при удалении сообщения",e);
        }

        // Отправляем сообщение с кнопкой miniApp
        InlineKeyboardButton appButton =
                        InlineKeyboardButton
                            .builder()
                            .text("Открыть приложение")
                            .webApp(new WebAppInfo("https://jambuddy.ru/index.html"))
                            .build();

        InlineKeyboardMarkup markup =
                        InlineKeyboardMarkup
                                .builder()
                                .keyboardRow(new InlineKeyboardRow(List.of(appButton)))
                                .build();

        SendMessage send =
                    SendMessage
                        .builder()
                        .chatId(chatId)
                        .text("Привет!")
                        .replyMarkup(markup)
                        .build();

        try {
            telegramClient.execute(send);
        } catch (TelegramApiException e) {
            log.error("Ошибка telegram API при отправке сообщения",e);
        }
    }

}
