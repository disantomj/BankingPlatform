package com.example.bankingplatform.billing;

import com.example.bankingplatform.audit.AuditAction;
import com.example.bankingplatform.audit.AuditService;
import com.example.bankingplatform.audit.AuditSeverity;
import com.example.bankingplatform.loan.Loan;
import com.example.bankingplatform.loan.LoanRepository;
import com.example.bankingplatform.loan.LoanStatus;
import com.example.bankingplatform.notification.NotificationService;
import com.example.bankingplatform.notification.NotificationType;
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
public class BillingSchedulerService {

    private final BillingRepository billingRepository;
    private final BillingService billingService;
    private final LoanRepository loanRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Autowired
    public BillingSchedulerService(BillingRepository billingRepository,
                                  BillingService billingService,
                                  LoanRepository loanRepository,
                                  NotificationService notificationService,
                                  AuditService auditService) {
        this.billingRepository = billingRepository;
        this.billingService = billingService;
        this.loanRepository = loanRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    // Run daily at 1 AM to generate recurring bills and loan payment notifications
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void processScheduledBillingAndNotifications() {
        System.out.println("Starting scheduled billing and loan notification processing...");

        try {
            generateRecurringBills();
            generateLoanPaymentNotifications();
            markOverdueBills();
        } catch (Exception e) {
            System.err.println("Error in scheduled billing processing: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("Completed scheduled billing and loan notification processing.");
    }

    private void generateRecurringBills() {
        LocalDate today = LocalDate.now();

        // Find paid bills that should generate next billing
        List<Billing> paidRecurringBills = billingRepository.findByStatusAndFrequencyIsNotNull(BillingStatus.PAID);

        for (Billing paidBilling : paidRecurringBills) {
            try {
                if (shouldGenerateNextBill(paidBilling, today)) {
                    generateNextRecurringBill(paidBilling);
                }
            } catch (Exception e) {
                System.err.println("Error generating next bill for billing " + paidBilling.getId() + ": " + e.getMessage());
            }
        }
    }

    private boolean shouldGenerateNextBill(Billing paidBilling, LocalDate today) {
        if (paidBilling.getFrequency() == null || paidBilling.getFrequency() == BillingFrequency.ONE_TIME) {
            return false;
        }

        if (paidBilling.getBillingType() != BillingType.SUBSCRIPTION &&
            paidBilling.getBillingType() != BillingType.SERVICE_FEE &&
            paidBilling.getBillingType() != BillingType.MAINTENANCE_FEE) {
            return false;
        }

        LocalDate nextDueDate = calculateNextBillingDate(paidBilling.getDueDate(), paidBilling.getFrequency());

        // Generate bill 3 days before it's due (or on the due date)
        return today.isEqual(nextDueDate.minusDays(3)) || today.isEqual(nextDueDate) || today.isAfter(nextDueDate);
    }

    private void generateNextRecurringBill(Billing originalBilling) {
        LocalDate nextDueDate = calculateNextBillingDate(originalBilling.getDueDate(), originalBilling.getFrequency());

        // Check if subscription has ended
        if (originalBilling.getSubscriptionEndDate() != null &&
            nextDueDate.isAfter(originalBilling.getSubscriptionEndDate())) {
            return;
        }

        // Check if a bill for this period already exists
        if (billAlreadyExistsForPeriod(originalBilling, nextDueDate)) {
            return;
        }

        Billing nextBilling = new Billing(
            originalBilling.getBillingType(),
            originalBilling.getUser(),
            originalBilling.getAccount(),
            originalBilling.getAmount(),
            BillingStatus.PENDING,
            originalBilling.getDescription() + " - " + getFrequencyDescription(originalBilling.getFrequency()),
            LocalDate.now(),
            nextDueDate
        );

        nextBilling.setBillingReference(generateBillingReference());
        nextBilling.setFrequency(originalBilling.getFrequency());
        nextBilling.setSubscriptionStartDate(originalBilling.getSubscriptionStartDate());
        nextBilling.setSubscriptionEndDate(originalBilling.getSubscriptionEndDate());
        nextBilling.calculateTotalAmount();

        billingRepository.save(nextBilling);

        // Create notification for new bill
        notificationService.createNotification(
            originalBilling.getUser().getId(),
            NotificationType.BILLING,
            "New Bill Generated",
            "Your " + getFrequencyDescription(originalBilling.getFrequency()).toLowerCase() +
            " " + originalBilling.getDescription() + " bill of " +
            originalBilling.getAmount() + " is now due on " + nextDueDate,
            "/billing"
        );

        auditService.logEntityAction(
            originalBilling.getUser().getId(),
            "SYSTEM",
            AuditAction.BILLING_CREATED,
            AuditSeverity.LOW,
            "Recurring bill generated automatically - " + originalBilling.getBillingType() +
            " for " + originalBilling.getAmount(),
            "Billing",
            nextBilling.getId().toString()
        );
    }

    private void generateLoanPaymentNotifications() {
        LocalDate today = LocalDate.now();
        LocalDate notificationDate = today.plusDays(3); // Notify 3 days before payment due

        List<Loan> activeLoans = loanRepository.findByStatus(LoanStatus.ACTIVE);
        activeLoans.addAll(loanRepository.findByStatus(LoanStatus.APPROVED));

        for (Loan loan : activeLoans) {
            try {
                LocalDate nextPaymentDate = calculateLoanNextPaymentDate(loan);

                if (nextPaymentDate != null && (nextPaymentDate.equals(notificationDate) || nextPaymentDate.equals(today))) {
                    createLoanPaymentNotification(loan, nextPaymentDate);
                }
            } catch (Exception e) {
                System.err.println("Error creating loan payment notification for loan " + loan.getId() + ": " + e.getMessage());
            }
        }
    }

    private LocalDate calculateLoanNextPaymentDate(Loan loan) {
        if (loan.getLastPaymentDate() != null) {
            return loan.getLastPaymentDate().plusMonths(1); // Assuming monthly payments
        } else if (loan.getFirstPaymentDate() != null) {
            return loan.getFirstPaymentDate();
        } else if (loan.getDisbursementDate() != null) {
            return loan.getDisbursementDate().plusMonths(1);
        } else {
            return loan.getApplicationDate().plusMonths(1);
        }
    }

    private void createLoanPaymentNotification(Loan loan, LocalDate paymentDate) {
        BigDecimal monthlyPayment = loan.getMonthlyPayment();
        if (monthlyPayment == null) {
            monthlyPayment = calculateMonthlyPayment(loan.getPrincipalAmount(), loan.getInterestRate(), loan.getTermMonths());
        }

        String message = String.format(
            "Your loan payment of %s is due on %s. Current balance: %s. Click to make a payment.",
            formatCurrency(monthlyPayment),
            paymentDate,
            formatCurrency(loan.getCurrentBalance())
        );

        notificationService.createNotification(
            loan.getUser().getId(),
            NotificationType.LOAN_PAYMENT,
            "Loan Payment Due",
            message,
            "/loans"
        );

        auditService.logEntityAction(
            loan.getUser().getId(),
            "SYSTEM",
            AuditAction.NOTIFICATION_CREATED,
            AuditSeverity.LOW,
            "Loan payment notification created for loan " + loan.getLoanReference(),
            "Loan",
            loan.getId().toString()
        );
    }

    private void markOverdueBills() {
        LocalDate today = LocalDate.now();
        List<BillingStatus> unpaidStatuses = List.of(BillingStatus.PENDING, BillingStatus.SENT);
        List<Billing> overdueBills = billingRepository.findByStatusInAndDueDateBeforeOrderByDueDateDesc(unpaidStatuses, today);

        for (Billing bill : overdueBills) {
            bill.setStatus(BillingStatus.OVERDUE);
            billingRepository.save(bill);

            // Create overdue notification
            notificationService.createNotification(
                bill.getUser().getId(),
                NotificationType.BILLING,
                "Bill Overdue",
                "Your " + bill.getDescription() + " payment of " +
                formatCurrency(bill.getAmount()) + " is now overdue. Please pay immediately to avoid fees.",
                "/billing"
            );
        }
    }

    private boolean billAlreadyExistsForPeriod(Billing originalBilling, LocalDate nextDueDate) {
        // Check if there's already a bill for this user, type, and due date
        return billingRepository.existsByUserAndBillingTypeAndDueDate(
            originalBilling.getUser(),
            originalBilling.getBillingType(),
            nextDueDate
        );
    }

    private LocalDate calculateNextBillingDate(LocalDate dueDate, BillingFrequency frequency) {
        return switch (frequency) {
            case WEEKLY -> dueDate.plusWeeks(1);
            case MONTHLY -> dueDate.plusMonths(1);
            case QUARTERLY -> dueDate.plusMonths(3);
            case SEMI_ANNUALLY -> dueDate.plusMonths(6);
            case ANNUALLY -> dueDate.plusYears(1);
            default -> dueDate.plusMonths(1);
        };
    }

    private String getFrequencyDescription(BillingFrequency frequency) {
        return switch (frequency) {
            case WEEKLY -> "Weekly";
            case MONTHLY -> "Monthly";
            case QUARTERLY -> "Quarterly";
            case SEMI_ANNUALLY -> "Semi-Annual";
            case ANNUALLY -> "Annual";
            default -> "Monthly";
        };
    }

    private BigDecimal calculateMonthlyPayment(BigDecimal principal, BigDecimal rate, Integer termMonths) {
        if (rate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(new BigDecimal(termMonths), 2, RoundingMode.HALF_UP);
        }

        BigDecimal monthlyRate = rate.divide(new BigDecimal("12"), 10, RoundingMode.HALF_UP);
        BigDecimal factor = BigDecimal.ONE.add(monthlyRate).pow(termMonths);

        return principal.multiply(monthlyRate).multiply(factor)
                .divide(factor.subtract(BigDecimal.ONE), 2, RoundingMode.HALF_UP);
    }

    private String formatCurrency(BigDecimal amount) {
        return "$" + amount.setScale(2, RoundingMode.HALF_UP).toString();
    }

    private String generateBillingReference() {
        return "BILL-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }
}