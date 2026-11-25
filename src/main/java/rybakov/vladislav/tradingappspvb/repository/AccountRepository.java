package rybakov.vladislav.tradingappspvb.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rybakov.vladislav.tradingappspvb.entity.Account;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface AccountRepository
        extends JpaRepository<Account, Long> {

    List<Account> findAllByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT a FROM Account a
            WHERE a.id = :id
            """)
    Optional<Account> findByIdForUpdate(
            @Param("id") Long id
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT a FROM Account a
        WHERE a.id IN :ids
        ORDER BY a.id ASC
        """)
    List<Account> findAllByIdsForUpdate(
            @Param("ids") Collection<Long> ids
    );
}