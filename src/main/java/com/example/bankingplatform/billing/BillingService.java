package com.example.bankingplatform.billing;

import com.example.bankingplatform.account.Account;
import com.example.bankingplatform.audit.AuditAction;
import com.example.bankingplatform.audit.AuditService;
import com.example.bankingplatform.audit.AuditSeverity;
import com.example.bankingplatform.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class BillingService {

    private final BillingRepository billingRepository;
    private final AuditService auditService;

    @Autowired
    public BillingService(BillingRepository billingRepository, AuditService auditService) {
        this.billingRepository = billingRepository;
        this.auditService = auditService;
    }

    public Billing createBilling(User user, Account account, BillingType billingType, BigDecimal amount,
                                String description, LocalDate dueDate) {
        String billingReference = generateBillingReference();

        Billing billing = new Billing(billingType, user, account, amount, BillingStatus.PENDING,
                                     description, LocalDate.now(), dueDate);
        billing.setBillingReference(billingReference);
        billing.calculateTotalAmount();

        Billing savedBilling = billingRepository.save(billing);

        auditService.logEntityAction(
            user.getId(),
            user.getUsername(),
            AuditAction.BILLING_CREATED,
            AuditSeverity.MEDIUM,
            "Billing created - " + billingType + " for " + amount + " " + billing.getCurrency(),
            "Billing",
            savedBilling.getId().toString()
        );

        return savedBilling;
    }

    public Billing createSubscription(User user, Account account, BigDecimal amount, String description,
                                     BillingFrequency frequency, LocalDate startDate, LocalDate endDate) {
        LocalDate firstDueDate = calculateNextBillingDate(startDate, frequency);
        Billing billing = createBilling(user, account, BillingType.SUBSCRIPTION, amount, description, firstDueDate);

        billing.setFrequency(frequency);
        billing.setSubscriptionStartDate(startDate);
        billing.setSubscriptionEndDate(endDate);
        billing.setNextBillingDate(calculateNextBillingDate(firstDueDate, frequency));

        return billingRepository.save(billing);
    }

    public Billing processPayment(Long billingId, BigDecimal paymentAmount, Long transactionId) {
        Billing billing = billingRepository.findById(billingId)
            .orElseThrow(() -> new RuntimeException("Billing not found with id: " + billingId));

        if (billing.getStatus() == BillingStatus.PAID) {
            throw new RuntimeException("Billing is already paid");
        }

        BigDecimal oldPaidAmount = billing.getPaidAmount() != null ? billing.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal newPaidAmount = oldPaidAmount.add(paymentAmount);

        billing.setPaidAmount(newPaidAmount);
        billing.setPaymentTransactionId(transactionId);

        if (billing.isFullyPaid()) {
            billing.setStatus(BillingStatus.PAID);
            billing.setPaidDate(LocalDate.now());

            if (billing.getFrequency() != null && billing.getFrequency() != BillingFrequency.ONE_TIME) {
                generateNextRecurringBill(billing);
            }
        }

        Billing savedBilling = billingRepository.save(billing);

        auditService.logChange(
            billing.getUser().getId(),
            billing.getUser().getUsername(),
            AuditAction.BILLING_PAYMENT_PROCESSED,
            AuditSeverity.MEDIUM,
            "Payment processed for billing - Amount: " + paymentAmount + " " + billing.getCurrency(),
            "Billing",
            billingId.toString(),
            oldPaidAmount.toString(),
            newPaidAmount.toString()
        );

        return savedBilling;
    }

    public Billing updateBillingStatus(Long billingId, BillingStatus newStatus) {
        Billing billing = billingRepository.findById(billingId)
            .orElseThrow(() -> new RuntimeException("Billing not found with id: " + billingId));

        BillingStatus oldStatus = billing.getStatus();
        billing.setStatus(newStatus);

        if (newStatus == BillingStatus.CANCELLED) {
            billing.setPaidDate(null);
            billing.setPaymentTransactionId(null);
        }

        Billing savedBilling = billingRepository.save(billing);

        auditService.logChange(
            billing.getUser().getId(),
            billing.getUser().getUsername(),
            AuditAction.BILLING_STATUS_CHANGED,
            AuditSeverity.MEDIUM,
            "Billing status changed from " + oldStatus + " to " + newStatus,
            "Billing",
            billingId.toString(),
            oldStatus.toString(),
            newStatus.toString()
        );

        return savedBilling;
    }

    public Billing applyDiscount(Long billingId, BigDecimal discountAmount) {
        Billing billing = billingRepository.findById(billingId)
            .orElseThrow(() -> new RuntimeException("Billing not found with id: " + billingId));

        if (billing.getStatus() == BillingStatus.PAID) {
            throw new IllegalStateException("Cannot apply discount to paid billing");
        }

        BigDecimal oldTotal = billing.getTotalAmount();
        billing.setDiscountAmount(discountAmount);
        billing.calculateTotalAmount();

        Billing savedBilling = billingRepository.save(billing);

        auditService.logChange(
            billing.getUser().getId(),
            billing.getUser().getUsername(),
            AuditAction.BILLING_DISCOUNT_APPLIED,
            AuditSeverity.MEDIUM,
            "Discount applied to billing - Amount: " + discountAmount + " " + billing.getCurrency(),
            "Billing",
            billingId.toString(),
            oldTotal.toString(),
            billing.getTotalAmount().toString()
        );

        return savedBilling;
    }

    public void deleteBilling(Long billingId) {
        Billing billing = billingRepository.findById(billingId)
            .orElseThrow(() -> new RuntimeException("Billing not found with id: " + billingId));

        if (billing.getStatus() == BillingStatus.PAID) {
            throw new IllegalStateException("Cannot delete paid billing");
        }

        auditService.logEntityAction(
            billing.getUser().getId(),
            billing.getUser().getUsername(),
            AuditAction.BILLING_DELETED,
            AuditSeverity.HIGH,
            "Billing deleted - " + billing.getBillingType() + " for " + billing.getAmount() + " " + billing.getCurrency(),
            "Billing",
            billingId.toString()
        );

        billingRepository.delete(billing);
    }

    @Transactional(readOnly = true)
    public Optional<Billing> findById(Long id) {
        return billingRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Billing> findByReference(String billingReference) {
        return billingRepository.findByBillingReference(billingReference);
    }

    @Transactional(readOnly = true)
    public List<Billing> findBillingsByUser(User user) {
        return billingRepository.findByUserOrderByDueDateDesc(user);
    }

    @Transactional(readOnly = true)
    public List<Billing> findUnpaidBillingsByUser(User user) {
        List<BillingStatus> unpaidStatuses = Arrays.asList(BillingStatus.PENDING, BillingStatus.SENT, BillingStatus.OVERDUE);
        return billingRepository.findByUserAndStatusInOrderByDueDateAsc(user, unpaidStatuses);
    }

    @Transactional(readOnly = true)
    public List<Billing> findBillingsByAccount(Account account) {
        return billingRepository.findByAccountOrderByDueDateDesc(account);
    }

    @Transactional(readOnly = true)
    public List<Billing> findBillingsByType(BillingType billingType) {
        return billingRepository.findByBillingTypeOrderByDueDateDesc(billingType);
    }

    @Transactional(readOnly = true)
    public List<Billing> findOverdueBillings() {
        return billingRepository.findByStatusOrderByDueDateDesc(BillingStatus.OVERDUE);
    }

    private void generateNextRecurringBill(Billing originalBilling) {
        LocalDate nextDueDate = originalBilling.getNextBillingDate();

        if (originalBilling.getSubscriptionEndDate() != null &&
            nextDueDate.isAfter(originalBilling.getSubscriptionEndDate())) {
            return;
        }

        Billing nextBilling = createBilling(
            originalBilling.getUser(),
            originalBilling.getAccount(),
            originalBilling.getBillingType(),
            originalBilling.getAmount(),
            originalBilling.getDescription(),
            nextDueDate
        );

        nextBilling.setFrequency(originalBilling.getFrequency());
        nextBilling.setSubscriptionStartDate(originalBilling.getSubscriptionStartDate());
        nextBilling.setSubscriptionEndDate(originalBilling.getSubscriptionEndDate());

        originalBilling.setNextBillingDate(calculateNextBillingDate(nextDueDate, originalBilling.getFrequency()));
        billingRepository.save(originalBilling);
        billingRepository.save(nextBilling);
    }

    private LocalDate calculateNextBillingDate(LocalDate currentDate, BillingFrequency frequency) {
        return switch (frequency) {
            case WEEKLY -> currentDate.plusWeeks(1);
            case MONTHLY -> currentDate.plusMonths(1);
            case QUARTERLY -> currentDate.plusMonths(3);
            case SEMI_ANNUALLY -> currentDate.plusMonths(6);
            case ANNUALLY -> currentDate.plusYears(1);
            default -> currentDate;
        };
    }

    private String generateBillingReference() {
        String reference;
        do {
            reference = "BILL" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        } while (billingRepository.existsByBillingReference(reference));
        return reference;
    }
}