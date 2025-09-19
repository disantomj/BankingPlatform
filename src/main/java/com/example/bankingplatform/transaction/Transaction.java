package com.example.bankingplatform.transaction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String transactionReference; // Custom generated reference number

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    private TransactionType transactionType;

    @Column(precision = 19, scale = 2, nullable = false)
    @NotNull
    @Positive(message = "Transaction amount must be positive")
    private BigDecimal amount;

    // Account relationships
    @Column(name = "from_account_id")
    private Integer fromAccountId; // Source account (null for deposits)

    @Column(name = "to_account_id")
    private Integer toAccountId; // Destination account (null for withdrawals)

    // User who initiated the transaction
    @Column(name = "user_id", nullable = false)
    @NotNull
    private Integer userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    private TransactionStatus status;

    @Enumerated(EnumType.STRING)
    private TransactionChannel channel;

    // Currency for this transaction
    @Column(length = 3)
    @NotBlank
    @Size(min = 3, max = 3)
    private String currency = "USD";

    // Transaction fee amount (separate from main amount)
    @Column(precision = 19, scale = 2)
    @PositiveOrZero
    private BigDecimal feeAmount = BigDecimal.ZERO;

    private String description;

    private String memo; // Customer memo/note

    // Running balance after this transaction (for the primary account)
    @Column(precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    // External reference (for bank transfers, check numbers, etc.)
    private String externalReference;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime processedAt; // When transaction was actually processed

    private LocalDateTime completedAt; // When transaction was completed

    // Default constructor
    public Transaction() {}

    // Constructor for basic transaction
    public Transaction(TransactionType transactionType, BigDecimal amount,
                       Integer userId, TransactionStatus status) {
        this.transactionType = transactionType;
        this.amount = amount;
        this.userId = userId;
        this.status = status;
        this.currency = "USD";
        this.feeAmount = BigDecimal.ZERO;
    }

    // Constructor for transfer transaction
    public Transaction(TransactionType transactionType, BigDecimal amount,
                       Integer fromAccountId, Integer toAccountId, Integer userId,
                       TransactionStatus status, String currency) {
        this.transactionType = transactionType;
        this.amount = amount;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.userId = userId;
        this.status = status;
        this.currency = currency;
        this.feeAmount = BigDecimal.ZERO;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTransactionReference() { return transactionReference; }
    public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }

    public TransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(TransactionType transactionType) { this.transactionType = transactionType; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Integer getFromAccountId() { return fromAccountId; }
    public void setFromAccountId(Integer fromAccountId) { this.fromAccountId = fromAccountId; }

    public Integer getToAccountId() { return toAccountId; }
    public void setToAccountId(Integer toAccountId) { this.toAccountId = toAccountId; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }

    public TransactionChannel getChannel() { return channel; }
    public void setChannel(TransactionChannel channel) { this.channel = channel; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }

    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }

    public String getExternalReference() { return externalReference; }
    public void setExternalReference(String externalReference) { this.externalReference = externalReference; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public BigDecimal getFeeAmount() { return feeAmount; }
    public void setFeeAmount(BigDecimal feeAmount) { this.feeAmount = feeAmount; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(transactionReference, that.transactionReference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, transactionReference);
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", transactionReference='" + transactionReference + '\'' +
                ", transactionType=" + transactionType +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}
