package ru.muwa.dto;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import ru.muwa.entity.Instrument;
import ru.muwa.entity.InviteStatus;

import java.util.Set;
import java.util.UUID;

@Data
@Setter
@Getter
@Builder
public class InviteDto {

    private UUID id;

    private UUID senderId;
    private UUID receiverId;

    private int senderAge;
    private int receiverAge;

    private InviteStatus status;

    private String senderScreenName;
    private String receiverScreenName;

    private String senderTelegramUsername;
    private String receiverTelegramUsername;

    private String senderProfileDescription;
    private String receiverProfileDescription;

    private Set<Instrument> instruments;

}
