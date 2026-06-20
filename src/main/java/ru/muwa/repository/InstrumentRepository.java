package ru.muwa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.muwa.entity.Instrument;

import java.util.Optional;

public interface InstrumentRepository extends JpaRepository<Instrument,Integer> {

    Optional<Instrument> findByName(String name);

}
