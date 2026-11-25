package rybakov.vladislav.tradingappspvb.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rybakov.vladislav.tradingappspvb.entity.Trade;

import java.util.List;

public interface TradeRepository
        extends JpaRepository<Trade, Long> {

    List<Trade> findAllByInstrument_SeccodeOrderByExecutedAtDesc(
            String seccode
    );
}