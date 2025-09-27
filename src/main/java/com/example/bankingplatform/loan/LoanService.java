package com.example.bankingplatform.loan;

import com.example.bankingplatform.account.Account;
import com.example.bankingplatform.audit.AuditAction;
import com.example.bankingplatform.audit.AuditService;
import com.example.bankingplatform.audit.AuditSeverity;
import com.example.bankingplatform.billing.BillingFrequency;
import com.example.bankingplatform.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class LoanService {

    private final LoanRepository loanRepository;
    private final AuditService auditService;
    private final CreditScoringService creditScoringService;

    @Autowired
    public LoanService(LoanRepository loanRepository, AuditService auditService, CreditScoringService creditScoringService) {
        this.loanRepository = loanRepository;
        this.auditService = auditService;
        this.creditScoringService = creditScoringService;
    }

    public Loan createLoanApplication(User user, Account disbursementAccount, LoanType loanType,
                                    BigDecimal principalAmount, BigDecimal interestRate, Integer termMonths,
                                    BillingFrequency paymentFrequency, String purpose) {
        String loanReference = generateLoanReference();

        // Automatically evaluate loan application using credit scoring
        CreditScoringService.LoanApprovalDecision decision = creditScoringService.shouldApproveLoan(user, principalAmount, loanType);

        Loan loan = new Loan(loanType, user, disbursementAccount, principalAmount, interestRate,
                           termMonths, paymentFrequency, purpose, LocalDate.now());
        loan.setLoanReference(loanReference);
        loan.setFirstPaymentDate(calculateFirstPaymentDate(LocalDate.now(), paymentFrequency));
        loan.setMaturityDate(calculateMaturityDate(LocalDate.now(), termMonths));
        loan.setMonthlyPayment(loan.calculateMonthlyPayment());

        // Set initial status based on automated decision
        if (decision.isApproved()) {
            loan.setStatus(LoanStatus.APPROVED);
            loan.setApprovalDate(LocalDate.now());
        } else {
            loan.setStatus(LoanStatus.REJECTED);
        }

        Loan savedLoan = loanRepository.save(loan);

        // Log the automated decision
        auditService.logEntityAction(
            user.getId(),
            "SYSTEM_AUTO_APPROVAL",
            decision.isApproved() ? AuditAction.LOAN_APPROVED : AuditAction.LOAN_REJECTED,
            AuditSeverity.HIGH,
            "Loan automatically " + (decision.isApproved() ? "approved" : "rejected") +
            " - Credit Score: " + decision.getCreditScore().getScore() +
            " - Reason: " + decision.getReason(),
            "Loan",
            savedLoan.getId().toString()
        );

        return savedLoan;
    }

    public Loan approveLoan(Long loanId, String approvedBy) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found with ID: " + loanId));

        if (loan.getStatus() != LoanStatus.PENDING && loan.getStatus() != LoanStatus.UNDER_REVIEW) {
            throw new IllegalStateException("Loan cannot be approved in current status: " + loan.getStatus());
        }

        loan.setStatus(LoanStatus.APPROVED);
        loan.setApprovalDate(LocalDate.now());

        Loan savedLoan = loanRepository.save(loan);

        auditService.logEntityAction(
            loan.getUser().getId(),
            approvedBy,
            AuditAction.LOAN_APPROVED,
            AuditSeverity.HIGH,
            "Loan approved - " + loan.getLoanReference() + " for " + loan.getPrincipalAmount() + " " + loan.getCurrency(),
            "Loan",
            savedLoan.getId().toString()
        );

        return savedLoan;
    }

    public Loan rejectLoan(Long loanId, String rejectedBy) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found with ID: " + loanId));

        if (loan.getStatus() != LoanStatus.PENDING && loan.getStatus() != LoanStatus.UNDER_REVIEW) {
            throw new IllegalStateException("Loan cannot be rejected in current status: " + loan.getStatus());
        }

        loan.setStatus(LoanStatus.REJECTED);

        Loan savedLoan = loanRepository.save(loan);

        auditService.logEntityAction(
            loan.getUser().getId(),
            rejectedBy,
            AuditAction.LOAN_REJECTED,
            AuditSeverity.MEDIUM,
            "Loan rejected - " + loan.getLoanReference(),
            "Loan",
            savedLoan.getId().toString()
        );

        return savedLoan;
    }

    public Loan disburseLoan(Long loanId, String disbursedBy) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found with ID: " + loanId));

        if (loan.getStatus() != LoanStatus.APPROVED) {
            throw new IllegalStateException("Only approved loans can be disbursed. Current status: " + loan.getStatus());
        }

        loan.setStatus(LoanStatus.ACTIVE);
        loan.setDisbursementDate(LocalDate.now());

        Loan savedLoan = loanRepository.save(loan);

        auditService.logEntityAction(
            loan.getUser().getId(),
            disbursedBy,
            AuditAction.LOAN_DISBURSED,
            AuditSeverity.HIGH,
            "Loan disbursed - " + loan.getLoanReference() + " amount: " + loan.getPrincipalAmount() + " " + loan.getCurrency(),
            "Loan",
            savedLoan.getId().toString()
        );

        return savedLoan;
    }

    public Loan processPayment(Long loanId, BigDecimal paymentAmount, Long transactionId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found with ID: " + loanId));

        if (loan.getStatus() != LoanStatus.ACTIVE) {
            throw new IllegalStateException("Payments can only be made on active loans");
        }

        if (paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }

        BigDecimal newBalance = loan.getCurrentBalance().subtract(paymentAmount);
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            newBalance = BigDecimal.ZERO;
        }

        loan.setCurrentBalance(newBalance);
        loan.setTotalPaidAmount(loan.getTotalPaidAmount().add(paymentAmount));
        loan.setPaymentsMade(loan.getPaymentsMade() + 1);
        loan.setPaymentsRemaining(loan.getPaymentsRemaining() - 1);
        loan.setLastPaymentDate(LocalDate.now());
        loan.setDaysDelinquent(0); // Reset delinquency on payment

        if (newBalance.compareTo(BigDecimal.ZERO) == 0) {
            loan.setStatus(LoanStatus.CLOSED);
        }

        Loan savedLoan = loanRepository.save(loan);

        auditService.logEntityAction(
            loan.getUser().getId(),
            loan.getUser().getUsername(),
            AuditAction.LOAN_PAYMENT_PROCESSED,
            AuditSeverity.MEDIUM,
            "Loan payment processed - " + loan.getLoanReference() + " payment: " + paymentAmount + " " + loan.getCurrency(),
            "Loan",
            savedLoan.getId().toString()
        );

        return savedLoan;
    }

    public Loan updateLoanStatus(Long loanId, LoanStatus newStatus) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found with ID: " + loanId));

        LoanStatus oldStatus = loan.getStatus();
        loan.setStatus(newStatus);

        Loan savedLoan = loanRepository.save(loan);

        auditService.logEntityAction(
            loan.getUser().getId(),
            loan.getUser().getUsername(),
            AuditAction.LOAN_STATUS_CHANGED,
            AuditSeverity.MEDIUM,
            "Loan status updated from " + oldStatus + " to " + newStatus + " - " + loan.getLoanReference(),
            "Loan",
            savedLoan.getId().toString()
        );

        return savedLoan;
    }

    public Loan markAsDelinquent(Long loanId, Integer daysDelinquent) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found with ID: " + loanId));

        loan.setDaysDelinquent(daysDelinquent);

        if (daysDelinquent > 90) {
            loan.setStatus(LoanStatus.DEFAULTED);
        }

        Loan savedLoan = loanRepository.save(loan);

        auditService.logEntityAction(
            loan.getUser().getId(),
            "SYSTEM",
            AuditAction.LOAN_MARKED_DELINQUENT,
            AuditSeverity.HIGH,
            "Loan marked delinquent - " + loan.getLoanReference() + " days: " + daysDelinquent,
            "Loan",
            savedLoan.getId().toString()
        );

        return savedLoan;
    }

    public void deleteLoan(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found with ID: " + loanId));

        if (loan.getStatus() == LoanStatus.ACTIVE || loan.getStatus() == LoanStatus.APPROVED) {
            throw new IllegalStateException("Cannot delete active or approved loans");
        }

        auditService.logEntityAction(
            loan.getUser().getId(),
            loan.getUser().getUsername(),
            AuditAction.LOAN_DELETED,
            AuditSeverity.HIGH,
            "Loan deleted - " + loan.getLoanReference(),
            "Loan",
            loan.getId().toString()
        );

        loanRepository.delete(loan);
    }

    // Finder methods
    public Optional<Loan> findById(Long id) {
        return loanRepository.findById(id);
    }

    public Optional<Loan> findByReference(String reference) {
        return loanRepository.findByLoanReference(reference);
    }

    public List<Loan> findLoansByUser(User user) {
        return loanRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public List<Loan> findActiveLoansByUser(User user) {
        return loanRepository.findByUserAndStatusOrderByCreatedAtDesc(user, LoanStatus.ACTIVE);
    }

    public List<Loan> findLoansByStatus(LoanStatus status) {
        return loanRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public List<Loan> findLoansByType(LoanType loanType) {
        return loanRepository.findByLoanTypeOrderByCreatedAtDesc(loanType);
    }

    public List<Loan> findDelinquentLoans() {
        return loanRepository.findByStatusAndDaysDelinquentGreaterThanOrderByDaysDelinquentDesc(LoanStatus.ACTIVE, 0);
    }

    public List<Loan> findLoansMaturingBetween(LocalDate startDate, LocalDate endDate) {
        return loanRepository.findByMaturityDateBetweenOrderByMaturityDateAsc(startDate, endDate);
    }

    // Get credit score for a user
    public CreditScoringService.CreditScore getCreditScore(User user) {
        return creditScoringService.calculateCreditScore(user);
    }

    // Get loan approval decision without creating loan
    public CreditScoringService.LoanApprovalDecision previewLoanDecision(User user, BigDecimal loanAmount, LoanType loanType) {
        return creditScoringService.shouldApproveLoan(user, loanAmount, loanType);
    }

    // Helper methods
    private String generateLoanReference() {
        String reference;
        do {
            reference = "LN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (loanRepository.existsByLoanReference(reference));
        return reference;
    }

    private LocalDate calculateFirstPaymentDate(LocalDate applicationDate, BillingFrequency frequency) {
        return switch (frequency) {
            case WEEKLY -> applicationDate.plusWeeks(1);
            case MONTHLY -> applicationDate.plusMonths(1);
            case QUARTERLY -> applicationDate.plusMonths(3);
            case SEMI_ANNUALLY -> applicationDate.plusMonths(6);
            case ANNUALLY -> applicationDate.plusYears(1);
            default -> applicationDate.plusMonths(1);
        };
    }

    private LocalDate calculateMaturityDate(LocalDate applicationDate, Integer termMonths) {
        return applicationDate.plusMonths(termMonths);
    }
}