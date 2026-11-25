package rybakov.vladislav.tradingappspvb.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rybakov.vladislav.tradingappspvb.entity.Trade;

import java.util.List;
import java.util.Optional;

public interface TradeRepository
        extends JpaRepository<Trade, Long> {

    List<Trade> findAllByInstrument_SeccodeOrderByExecutedAtDesc(
            String seccode
    );

    Optional<Trade>
    findFirstByInstrument_SeccodeOrderByExecutedAtDescIdDesc(
            String seccode
    );

    List<Trade> findAllByOrderByExecutedAtDescIdDesc();
}