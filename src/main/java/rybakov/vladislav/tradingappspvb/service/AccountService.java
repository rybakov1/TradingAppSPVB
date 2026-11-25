package rybakov.vladislav.tradingappspvb.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rybakov.vladislav.tradingappspvb.dto.request.CreateAccountRequest;
import rybakov.vladislav.tradingappspvb.dto.response.AccountResponse;
import rybakov.vladislav.tradingappspvb.entity.Account;
import rybakov.vladislav.tradingappspvb.exception.ResourceNotFoundException;
import rybakov.vladislav.tradingappspvb.repository.AccountRepository;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;

    @Transactional
    public AccountResponse create(CreateAccountRequest request) {

        Account account = new Account();

        account.setUserId(request.userId());
        account.setBalance(request.balance());
        account.setReservedBalance(BigDecimal.ZERO);
        account.setCurrency(request.currency());

        return toResponse(accountRepository.save(account));
    }

    public AccountResponse getById(Long id) {
        return toResponse(findAccount(id));
    }

    public List<AccountResponse> getAll() {
        return accountRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AccountResponse> getByUserId(Long userId) {
        return accountRepository.findAllByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public Account findAccount(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found: " + id
                ));
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getUserId(),
                account.getBalance(),
                account.getReservedBalance(),
                account.getAvailableBalance(),
                account.getCurrency()
        );
    }
}