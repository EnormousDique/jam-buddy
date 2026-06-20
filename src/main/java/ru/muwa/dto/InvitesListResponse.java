package ru.muwa.dto;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import ru.muwa.entity.Invite;

import java.util.List;

@Data
@Builder
@Setter
@Getter
public class InvitesListResponse {
    private List<InviteDto> list;
}
