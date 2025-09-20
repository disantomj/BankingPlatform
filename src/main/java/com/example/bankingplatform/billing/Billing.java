package com.example.bankingplatform.billing;

import com.example.bankingplatform.account.Account;
import com.example.bankingplatform.user.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "billings", indexes = {
    @Index(name = "idx_billing_user", columnList = "user_id"),
    @Index(name = "idx_billing_account", columnList = "account_id"),
    @Index(name = "idx_billing_status", columnList = "status"),
    @Index(name = "idx_billing_due_date", columnList = "due_date"),
    @Index(name = "idx_billing_type", columnList = "billing_type")
})
public class Billing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String billingReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    private BillingType billingType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"password", "createdAt", "updatedAt"})
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "account_id")
    @JsonIgnoreProperties({"user"})
    private Account account; // Nullable for user-level billing

    @Column(precision = 19, scale = 2, nullable = false)
    @NotNull
    @Positive(message = "Billing amount must be positive")
    private BigDecimal amount;

    @Column(precision = 19, scale = 2)
    @PositiveOrZero
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    @PositiveOrZero
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2, nullable = false)
    @NotNull
    @PositiveOrZero
    private BigDecimal totalAmount;

    @Column(precision = 19, scale = 2)
    @PositiveOrZero
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    private BillingStatus status;

    @Enumerated(EnumType.STRING)
    private BillingFrequency frequency;

    @Column(length = 3)
    @NotBlank
    @Size(min = 3, max = 3)
    private String currency = "USD";

    @NotBlank
    @Size(max = 200)
    private String description;

    private String memo;

    @Column(nullable = false)
    private LocalDate issueDate;

    @Column(nullable = false)
    private LocalDate dueDate;

    private LocalDate paidDate;

    private LocalDate nextBillingDate; // For recurring billing

    // For subscription billing
    private LocalDate subscriptionStartDate;
    private LocalDate subscriptionEndDate;

    // Late fee configuration
    @Column(precision = 19, scale = 2)
    @PositiveOrZero
    private BigDecimal lateFeeAmount = BigDecimal.ZERO;

    private Integer gracePeriodDays = 0;

    // Reference to related transaction when paid
    private Long paymentTransactionId;

    // External references
    private String invoiceNumber;
    private String purchaseOrderNumber;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public Billing() {}

    public Billing(BillingType billingType, User user, Account account, BigDecimal amount,
                   BillingStatus status, String description, LocalDate issueDate, LocalDate dueDate) {
        this.billingType = billingType;
        this.user = user;
        this.account = account;
        this.amount = amount;
        this.totalAmount = amount;
        this.status = status;
        this.description = description;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.currency = "USD";
    }

    // Calculate total amount including tax and discount
    public void calculateTotalAmount() {
        this.totalAmount = this.amount
            .add(this.taxAmount != null ? this.taxAmount : BigDecimal.ZERO)
            .subtract(this.discountAmount != null ? this.discountAmount : BigDecimal.ZERO);
    }

    // Calculate remaining balance
    public BigDecimal getRemainingBalance() {
        return this.totalAmount.subtract(this.paidAmount != null ? this.paidAmount : BigDecimal.ZERO);
    }

    // Check if overdue
    public boolean isOverdue() {
        return this.status != BillingStatus.PAID &&
               this.dueDate != null &&
               LocalDate.now().isAfter(this.dueDate);
    }

    // Check if payment is complete
    public boolean isFullyPaid() {
        return this.paidAmount != null &&
               this.paidAmount.compareTo(this.totalAmount) >= 0;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBillingReference() { return billingReference; }
    public void setBillingReference(String billingReference) { this.billingReference = billingReference; }

    public BillingType getBillingType() { return billingType; }
    public void setBillingType(BillingType billingType) { this.billingType = billingType; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }

    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }

    public BillingStatus getStatus() { return status; }
    public void setStatus(BillingStatus status) { this.status = status; }

    public BillingFrequency getFrequency() { return frequency; }
    public void setFrequency(BillingFrequency frequency) { this.frequency = frequency; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }

    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public LocalDate getPaidDate() { return paidDate; }
    public void setPaidDate(LocalDate paidDate) { this.paidDate = paidDate; }

    public LocalDate getNextBillingDate() { return nextBillingDate; }
    public void setNextBillingDate(LocalDate nextBillingDate) { this.nextBillingDate = nextBillingDate; }

    public LocalDate getSubscriptionStartDate() { return subscriptionStartDate; }
    public void setSubscriptionStartDate(LocalDate subscriptionStartDate) { this.subscriptionStartDate = subscriptionStartDate; }

    public LocalDate getSubscriptionEndDate() { return subscriptionEndDate; }
    public void setSubscriptionEndDate(LocalDate subscriptionEndDate) { this.subscriptionEndDate = subscriptionEndDate; }

    public BigDecimal getLateFeeAmount() { return lateFeeAmount; }
    public void setLateFeeAmount(BigDecimal lateFeeAmount) { this.lateFeeAmount = lateFeeAmount; }

    public Integer getGracePeriodDays() { return gracePeriodDays; }
    public void setGracePeriodDays(Integer gracePeriodDays) { this.gracePeriodDays = gracePeriodDays; }

    public Long getPaymentTransactionId() { return paymentTransactionId; }
    public void setPaymentTransactionId(Long paymentTransactionId) { this.paymentTransactionId = paymentTransactionId; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public String getPurchaseOrderNumber() { return purchaseOrderNumber; }
    public void setPurchaseOrderNumber(String purchaseOrderNumber) { this.purchaseOrderNumber = purchaseOrderNumber; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Billing billing = (Billing) o;
        return Objects.equals(id, billing.id) &&
               Objects.equals(billingReference, billing.billingReference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, billingReference);
    }

    @Override
    public String toString() {
        return "Billing{" +
                "id=" + id +
                ", billingReference='" + billingReference + '\'' +
                ", billingType=" + billingType +
                ", amount=" + amount +
                ", totalAmount=" + totalAmount +
                ", status=" + status +
                ", dueDate=" + dueDate +
                '}';
    }
}