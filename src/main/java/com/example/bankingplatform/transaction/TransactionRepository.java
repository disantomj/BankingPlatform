package com.example.bankingplatform.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Find by reference number
    Optional<Transaction> findByTransactionReference(String transactionReference);

    // Check if reference exists
    boolean existsByTransactionReference(String transactionReference);

    // Find by user
    List<Transaction> findByUserIdOrderByCreatedAtDesc(Integer userId);

    // Find by account (either from or to)
    List<Transaction> findByFromAccountIdOrderByCreatedAtDesc(Integer fromAccountId);
    List<Transaction> findByToAccountIdOrderByCreatedAtDesc(Integer toAccountId);

    // Find by status
    List<Transaction> findByStatusOrderByCreatedAtDesc(TransactionStatus status);

    // Find by type
    List<Transaction> findByTransactionTypeOrderByCreatedAtDesc(TransactionType transactionType);

    // Find by date range
    List<Transaction> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startDate, LocalDateTime endDate);

    // Find by user and date range
    List<Transaction> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(Integer userId, LocalDateTime startDate, LocalDateTime endDate);

    // Find by status and date range
    List<Transaction> findByStatusAndCreatedAtBetweenOrderByCreatedAtDesc(TransactionStatus status, LocalDateTime startDate, LocalDateTime endDate);

    // Find by user and status
    List<Transaction> findByUserIdAndStatusOrderByCreatedAtDesc(Integer userId, TransactionStatus status);

    // Find recent transactions (using service to limit)
    List<Transaction> findTop10ByUserIdOrderByCreatedAtDesc(Integer userId);

    // Find all transactions for an account (both from and to) - Spring Data will implement this
    List<Transaction> findByFromAccountIdOrToAccountIdOrderByCreatedAtDesc(Integer fromAccountId, Integer toAccountId);

    // Paginated versions for better performance
    Page<Transaction> findByUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);
    Page<Transaction> findByFromAccountIdOrToAccountIdOrderByCreatedAtDesc(Integer fromAccountId, Integer toAccountId, Pageable pageable);
    Page<Transaction> findByStatusOrderByCreatedAtDesc(TransactionStatus status, Pageable pageable);
    Page<Transaction> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
}