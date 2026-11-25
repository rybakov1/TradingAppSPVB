package rybakov.vladislav.tradingappspvb.repository;

import org.springframework.data.jpa.repository.JpaRepository;
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
}