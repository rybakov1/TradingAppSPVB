package rybakov.vladislav.tradingappspvb.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rybakov.vladislav.tradingappspvb.entity.Position;

import java.util.List;
import java.util.Optional;

public interface PositionRepository
        extends JpaRepository<Position, Long> {

    List<Position> findAllByAccount_Id(Long accountId);

    Optional<Position> findByAccount_IdAndInstrument_Seccode(
            Long accountId,
            String seccode
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p FROM Position p
            WHERE p.account.id = :accountId
            AND p.instrument.seccode = :seccode
            """)
    Optional<Position> findForUpdate(
            @Param("accountId") Long accountId,
            @Param("seccode") String seccode
    );
}