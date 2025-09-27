package com.example.bankingplatform.billing;

import com.example.bankingplatform.account.Account;
import com.example.bankingplatform.account.AccountRepository;
import com.example.bankingplatform.user.User;
import com.example.bankingplatform.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/billings")
public class BillingController {

    private final BillingService billingService;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    @Autowired
    public BillingController(BillingService billingService, UserRepository userRepository, AccountRepository accountRepository) {
        this.billingService = billingService;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
    }

    @PostMapping
    public ResponseEntity<?> createBilling(@Valid @RequestBody CreateBillingRequest request) {
        try {
            User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + request.getUserId()));

            Account account = null;
            if (request.getAccountId() != null) {
                account = accountRepository.findById(request.getAccountId())
                    .orElseThrow(() -> new RuntimeException("Account not found with ID: " + request.getAccountId()));
            }

            Billing billing = billingService.createBilling(
                user,
                account,
                request.getBillingType(),
                request.getAmount(),
                request.getDescription(),
                request.getDueDate()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(billing);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/subscription")
    public ResponseEntity<?> createSubscription(@Valid @RequestBody CreateSubscriptionRequest request) {
        try {
            User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + request.getUserId()));

            Account account = null;
            if (request.getAccountId() != null) {
                account = accountRepository.findById(request.getAccountId())
                    .orElseThrow(() -> new RuntimeException("Account not found with ID: " + request.getAccountId()));
            }

            Billing billing = billingService.createSubscription(
                user,
                account,
                request.getAmount(),
                request.getDescription(),
                request.getFrequency(),
                request.getStartDate(),
                request.getEndDate()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(billing);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Billing> getBilling(@PathVariable Long id) {
        return billingService.findById(id)
            .map(billing -> ResponseEntity.ok(billing))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/reference/{reference}")
    public ResponseEntity<Billing> getBillingByReference(@PathVariable String reference) {
        return billingService.findByReference(reference)
            .map(billing -> ResponseEntity.ok(billing))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Billing>> getBillingsByUser(@PathVariable Integer userId) {
        User user = new User();
        user.setId(userId);

        List<Billing> billings = billingService.findBillingsByUser(user);
        return ResponseEntity.ok(billings);
    }

    @GetMapping("/user/{userId}/unpaid")
    public ResponseEntity<List<Billing>> getUnpaidBillingsByUser(@PathVariable Integer userId) {
        User user = new User();
        user.setId(userId);

        List<Billing> billings = billingService.findUnpaidBillingsByUser(user);
        return ResponseEntity.ok(billings);
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<Billing>> getBillingsByAccount(@PathVariable Integer accountId) {
        Account account = new Account();
        account.setId(accountId);

        List<Billing> billings = billingService.findBillingsByAccount(account);
        return ResponseEntity.ok(billings);
    }

    @GetMapping("/type/{billingType}")
    public ResponseEntity<List<Billing>> getBillingsByType(@PathVariable BillingType billingType) {
        List<Billing> billings = billingService.findBillingsByType(billingType);
        return ResponseEntity.ok(billings);
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<Billing>> getOverdueBillings() {
        List<Billing> billings = billingService.findOverdueBillings();
        return ResponseEntity.ok(billings);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Billing> updateBillingStatus(
            @PathVariable Long id,
            @RequestBody UpdateStatusRequest request) {
        try {
            Billing billing = billingService.updateBillingStatus(id, request.getStatus());
            return ResponseEntity.ok(billing);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/payment")
    public ResponseEntity<?> processPayment(
            @PathVariable Long id,
            @RequestBody ProcessPaymentRequest request) {
        try {
            Billing billing = billingService.processPayment(id, request.getPaymentAmount(), request.getTransactionId());
            return ResponseEntity.ok(billing);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // New endpoint for realistic payment processing from account
    @PostMapping("/{id}/pay-from-account")
    public ResponseEntity<?> payFromAccount(
            @PathVariable Long id,
            @RequestBody PayFromAccountRequest request) {
        try {
            Billing billing = billingService.processPaymentFromAccount(
                id,
                request.getAccountId(),
                request.getPaymentAmount()
            );
            return ResponseEntity.ok(billing);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/discount")
    public ResponseEntity<Billing> applyDiscount(
            @PathVariable Long id,
            @RequestBody ApplyDiscountRequest request) {
        try {
            Billing billing = billingService.applyDiscount(id, request.getDiscountAmount());
            return ResponseEntity.ok(billing);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBilling(@PathVariable Long id) {
        try {
            billingService.deleteBilling(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Request DTOs
    public static class CreateBillingRequest {
        private Integer userId;
        private Integer accountId;
        private BillingType billingType;
        private BigDecimal amount;
        private String description;
        private LocalDate dueDate;

        public Integer getUserId() { return userId; }
        public void setUserId(Integer userId) { this.userId = userId; }

        public Integer getAccountId() { return accountId; }
        public void setAccountId(Integer accountId) { this.accountId = accountId; }

        public BillingType getBillingType() { return billingType; }
        public void setBillingType(BillingType billingType) { this.billingType = billingType; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public LocalDate getDueDate() { return dueDate; }
        public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    }

    public static class CreateSubscriptionRequest {
        private Integer userId;
        private Integer accountId;
        private BigDecimal amount;
        private String description;
        private BillingFrequency frequency;
        private LocalDate startDate;
        private LocalDate endDate;

        public Integer getUserId() { return userId; }
        public void setUserId(Integer userId) { this.userId = userId; }

        public Integer getAccountId() { return accountId; }
        public void setAccountId(Integer accountId) { this.accountId = accountId; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public BillingFrequency getFrequency() { return frequency; }
        public void setFrequency(BillingFrequency frequency) { this.frequency = frequency; }

        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    }

    public static class UpdateStatusRequest {
        private BillingStatus status;

        public BillingStatus getStatus() { return status; }
        public void setStatus(BillingStatus status) { this.status = status; }
    }

    public static class ProcessPaymentRequest {
        private BigDecimal paymentAmount;
        private Long transactionId;

        public BigDecimal getPaymentAmount() { return paymentAmount; }
        public void setPaymentAmount(BigDecimal paymentAmount) { this.paymentAmount = paymentAmount; }

        public Long getTransactionId() { return transactionId; }
        public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }
    }

    public static class PayFromAccountRequest {
        private Long accountId;
        private BigDecimal paymentAmount;

        public Long getAccountId() { return accountId; }
        public void setAccountId(Long accountId) { this.accountId = accountId; }

        public BigDecimal getPaymentAmount() { return paymentAmount; }
        public void setPaymentAmount(BigDecimal paymentAmount) { this.paymentAmount = paymentAmount; }
    }

    public static class ApplyDiscountRequest {
        private BigDecimal discountAmount;

        public BigDecimal getDiscountAmount() { return discountAmount; }
        public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    }
}