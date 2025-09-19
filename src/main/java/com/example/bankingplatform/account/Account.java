package com.example.bankingplatform.account;

import com.example.bankingplatform.user.User;
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
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false)
    private String accountNum;

    @Enumerated(EnumType.STRING)
    @NotNull
    private AccountType accountType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"password", "createdAt", "updatedAt"})
    private User user;

    @Column(precision = 19, scale = 2)
    @NotNull
    @DecimalMin(value = "0.0", inclusive = true, message = "Balance cannot be negative")
    private BigDecimal balance;

    @Column(precision = 19, scale = 2)
    @NotNull
    @PositiveOrZero
    private BigDecimal availableBalance;

    @Column(precision = 19, scale = 2)
    @NotNull
    @PositiveOrZero
    private BigDecimal minimumBalance;

    @Column(precision = 19, scale = 2)
    @NotNull
    @PositiveOrZero
    private BigDecimal overdraftLimit;

    @Enumerated(EnumType.STRING)
    @NotNull
    private AccountStatus status;

    @NotBlank
    @Size(max = 100)
    private String accountName; // "John's Checking"

    private String description;

    @Column(length = 3)
    @NotBlank
    @Size(min = 3, max = 3)
    private String currency; // "USD"

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime closedAt; // Nullable

    // Constructors
    public Account() {}

    public Account(String accountNum, AccountType accountType, User user, BigDecimal balance,
                   BigDecimal availableBalance, BigDecimal minimumBalance, BigDecimal overdraftLimit,
                   AccountStatus status, String accountName, String currency) {
        this.accountNum = accountNum;
        this.accountType = accountType;
        this.user = user;
        this.balance = balance;
        this.availableBalance = availableBalance;
        this.minimumBalance = minimumBalance;
        this.overdraftLimit = overdraftLimit;
        this.status = status;
        this.accountName = accountName;
        this.currency = currency;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getAccountNum() {
        return accountNum;
    }

    public void setAccountNum(String accountNum) {
        this.accountNum = accountNum;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public void setAvailableBalance(BigDecimal availableBalance) {
        this.availableBalance = availableBalance;
    }

    public BigDecimal getMinimumBalance() {
        return minimumBalance;
    }

    public void setMinimumBalance(BigDecimal minimumBalance) {
        this.minimumBalance = minimumBalance;
    }

    public BigDecimal getOverdraftLimit() {
        return overdraftLimit;
    }

    public void setOverdraftLimit(BigDecimal overdraftLimit) {
        this.overdraftLimit = overdraftLimit;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return Objects.equals(id, account.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
