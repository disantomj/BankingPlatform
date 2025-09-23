package com.example.bankingplatform.transaction;

import com.example.bankingplatform.account.Account;
import com.example.bankingplatform.account.AccountRepository;
import com.example.bankingplatform.audit.AuditAction;
import com.example.bankingplatform.audit.AuditService;
import com.example.bankingplatform.audit.AuditSeverity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final AuditService auditService;

    @Autowired
    public TransactionService(TransactionRepository transactionRepository, AccountRepository accountRepository, AuditService auditService) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.auditService = auditService;
    }

    // Create deposit transaction
    public Transaction createDeposit(Integer toAccountId, BigDecimal amount, Integer userId,
                                   String description, TransactionChannel channel) {

        Account account = accountRepository.findById(toAccountId)
            .orElseThrow(() -> new RuntimeException("Account not found: " + toAccountId));

        Transaction transaction = new Transaction();
        transaction.setTransactionReference(generateTransactionReference());
        transaction.setTransactionType(TransactionType.DEPOSIT);
        transaction.setAmount(amount);
        transaction.setToAccountId(toAccountId);
        transaction.setUserId(userId);
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setChannel(channel);
        transaction.setDescription(description);
        transaction.setCurrency(account.getCurrency());

        Transaction savedTransaction = transactionRepository.save(transaction);

        auditService.logEntityAction(
            userId,
            "User" + userId,
            AuditAction.TRANSACTION_CREATED,
            AuditSeverity.MEDIUM,
            "Deposit transaction created - Amount: " + amount + " " + account.getCurrency(),
            "Transaction",
            savedTransaction.getId().toString()
        );

        return savedTransaction;
    }

    // Create withdrawal transaction
    public Transaction createWithdrawal(Integer fromAccountId, BigDecimal amount, Integer userId,
                                      String description, TransactionChannel channel) {

        Account account = accountRepository.findById(fromAccountId)
            .orElseThrow(() -> new RuntimeException("Account not found: " + fromAccountId));

        // Check sufficient balance
        if (account.getAvailableBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }

        Transaction transaction = new Transaction();
        transaction.setTransactionReference(generateTransactionReference());
        transaction.setTransactionType(TransactionType.WITHDRAWAL);
        transaction.setAmount(amount);
        transaction.setFromAccountId(fromAccountId);
        transaction.setUserId(userId);
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setChannel(channel);
        transaction.setDescription(description);
        transaction.setCurrency(account.getCurrency());

        Transaction savedTransaction = transactionRepository.save(transaction);

        auditService.logEntityAction(
            userId,
            "User" + userId,
            AuditAction.TRANSACTION_CREATED,
            AuditSeverity.MEDIUM,
            "Withdrawal transaction created - Amount: " + amount + " " + account.getCurrency(),
            "Transaction",
            savedTransaction.getId().toString()
        );

        return savedTransaction;
    }

    // Create transfer transaction
    public Transaction createTransfer(Integer fromAccountId, Integer toAccountId, BigDecimal amount,
                                    Integer userId, String description, TransactionChannel channel) {

        Account fromAccount = accountRepository.findById(fromAccountId)
            .orElseThrow(() -> new RuntimeException("Source account not found: " + fromAccountId));
        Account toAccount = accountRepository.findById(toAccountId)
            .orElseThrow(() -> new RuntimeException("Destination account not found: " + toAccountId));

        // Check sufficient balance
        if (fromAccount.getAvailableBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }

        // Check currency match (for now, require same currency)
        if (!fromAccount.getCurrency().equals(toAccount.getCurrency())) {
            throw new RuntimeException("Currency mismatch between accounts");
        }

        Transaction transaction = new Transaction();
        transaction.setTransactionReference(generateTransactionReference());
        transaction.setTransactionType(TransactionType.TRANSFER);
        transaction.setAmount(amount);
        transaction.setFromAccountId(fromAccountId);
        transaction.setToAccountId(toAccountId);
        transaction.setUserId(userId);
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setChannel(channel);
        transaction.setDescription(description);
        transaction.setCurrency(fromAccount.getCurrency());

        Transaction savedTransaction = transactionRepository.save(transaction);

        auditService.logEntityAction(
            userId,
            "User" + userId,
            AuditAction.TRANSACTION_CREATED,
            AuditSeverity.MEDIUM,
            "Transfer transaction created - Amount: " + amount + " " + fromAccount.getCurrency() + " from Account " + fromAccountId + " to Account " + toAccountId,
            "Transaction",
            savedTransaction.getId().toString()
        );

        return savedTransaction;
    }

    // Process/Complete a transaction
    public Transaction processTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new RuntimeException("Transaction is not in pending status");
        }

        try {
            // Update account balances based on transaction type
            updateAccountBalances(transaction);

            // Mark transaction as completed
            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction.setProcessedAt(LocalDateTime.now());
            transaction.setCompletedAt(LocalDateTime.now());

            Transaction completedTransaction = transactionRepository.save(transaction);

            auditService.logChange(
                transaction.getUserId(),
                "User" + transaction.getUserId(),
                AuditAction.TRANSACTION_PROCESSED,
                AuditSeverity.MEDIUM,
                "Transaction processed successfully - " + transaction.getTransactionType() + " for " + transaction.getAmount() + " " + transaction.getCurrency(),
                "Transaction",
                transactionId.toString(),
                TransactionStatus.PENDING.toString(),
                TransactionStatus.COMPLETED.toString()
            );

            return completedTransaction;
        } catch (Exception e) {
            // Mark transaction as failed
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setProcessedAt(LocalDateTime.now());
            transactionRepository.save(transaction);

            auditService.logFailedAction(
                transaction.getUserId(),
                "User" + transaction.getUserId(),
                AuditAction.TRANSACTION_PROCESSED,
                AuditSeverity.HIGH,
                "Transaction processing failed - " + transaction.getTransactionType() + " for " + transaction.getAmount() + " " + transaction.getCurrency(),
                e.getMessage()
            );

            throw new RuntimeException("Transaction processing failed: " + e.getMessage());
        }
    }

    // Get all transactions for an account (both incoming and outgoing) - optimized single query
    public List<Transaction> getAccountTransactions(Integer accountId) {
        // Use single query instead of two separate queries + merge
        return transactionRepository.findByFromAccountIdOrToAccountIdOrderByCreatedAtDesc(accountId, accountId);
    }

    // Get transactions by user
    @Transactional(readOnly = true)
    public List<Transaction> getUserTransactions(Integer userId) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // Get paginated transactions by user
    @Transactional(readOnly = true)
    public Page<Transaction> getUserTransactionsPaginated(Integer userId, Pageable pageable) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    // Get paginated transactions for an account
    @Transactional(readOnly = true)
    public Page<Transaction> getAccountTransactionsPaginated(Integer accountId, Pageable pageable) {
        return transactionRepository.findByFromAccountIdOrToAccountIdOrderByCreatedAtDesc(accountId, accountId, pageable);
    }

    // Get transactions by date range
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return transactionRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(startDate, endDate);
    }

    // Get account transactions by date range
    @Transactional(readOnly = true)
    public List<Transaction> getAccountTransactionsByDateRange(Integer accountId, LocalDateTime startDate, LocalDateTime endDate) {
        List<Transaction> allTransactions = getAccountTransactions(accountId);
        return allTransactions.stream()
            .filter(t -> t.getCreatedAt().isAfter(startDate) && t.getCreatedAt().isBefore(endDate))
            .collect(Collectors.toList());
    }

    // Find transaction by ID
    @Transactional(readOnly = true)
    public Transaction findById(Long transactionId) {
        return transactionRepository.findById(transactionId)
            .orElseThrow(() -> new RuntimeException("Transaction not found with id: " + transactionId));
    }

    // Find transaction by reference
    @Transactional(readOnly = true)
    public Optional<Transaction> findByReference(String transactionReference) {
        return transactionRepository.findByTransactionReference(transactionReference);
    }

    // Get recent transactions for user
    @Transactional(readOnly = true)
    public List<Transaction> getRecentUserTransactions(Integer userId) {
        return transactionRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId);
    }

    // Cancel a pending transaction
    public Transaction cancelTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new RuntimeException("Only pending transactions can be cancelled");
        }

        transaction.setStatus(TransactionStatus.CANCELLED);
        transaction.setProcessedAt(LocalDateTime.now());

        Transaction cancelledTransaction = transactionRepository.save(transaction);

        auditService.logChange(
            transaction.getUserId(),
            "User" + transaction.getUserId(),
            AuditAction.TRANSACTION_CANCELLED,
            AuditSeverity.MEDIUM,
            "Transaction cancelled - " + transaction.getTransactionType() + " for " + transaction.getAmount() + " " + transaction.getCurrency(),
            "Transaction",
            transactionId.toString(),
            TransactionStatus.PENDING.toString(),
            TransactionStatus.CANCELLED.toString()
        );

        return cancelledTransaction;
    }

    // Private helper methods
    private void updateAccountBalances(Transaction transaction) {
        switch (transaction.getTransactionType()) {
            case DEPOSIT:
                updateAccountBalance(transaction.getToAccountId(), transaction.getAmount(), true);
                break;
            case WITHDRAWAL:
                updateAccountBalance(transaction.getFromAccountId(), transaction.getAmount(), false);
                break;
            case TRANSFER:
                updateAccountBalance(transaction.getFromAccountId(), transaction.getAmount(), false);
                updateAccountBalance(transaction.getToAccountId(), transaction.getAmount(), true);
                break;
            default:
                throw new RuntimeException("Unsupported transaction type: " + transaction.getTransactionType());
        }
    }

    private void updateAccountBalance(Integer accountId, BigDecimal amount, boolean isCredit) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));

        BigDecimal newBalance = isCredit
            ? account.getBalance().add(amount)
            : account.getBalance().subtract(amount);

        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Transaction would result in negative balance");
        }

        account.setBalance(newBalance);
        account.setAvailableBalance(newBalance);
        accountRepository.save(account);
    }

    private String generateTransactionReference() {
        String reference;
        do {
            reference = "TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        } while (transactionRepository.existsByTransactionReference(reference));
        return reference;
    }
}