package ru.muwa.dto;

import lombok.Builder;
import lombok.Data;
import ru.muwa.entity.Instrument;

import java.util.List;


@Data
@Builder
public class InstrumentResponse {
        private List<Instrument> instruments;
}
