package ru.muwa.dto;

import lombok.*;
import ru.muwa.entity.InviteStatus;

@Data
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InviteUpdateRequest {
    InviteStatus status;
}
