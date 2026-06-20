package ru.muwa.dto;

import lombok.Data;

@Data
public class LoginRequest {

    private Long telegramId;
    private String telegramUsername;

}
