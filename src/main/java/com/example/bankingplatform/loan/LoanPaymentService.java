package com.example.bankingplatform.loan;

import com.example.bankingplatform.account.Account;
import com.example.bankingplatform.account.AccountRepository;
import com.example.bankingplatform.audit.AuditAction;
import com.example.bankingplatform.audit.AuditService;
import com.example.bankingplatform.audit.AuditSeverity;
import com.example.bankingplatform.billing.BillingFrequency;
import com.example.bankingplatform.transaction.Transaction;
import com.example.bankingplatform.transaction.TransactionChannel;
import com.example.bankingplatform.transaction.TransactionService;
import com.example.bankingplatform.transaction.TransactionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class LoanPaymentService {

    private final LoanRepository loanRepository;
    private final AccountRepository accountRepository;
    private final TransactionService transactionService;
    private final AuditService auditService;

    @Autowired
    public LoanPaymentService(LoanRepository loanRepository,
                             AccountRepository accountRepository,
                             TransactionService transactionService,
                             AuditService auditService) {
        this.loanRepository = loanRepository;
        this.accountRepository = accountRepository;
        this.transactionService = transactionService;
        this.auditService = auditService;
    }

    // Run daily at 2 AM to process loan payments and interest
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void processScheduledLoanPayments() {
        System.out.println("Starting scheduled loan payment processing...");

        List<Loan> activeLoans = loanRepository.findByStatus(LoanStatus.ACTIVE);

        for (Loan loan : activeLoans) {
            try {
                processLoanPayment(loan);
            } catch (Exception e) {
                System.err.println("Error processing payment for loan " + loan.getId() + ": " + e.getMessage());
                auditService.logFailedAction(
                    loan.getUser().getId(),
                    "SYSTEM",
                    AuditAction.LOAN_PAYMENT_PROCESSED,
                    AuditSeverity.HIGH,
                    "Failed to process automatic payment for loan " + loan.getLoanReference(),
                    e.getMessage()
                );
            }
        }

        System.out.println("Completed scheduled loan payment processing.");
    }

    @Transactional
    public void processLoanPayment(Loan loan) {
        if (loan.getStatus() != LoanStatus.ACTIVE) {
            return; // Only process active loans
        }

        LocalDate today = LocalDate.now();
        LocalDate nextPaymentDate = calculateNextPaymentDate(loan);

        // Check if payment is due
        if (today.isBefore(nextPaymentDate)) {
            return; // Payment not due yet
        }

        // Calculate interest accrued since last payment
        BigDecimal interestAmount = calculateInterestAccrued(loan, today);

        // Principal payment = monthly payment - interest
        BigDecimal principalPayment = loan.getMonthlyPayment().subtract(interestAmount);

        // Ensure we don't pay more principal than remaining balance
        if (principalPayment.compareTo(loan.getCurrentBalance()) > 0) {
            principalPayment = loan.getCurrentBalance();
            // Recalculate total payment for final payment
            BigDecimal totalPayment = principalPayment.add(interestAmount);
            loan.setMonthlyPayment(totalPayment);
        }

        // Check if borrower has sufficient funds
        Account disbursementAccount = loan.getDisbursementAccount();
        if (disbursementAccount == null || disbursementAccount.getBalance().compareTo(loan.getMonthlyPayment()) < 0) {
            handleInsufficientFunds(loan, today);
            return;
        }

        // Process the payment
        processPaymentTransaction(loan, interestAmount, principalPayment, today);

        // Update loan details
        updateLoanAfterPayment(loan, interestAmount, principalPayment, today);

        loanRepository.save(loan);

        auditService.logEntityAction(
            loan.getUser().getId(),
            "SYSTEM",
            AuditAction.LOAN_PAYMENT_PROCESSED,
            AuditSeverity.MEDIUM,
            "Automatic loan payment processed - Principal: " + principalPayment +
            ", Interest: " + interestAmount + ", Total: " + loan.getMonthlyPayment(),
            "Loan",
            loan.getId().toString()
        );
    }

    private LocalDate calculateNextPaymentDate(Loan loan) {
        if (loan.getLastPaymentDate() == null) {
            return loan.getFirstPaymentDate();
        }

        return switch (loan.getPaymentFrequency()) {
            case ONE_TIME -> loan.getLastPaymentDate(); // One-time payment doesn't repeat
            case WEEKLY -> loan.getLastPaymentDate().plusWeeks(1);
            case MONTHLY -> loan.getLastPaymentDate().plusMonths(1);
            case QUARTERLY -> loan.getLastPaymentDate().plusMonths(3);
            case SEMI_ANNUALLY -> loan.getLastPaymentDate().plusMonths(6);
            case ANNUALLY -> loan.getLastPaymentDate().plusYears(1);
        };
    }

    private BigDecimal calculateInterestAccrued(Loan loan, LocalDate paymentDate) {
        LocalDate lastPaymentDate = loan.getLastPaymentDate() != null ?
            loan.getLastPaymentDate() : loan.getDisbursementDate();

        if (lastPaymentDate == null) {
            lastPaymentDate = loan.getApplicationDate();
        }

        long daysBetween = ChronoUnit.DAYS.between(lastPaymentDate, paymentDate);

        // Calculate daily interest rate
        BigDecimal annualRate = loan.getInterestRate();
        BigDecimal dailyRate = annualRate.divide(new BigDecimal("365"), 10, RoundingMode.HALF_UP);

        // Interest = Principal × Daily Rate × Days
        BigDecimal interestAccrued = loan.getCurrentBalance()
            .multiply(dailyRate)
            .multiply(new BigDecimal(daysBetween))
            .setScale(2, RoundingMode.HALF_UP);

        return interestAccrued;
    }

    private void handleInsufficientFunds(Loan loan, LocalDate today) {
        // Calculate days delinquent
        LocalDate expectedPaymentDate = calculateNextPaymentDate(loan);
        long daysLate = ChronoUnit.DAYS.between(expectedPaymentDate, today);

        loan.setDaysDelinquent((int) daysLate);

        // Calculate late fees (e.g., $25 + 5% of payment amount)
        BigDecimal lateFee = new BigDecimal("25.00")
            .add(loan.getMonthlyPayment().multiply(new BigDecimal("0.05")));

        loan.setLateFeesAccrued(loan.getLateFeesAccrued().add(lateFee));

        // Mark as defaulted if more than 30 days late
        if (daysLate > 30) {
            loan.setStatus(LoanStatus.DEFAULTED);
        }

        loanRepository.save(loan);

        auditService.logEntityAction(
            loan.getUser().getId(),
            "SYSTEM",
            AuditAction.TRANSACTION_FAILED,
            AuditSeverity.HIGH,
            "Loan payment failed due to insufficient funds - Days late: " + daysLate +
            ", Late fee added: " + lateFee,
            "Loan",
            loan.getId().toString()
        );
    }

    private void processPaymentTransaction(Loan loan, BigDecimal interestAmount,
                                         BigDecimal principalPayment, LocalDate paymentDate) {
        try {
            // Create a withdrawal transaction for the loan payment
            transactionService.createWithdrawal(
                loan.getDisbursementAccount().getId(),
                loan.getMonthlyPayment(),
                loan.getUser().getId(),
                "Automatic loan payment - Loan " + loan.getLoanReference() +
                " (Principal: " + principalPayment + ", Interest: " + interestAmount + ")",
                TransactionChannel.ONLINE_BANKING // Automatic system payment
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to process payment transaction: " + e.getMessage(), e);
        }
    }

    private void updateLoanAfterPayment(Loan loan, BigDecimal interestAmount,
                                       BigDecimal principalPayment, LocalDate paymentDate) {
        // Update loan balance and payment tracking
        loan.setCurrentBalance(loan.getCurrentBalance().subtract(principalPayment));
        loan.setTotalPaidAmount(loan.getTotalPaidAmount().add(loan.getMonthlyPayment()));
        loan.setTotalInterestPaid(loan.getTotalInterestPaid().add(interestAmount));
        loan.setPaymentsMade(loan.getPaymentsMade() + 1);
        loan.setLastPaymentDate(paymentDate);

        // Calculate remaining payments
        if (loan.getPaymentsRemaining() != null && loan.getPaymentsRemaining() > 0) {
            loan.setPaymentsRemaining(loan.getPaymentsRemaining() - 1);
        }

        // Reset delinquency if payment successful
        loan.setDaysDelinquent(0);

        // Check if loan is paid off
        if (loan.getCurrentBalance().compareTo(BigDecimal.ZERO) <= 0) {
            loan.setCurrentBalance(BigDecimal.ZERO);
            loan.setStatus(LoanStatus.CLOSED);
            loan.setPaymentsRemaining(0);
        }
    }

    // Manual payment processing method
    @Transactional
    public Loan processManualPayment(Long loanId, BigDecimal paymentAmount, String description) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new RuntimeException("Loan not found: " + loanId));

        if (loan.getStatus() != LoanStatus.ACTIVE && loan.getStatus() != LoanStatus.DEFAULTED && loan.getStatus() != LoanStatus.APPROVED) {
            throw new RuntimeException("Cannot process payment for loan with status: " + loan.getStatus());
        }

        // Check if borrower has sufficient funds
        Account account = loan.getDisbursementAccount();
        if (account.getBalance().compareTo(paymentAmount) < 0) {
            throw new RuntimeException("Insufficient funds for payment");
        }

        LocalDate today = LocalDate.now();
        BigDecimal interestAmount = calculateInterestAccrued(loan, today);
        BigDecimal principalPayment = paymentAmount.subtract(interestAmount);

        if (principalPayment.compareTo(BigDecimal.ZERO) < 0) {
            principalPayment = BigDecimal.ZERO;
            interestAmount = paymentAmount; // Payment goes entirely to interest
        }

        // Ensure we don't overpay principal
        if (principalPayment.compareTo(loan.getCurrentBalance()) > 0) {
            principalPayment = loan.getCurrentBalance();
        }

        // Process the payment transaction
        processPaymentTransaction(loan, interestAmount, principalPayment, today);

        // Update loan details
        updateLoanAfterPayment(loan, interestAmount, principalPayment, today);

        Loan savedLoan = loanRepository.save(loan);

        auditService.logEntityAction(
            loan.getUser().getId(),
            loan.getUser().getUsername(),
            AuditAction.LOAN_PAYMENT_PROCESSED,
            AuditSeverity.MEDIUM,
            "Manual loan payment processed - " + description +
            " (Principal: " + principalPayment + ", Interest: " + interestAmount + ")",
            "Loan",
            loan.getId().toString()
        );

        return savedLoan;
    }
}