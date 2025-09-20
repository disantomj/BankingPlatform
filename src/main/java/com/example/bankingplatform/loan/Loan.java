package com.example.bankingplatform.loan;

import com.example.bankingplatform.account.Account;
import com.example.bankingplatform.billing.BillingFrequency;
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
@Table(name = "loans", indexes = {
    @Index(name = "idx_loan_user", columnList = "user_id"),
    @Index(name = "idx_loan_account", columnList = "account_id"),
    @Index(name = "idx_loan_status", columnList = "status"),
    @Index(name = "idx_loan_type", columnList = "loan_type"),
    @Index(name = "idx_loan_reference", columnList = "loan_reference")
})
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String loanReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    private LoanType loanType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"password", "createdAt", "updatedAt"})
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "account_id")
    @JsonIgnoreProperties({"user"})
    private Account disbursementAccount; // Account where loan funds are deposited

    @Column(precision = 19, scale = 2, nullable = false)
    @NotNull
    @Positive(message = "Principal amount must be positive")
    private BigDecimal principalAmount;

    @Column(precision = 19, scale = 2, nullable = false)
    @NotNull
    @PositiveOrZero
    private BigDecimal currentBalance;

    @Column(precision = 5, scale = 4, nullable = false)
    @NotNull
    @DecimalMin(value = "0.0", message = "Interest rate cannot be negative")
    @DecimalMax(value = "1.0", message = "Interest rate cannot exceed 100%")
    private BigDecimal interestRate; // Annual interest rate as decimal (e.g., 0.05 for 5%)

    @Column(nullable = false)
    @NotNull
    @Positive
    private Integer termMonths; // Loan term in months

    @Column(precision = 19, scale = 2)
    @PositiveOrZero
    private BigDecimal monthlyPayment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    private BillingFrequency paymentFrequency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    private LoanStatus status;

    @Column(length = 3)
    @NotBlank
    @Size(min = 3, max = 3)
    private String currency = "USD";

    @NotBlank
    @Size(max = 500)
    private String purpose; // Purpose of the loan

    private String collateralDescription;

    @Column(precision = 19, scale = 2)
    @PositiveOrZero
    private BigDecimal collateralValue;

    // Payment tracking
    @Column(precision = 19, scale = 2)
    @PositiveOrZero
    private BigDecimal totalPaidAmount = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    @PositiveOrZero
    private BigDecimal totalInterestPaid = BigDecimal.ZERO;

    @PositiveOrZero
    private Integer paymentsRemaining;

    @PositiveOrZero
    private Integer paymentsMade = 0;

    // Important dates
    @Column(nullable = false)
    private LocalDate applicationDate;

    private LocalDate approvalDate;

    private LocalDate disbursementDate;

    @Column(nullable = false)
    private LocalDate firstPaymentDate;

    private LocalDate lastPaymentDate;

    @Column(nullable = false)
    private LocalDate maturityDate;

    // Late payment tracking
    @PositiveOrZero
    private Integer daysDelinquent = 0;

    @Column(precision = 19, scale = 2)
    @PositiveOrZero
    private BigDecimal lateFeesAccrued = BigDecimal.ZERO;

    // Credit information
    private Integer creditScore;

    @Column(precision = 19, scale = 2)
    @PositiveOrZero
    private BigDecimal annualIncome;

    private String employmentStatus;

    // Risk assessment
    @Column(length = 10)
    private String riskGrade; // A, B, C, D, E, etc.

    // External references
    private String applicationNumber;
    private Long originatingBranchId;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public Loan() {}

    public Loan(LoanType loanType, User user, Account disbursementAccount, BigDecimal principalAmount,
                BigDecimal interestRate, Integer termMonths, BillingFrequency paymentFrequency,
                String purpose, LocalDate applicationDate) {
        this.loanType = loanType;
        this.user = user;
        this.disbursementAccount = disbursementAccount;
        this.principalAmount = principalAmount;
        this.currentBalance = principalAmount;
        this.interestRate = interestRate;
        this.termMonths = termMonths;
        this.paymentFrequency = paymentFrequency;
        this.purpose = purpose;
        this.applicationDate = applicationDate;
        this.status = LoanStatus.PENDING;
        this.currency = "USD";
        this.paymentsRemaining = termMonths;
    }

    // Business logic methods
    public BigDecimal calculateMonthlyPayment() {
        if (paymentFrequency == BillingFrequency.MONTHLY && interestRate.compareTo(BigDecimal.ZERO) > 0) {
            // Calculate using standard loan payment formula: M = P * [r(1+r)^n] / [(1+r)^n - 1]
            BigDecimal monthlyRate = interestRate.divide(BigDecimal.valueOf(12), 10, BigDecimal.ROUND_HALF_UP);
            BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
            BigDecimal numerator = monthlyRate.multiply(onePlusRate.pow(termMonths));
            BigDecimal denominator = onePlusRate.pow(termMonths).subtract(BigDecimal.ONE);
            return principalAmount.multiply(numerator.divide(denominator, 2, BigDecimal.ROUND_HALF_UP));
        }
        // For other frequencies or zero interest, use simple division
        return principalAmount.divide(BigDecimal.valueOf(termMonths), 2, BigDecimal.ROUND_HALF_UP);
    }

    public BigDecimal getRemainingBalance() {
        return currentBalance;
    }

    public boolean isOverdue() {
        return daysDelinquent > 0;
    }

    public boolean isPaidOff() {
        return currentBalance.compareTo(BigDecimal.ZERO) <= 0 && status == LoanStatus.CLOSED;
    }

    public BigDecimal getLoanToValueRatio() {
        if (collateralValue != null && collateralValue.compareTo(BigDecimal.ZERO) > 0) {
            return principalAmount.divide(collateralValue, 4, BigDecimal.ROUND_HALF_UP);
        }
        return null;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLoanReference() { return loanReference; }
    public void setLoanReference(String loanReference) { this.loanReference = loanReference; }

    public LoanType getLoanType() { return loanType; }
    public void setLoanType(LoanType loanType) { this.loanType = loanType; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Account getDisbursementAccount() { return disbursementAccount; }
    public void setDisbursementAccount(Account disbursementAccount) { this.disbursementAccount = disbursementAccount; }

    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public void setPrincipalAmount(BigDecimal principalAmount) { this.principalAmount = principalAmount; }

    public BigDecimal getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(BigDecimal currentBalance) { this.currentBalance = currentBalance; }

    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }

    public Integer getTermMonths() { return termMonths; }
    public void setTermMonths(Integer termMonths) { this.termMonths = termMonths; }

    public BigDecimal getMonthlyPayment() { return monthlyPayment; }
    public void setMonthlyPayment(BigDecimal monthlyPayment) { this.monthlyPayment = monthlyPayment; }

    public BillingFrequency getPaymentFrequency() { return paymentFrequency; }
    public void setPaymentFrequency(BillingFrequency paymentFrequency) { this.paymentFrequency = paymentFrequency; }

    public LoanStatus getStatus() { return status; }
    public void setStatus(LoanStatus status) { this.status = status; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getCollateralDescription() { return collateralDescription; }
    public void setCollateralDescription(String collateralDescription) { this.collateralDescription = collateralDescription; }

    public BigDecimal getCollateralValue() { return collateralValue; }
    public void setCollateralValue(BigDecimal collateralValue) { this.collateralValue = collateralValue; }

    public BigDecimal getTotalPaidAmount() { return totalPaidAmount; }
    public void setTotalPaidAmount(BigDecimal totalPaidAmount) { this.totalPaidAmount = totalPaidAmount; }

    public BigDecimal getTotalInterestPaid() { return totalInterestPaid; }
    public void setTotalInterestPaid(BigDecimal totalInterestPaid) { this.totalInterestPaid = totalInterestPaid; }

    public Integer getPaymentsRemaining() { return paymentsRemaining; }
    public void setPaymentsRemaining(Integer paymentsRemaining) { this.paymentsRemaining = paymentsRemaining; }

    public Integer getPaymentsMade() { return paymentsMade; }
    public void setPaymentsMade(Integer paymentsMade) { this.paymentsMade = paymentsMade; }

    public LocalDate getApplicationDate() { return applicationDate; }
    public void setApplicationDate(LocalDate applicationDate) { this.applicationDate = applicationDate; }

    public LocalDate getApprovalDate() { return approvalDate; }
    public void setApprovalDate(LocalDate approvalDate) { this.approvalDate = approvalDate; }

    public LocalDate getDisbursementDate() { return disbursementDate; }
    public void setDisbursementDate(LocalDate disbursementDate) { this.disbursementDate = disbursementDate; }

    public LocalDate getFirstPaymentDate() { return firstPaymentDate; }
    public void setFirstPaymentDate(LocalDate firstPaymentDate) { this.firstPaymentDate = firstPaymentDate; }

    public LocalDate getLastPaymentDate() { return lastPaymentDate; }
    public void setLastPaymentDate(LocalDate lastPaymentDate) { this.lastPaymentDate = lastPaymentDate; }

    public LocalDate getMaturityDate() { return maturityDate; }
    public void setMaturityDate(LocalDate maturityDate) { this.maturityDate = maturityDate; }

    public Integer getDaysDelinquent() { return daysDelinquent; }
    public void setDaysDelinquent(Integer daysDelinquent) { this.daysDelinquent = daysDelinquent; }

    public BigDecimal getLateFeesAccrued() { return lateFeesAccrued; }
    public void setLateFeesAccrued(BigDecimal lateFeesAccrued) { this.lateFeesAccrued = lateFeesAccrued; }

    public Integer getCreditScore() { return creditScore; }
    public void setCreditScore(Integer creditScore) { this.creditScore = creditScore; }

    public BigDecimal getAnnualIncome() { return annualIncome; }
    public void setAnnualIncome(BigDecimal annualIncome) { this.annualIncome = annualIncome; }

    public String getEmploymentStatus() { return employmentStatus; }
    public void setEmploymentStatus(String employmentStatus) { this.employmentStatus = employmentStatus; }

    public String getRiskGrade() { return riskGrade; }
    public void setRiskGrade(String riskGrade) { this.riskGrade = riskGrade; }

    public String getApplicationNumber() { return applicationNumber; }
    public void setApplicationNumber(String applicationNumber) { this.applicationNumber = applicationNumber; }

    public Long getOriginatingBranchId() { return originatingBranchId; }
    public void setOriginatingBranchId(Long originatingBranchId) { this.originatingBranchId = originatingBranchId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Loan loan = (Loan) o;
        return Objects.equals(id, loan.id) &&
               Objects.equals(loanReference, loan.loanReference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, loanReference);
    }

    @Override
    public String toString() {
        return "Loan{" +
                "id=" + id +
                ", loanReference='" + loanReference + '\'' +
                ", loanType=" + loanType +
                ", principalAmount=" + principalAmount +
                ", currentBalance=" + currentBalance +
                ", status=" + status +
                ", maturityDate=" + maturityDate +
                '}';
    }
}