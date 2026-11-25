package rybakov.vladislav.tradingappspvb.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rybakov.vladislav.tradingappspvb.entity.Instrument;

import java.util.Optional;

public interface InstrumentRepository
        extends JpaRepository<Instrument, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT i FROM Instrument i
            WHERE i.seccode = :seccode
            """)
    Optional<Instrument> findBySeccodeForUpdate(
            @Param("seccode") String seccode
    );
}