package ru.muwa.dto;

import lombok.Data;

@Data
public class RegisterRequest {

    private Long telegramId;
    private String telegramUsername;

}
