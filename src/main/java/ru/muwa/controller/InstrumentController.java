package ru.muwa.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.muwa.dto.InstrumentResponse;
import ru.muwa.entity.Instrument;
import ru.muwa.repository.InstrumentRepository;

@RestController
@RequestMapping("/profiles/instruments")
@RequiredArgsConstructor
public class InstrumentController {

    private final InstrumentRepository repository;

    @GetMapping("")
    public InstrumentResponse getInstruments(){

        return InstrumentResponse
                .builder()
                .instruments(repository.findAll())
                .build();
    }

}
