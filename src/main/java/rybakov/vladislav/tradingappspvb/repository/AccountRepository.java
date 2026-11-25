package rybakov.vladislav.tradingappspvb.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rybakov.vladislav.tradingappspvb.entity.Account;

import java.util.List;

public interface AccountRepository
        extends JpaRepository<Account, Long> {

    List<Account> findAllByUserId(Long userId);
}