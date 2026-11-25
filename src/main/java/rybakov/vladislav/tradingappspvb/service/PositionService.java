package rybakov.vladislav.tradingappspvb.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rybakov.vladislav.tradingappspvb.dto.request.CreatePositionRequest;
import rybakov.vladislav.tradingappspvb.dto.response.PositionResponse;
import rybakov.vladislav.tradingappspvb.entity.Account;
import rybakov.vladislav.tradingappspvb.entity.Instrument;
import rybakov.vladislav.tradingappspvb.entity.Position;
import rybakov.vladislav.tradingappspvb.exception.BusinessException;
import rybakov.vladislav.tradingappspvb.exception.ConflictException;
import rybakov.vladislav.tradingappspvb.exception.ResourceNotFoundException;
import rybakov.vladislav.tradingappspvb.repository.PositionRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PositionService {

    private final PositionRepository positionRepository;
    private final AccountService accountService;
    private final InstrumentService instrumentService;

    @Transactional
    public PositionResponse create(
            Long accountId,
            CreatePositionRequest request
    ) {
        Account account = accountService.findAccount(accountId);

        Instrument instrument = instrumentService.findInstrument(
                request.seccode()
        );

        if (account.getCurrency() != instrument.getCurrency()) {
            throw new BusinessException(
                    "Account currency does not match instrument currency"
            );
        }

        if (positionRepository
                .findByAccount_IdAndInstrument_Seccode(
                        accountId,
                        instrument.getSeccode()
                ).isPresent()) {

            throw new ConflictException(
                    "Position already exists for instrument: "
                            + instrument.getSeccode()
            );
        }

        Position position = new Position();

        position.setAccount(account);
        position.setInstrument(instrument);
        position.setQuantity(request.quantity());
        position.setReservedQuantity(0L);

        return toResponse(positionRepository.save(position));
    }

    public List<PositionResponse> getByAccountId(Long accountId) {

        accountService.findAccount(accountId);

        return positionRepository.findAllByAccount_Id(accountId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public PositionResponse getByAccountIdAndSeccode(
            Long accountId,
            String seccode
    ) {
        return toResponse(findPosition(accountId, seccode));
    }

    public Position findPosition(Long accountId, String seccode) {

        Instrument instrument = instrumentService.findInstrument(seccode);

        return positionRepository
                .findByAccount_IdAndInstrument_Seccode(
                        accountId,
                        instrument.getSeccode()
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Position not found for account "
                                + accountId
                                + " and instrument "
                                + instrument.getSeccode()
                ));
    }

    private PositionResponse toResponse(Position position) {
        return new PositionResponse(
                position.getId(),
                position.getAccount().getId(),
                position.getInstrument().getSeccode(),
                position.getQuantity(),
                position.getReservedQuantity(),
                position.getAvailableQuantity()
        );
    }
}