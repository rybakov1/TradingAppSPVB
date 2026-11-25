package rybakov.vladislav.tradingappspvb.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rybakov.vladislav.tradingappspvb.dto.request.CreateInstrumentRequest;
import rybakov.vladislav.tradingappspvb.dto.response.InstrumentResponse;
import rybakov.vladislav.tradingappspvb.entity.Instrument;
import rybakov.vladislav.tradingappspvb.exception.ConflictException;
import rybakov.vladislav.tradingappspvb.exception.ResourceNotFoundException;
import rybakov.vladislav.tradingappspvb.repository.InstrumentRepository;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InstrumentService {

    private final InstrumentRepository instrumentRepository;

    @Transactional
    public InstrumentResponse create(CreateInstrumentRequest request) {

        String seccode = normalizeSeccode(request.seccode());

        if (instrumentRepository.existsById(seccode)) {
            throw new ConflictException(
                    "Instrument already exists: " + seccode
            );
        }

        Instrument instrument = new Instrument();

        instrument.setSeccode(seccode);
        instrument.setType(request.type());
        instrument.setLotSize(request.lotSize());
        instrument.setMinStep(request.minStep());
        instrument.setCurrency(request.currency());

        Instrument saved = instrumentRepository.save(instrument);

        return toResponse(saved);
    }

    public InstrumentResponse getBySeccode(String seccode) {
        return toResponse(findInstrument(seccode));
    }

    public List<InstrumentResponse> getAll() {
        return instrumentRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public Instrument findInstrument(String seccode) {

        String normalized = normalizeSeccode(seccode);

        return instrumentRepository.findById(normalized)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Instrument not found: " + normalized
                ));
    }

    private String normalizeSeccode(String seccode) {
        return seccode.trim().toUpperCase(Locale.ROOT);
    }

    private InstrumentResponse toResponse(Instrument instrument) {
        return new InstrumentResponse(
                instrument.getSeccode(),
                instrument.getType(),
                instrument.getLotSize(),
                instrument.getMinStep(),
                instrument.getCurrency()
        );
    }
}