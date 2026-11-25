package rybakov.vladislav.tradingappspvb.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rybakov.vladislav.tradingappspvb.entity.Instrument;

public interface InstrumentRepository
        extends JpaRepository<Instrument, String> {
}