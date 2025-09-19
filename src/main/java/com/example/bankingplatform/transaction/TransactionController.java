package com.example.bankingplatform.transaction;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    @Autowired
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // Create deposit
    @PostMapping("/deposit")
    public ResponseEntity<?> createDeposit(@Valid @RequestBody DepositRequest request) {
        try {
            Transaction transaction = transactionService.createDeposit(
                request.getToAccountId(),
                request.getAmount(),
                request.getUserId(),
                request.getDescription(),
                request.getChannel()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    // Create withdrawal
    @PostMapping("/withdrawal")
    public ResponseEntity<?> createWithdrawal(@Valid @RequestBody WithdrawalRequest request) {
        try {
            Transaction transaction = transactionService.createWithdrawal(
                request.getFromAccountId(),
                request.getAmount(),
                request.getUserId(),
                request.getDescription(),
                request.getChannel()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    // Create transfer
    @PostMapping("/transfer")
    public ResponseEntity<?> createTransfer(@Valid @RequestBody TransferRequest request) {
        try {
            Transaction transaction = transactionService.createTransfer(
                request.getFromAccountId(),
                request.getToAccountId(),
                request.getAmount(),
                request.getUserId(),
                request.getDescription(),
                request.getChannel()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    // Process a pending transaction
    @PutMapping("/{id}/process")
    public ResponseEntity<?> processTransaction(@PathVariable Long id) {
        try {
            Transaction transaction = transactionService.processTransaction(id);
            return ResponseEntity.ok(transaction);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    // Cancel a pending transaction
    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelTransaction(@PathVariable Long id) {
        try {
            Transaction transaction = transactionService.cancelTransaction(id);
            return ResponseEntity.ok(transaction);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    // Get transaction by ID
    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getTransaction(@PathVariable Long id) {
        try {
            Transaction transaction = transactionService.findById(id);
            return ResponseEntity.ok(transaction);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Get transaction by reference
    @GetMapping("/reference/{reference}")
    public ResponseEntity<Transaction> getTransactionByReference(@PathVariable String reference) {
        return transactionService.findByReference(reference)
            .map(transaction -> ResponseEntity.ok(transaction))
            .orElse(ResponseEntity.notFound().build());
    }

    // Get all transactions for an account
    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<Transaction>> getAccountTransactions(@PathVariable Integer accountId) {
        List<Transaction> transactions = transactionService.getAccountTransactions(accountId);
        return ResponseEntity.ok(transactions);
    }

    // Get transactions for an account by date range
    @GetMapping("/account/{accountId}/date-range")
    public ResponseEntity<List<Transaction>> getAccountTransactionsByDateRange(
            @PathVariable Integer accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        List<Transaction> transactions = transactionService.getAccountTransactionsByDateRange(accountId, startDate, endDate);
        return ResponseEntity.ok(transactions);
    }

    // Get all transactions for a user
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Transaction>> getUserTransactions(@PathVariable Integer userId) {
        List<Transaction> transactions = transactionService.getUserTransactions(userId);
        return ResponseEntity.ok(transactions);
    }

    // Get recent transactions for a user
    @GetMapping("/user/{userId}/recent")
    public ResponseEntity<List<Transaction>> getRecentUserTransactions(@PathVariable Integer userId) {
        List<Transaction> transactions = transactionService.getRecentUserTransactions(userId);
        return ResponseEntity.ok(transactions);
    }

    // Get transactions by date range
    @GetMapping("/date-range")
    public ResponseEntity<List<Transaction>> getTransactionsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        List<Transaction> transactions = transactionService.getTransactionsByDateRange(startDate, endDate);
        return ResponseEntity.ok(transactions);
    }

    // Request DTOs
    public static class DepositRequest {
        private Integer toAccountId;
        private BigDecimal amount;
        private Integer userId;
        private String description;
        private TransactionChannel channel;

        // Getters and setters
        public Integer getToAccountId() { return toAccountId; }
        public void setToAccountId(Integer toAccountId) { this.toAccountId = toAccountId; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public Integer getUserId() { return userId; }
        public void setUserId(Integer userId) { this.userId = userId; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public TransactionChannel getChannel() { return channel; }
        public void setChannel(TransactionChannel channel) { this.channel = channel; }
    }

    public static class WithdrawalRequest {
        private Integer fromAccountId;
        private BigDecimal amount;
        private Integer userId;
        private String description;
        private TransactionChannel channel;

        // Getters and setters
        public Integer getFromAccountId() { return fromAccountId; }
        public void setFromAccountId(Integer fromAccountId) { this.fromAccountId = fromAccountId; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public Integer getUserId() { return userId; }
        public void setUserId(Integer userId) { this.userId = userId; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public TransactionChannel getChannel() { return channel; }
        public void setChannel(TransactionChannel channel) { this.channel = channel; }
    }

    public static class TransferRequest {
        private Integer fromAccountId;
        private Integer toAccountId;
        private BigDecimal amount;
        private Integer userId;
        private String description;
        private TransactionChannel channel;

        // Getters and setters
        public Integer getFromAccountId() { return fromAccountId; }
        public void setFromAccountId(Integer fromAccountId) { this.fromAccountId = fromAccountId; }

        public Integer getToAccountId() { return toAccountId; }
        public void setToAccountId(Integer toAccountId) { this.toAccountId = toAccountId; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public Integer getUserId() { return userId; }
        public void setUserId(Integer userId) { this.userId = userId; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public TransactionChannel getChannel() { return channel; }
        public void setChannel(TransactionChannel channel) { this.channel = channel; }
    }
}