package rybakov.vladislav.tradingappspvb.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rybakov.vladislav.tradingappspvb.dto.response.TradeResponse;
import rybakov.vladislav.tradingappspvb.entity.Instrument;
import rybakov.vladislav.tradingappspvb.entity.Trade;
import rybakov.vladislav.tradingappspvb.exception.ResourceNotFoundException;
import rybakov.vladislav.tradingappspvb.repository.TradeRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TradeService {

    private final TradeRepository tradeRepository;
    private final InstrumentService instrumentService;

    public List<TradeResponse> getAll() {

        return tradeRepository
                .findAllByOrderByExecutedAtDescIdDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public TradeResponse getById(Long id) {

        Trade trade = tradeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Trade not found: " + id
                ));

        return toResponse(trade);
    }

    public List<TradeResponse> getBySeccode(String seccode) {

        Instrument instrument =
                instrumentService.findInstrument(seccode);

        return tradeRepository
                .findAllByInstrument_SeccodeOrderByExecutedAtDescIdDesc(
                        instrument.getSeccode()
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private TradeResponse toResponse(Trade trade) {

        return new TradeResponse(
                trade.getId(),
                trade.getInstrument().getSeccode(),
                trade.getBuyOrder().getId(),
                trade.getSellOrder().getId(),
                trade.getPrice(),
                trade.getQuantity(),
                trade.getExecutedAt()
        );
    }
}