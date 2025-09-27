package com.example.bankingplatform.billing;

import com.example.bankingplatform.account.Account;
import com.example.bankingplatform.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillingRepository extends JpaRepository<Billing, Long> {

    // Find by reference
    Optional<Billing> findByBillingReference(String billingReference);
    boolean existsByBillingReference(String billingReference);

    // Find by user
    List<Billing> findByUserOrderByDueDateDesc(User user);
    List<Billing> findByUserAndStatusOrderByDueDateDesc(User user, BillingStatus status);

    // Find by account
    List<Billing> findByAccountOrderByDueDateDesc(Account account);
    List<Billing> findByAccountAndStatusOrderByDueDateDesc(Account account, BillingStatus status);

    // Find by status
    List<Billing> findByStatusOrderByDueDateDesc(BillingStatus status);

    // Find by billing type
    List<Billing> findByBillingTypeOrderByDueDateDesc(BillingType billingType);
    List<Billing> findByUserAndBillingTypeOrderByDueDateDesc(User user, BillingType billingType);

    // Find by date ranges
    List<Billing> findByDueDateBetweenOrderByDueDateDesc(LocalDate startDate, LocalDate endDate);
    List<Billing> findByIssueDateBetweenOrderByDueDateDesc(LocalDate startDate, LocalDate endDate);

    // Find overdue bills
    List<Billing> findByStatusNotAndDueDateBeforeOrderByDueDateDesc(BillingStatus status, LocalDate date);

    // Find by user and date range
    List<Billing> findByUserAndDueDateBetweenOrderByDueDateDesc(User user, LocalDate startDate, LocalDate endDate);
    List<Billing> findByUserAndIssueDateBetweenOrderByDueDateDesc(User user, LocalDate startDate, LocalDate endDate);

    // Find upcoming bills
    List<Billing> findByStatusAndDueDateBetweenOrderByDueDateAsc(BillingStatus status, LocalDate startDate, LocalDate endDate);

    // Find recurring bills due for next billing
    List<Billing> findByFrequencyNotNullAndNextBillingDateBeforeOrderByNextBillingDateAsc(LocalDate date);

    // Find by invoice number
    Optional<Billing> findByInvoiceNumber(String invoiceNumber);

    // Find recent bills for user
    List<Billing> findTop10ByUserOrderByCreatedAtDesc(User user);

    // Find unpaid bills for user
    List<Billing> findByUserAndStatusInOrderByDueDateAsc(User user, List<BillingStatus> statuses);

    // Find bills by subscription period
    List<Billing> findBySubscriptionStartDateLessThanEqualAndSubscriptionEndDateGreaterThanEqualOrderByDueDateDesc(
        LocalDate startDate, LocalDate endDate);

    // Find by payment transaction
    Optional<Billing> findByPaymentTransactionId(Long transactionId);

    // Find overdue bills
    List<Billing> findByStatusInAndDueDateBeforeOrderByDueDateDesc(List<BillingStatus> statuses, LocalDate date);

    // Check if bill already exists for period
    boolean existsByUserAndBillingTypeAndDueDate(User user, BillingType billingType, LocalDate dueDate);

    // Find paid recurring bills
    List<Billing> findByStatusAndFrequencyIsNotNull(BillingStatus status);
}