package com.example.bankingplatform.loan;

import com.example.bankingplatform.account.Account;
import com.example.bankingplatform.account.AccountRepository;
import com.example.bankingplatform.billing.BillingFrequency;
import com.example.bankingplatform.user.User;
import com.example.bankingplatform.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;
    private final LoanPaymentService loanPaymentService;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    @Autowired
    public LoanController(LoanService loanService, LoanPaymentService loanPaymentService, UserRepository userRepository, AccountRepository accountRepository) {
        this.loanService = loanService;
        this.loanPaymentService = loanPaymentService;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
    }

    @PostMapping
    public ResponseEntity<?> createLoanApplication(@Valid @RequestBody CreateLoanRequest request) {
        try {
            User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + request.getUserId()));

            Account account = null;
            if (request.getDisbursementAccountId() != null) {
                account = accountRepository.findById(request.getDisbursementAccountId())
                    .orElseThrow(() -> new RuntimeException("Account not found with ID: " + request.getDisbursementAccountId()));
            }

            Loan loan = loanService.createLoanApplication(
                user,
                account,
                request.getLoanType(),
                request.getPrincipalAmount(),
                request.getInterestRate(),
                request.getTermMonths(),
                request.getPaymentFrequency(),
                request.getPurpose()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(loan);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Loan> getLoan(@PathVariable Long id) {
        return loanService.findById(id)
            .map(loan -> ResponseEntity.ok(loan))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/reference/{reference}")
    public ResponseEntity<Loan> getLoanByReference(@PathVariable String reference) {
        return loanService.findByReference(reference)
            .map(loan -> ResponseEntity.ok(loan))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Loan>> getLoansByUser(@PathVariable Integer userId) {
        User user = new User();
        user.setId(userId);

        List<Loan> loans = loanService.findLoansByUser(user);
        return ResponseEntity.ok(loans);
    }

    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<Loan>> getActiveLoansByUser(@PathVariable Integer userId) {
        User user = new User();
        user.setId(userId);

        List<Loan> loans = loanService.findActiveLoansByUser(user);
        return ResponseEntity.ok(loans);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Loan>> getLoansByStatus(@PathVariable LoanStatus status) {
        List<Loan> loans = loanService.findLoansByStatus(status);
        return ResponseEntity.ok(loans);
    }

    @GetMapping("/type/{loanType}")
    public ResponseEntity<List<Loan>> getLoansByType(@PathVariable LoanType loanType) {
        List<Loan> loans = loanService.findLoansByType(loanType);
        return ResponseEntity.ok(loans);
    }

    @GetMapping("/delinquent")
    public ResponseEntity<List<Loan>> getDelinquentLoans() {
        List<Loan> loans = loanService.findDelinquentLoans();
        return ResponseEntity.ok(loans);
    }

    @GetMapping("/maturing")
    public ResponseEntity<List<Loan>> getLoansMaturingSoon(
            @RequestParam(defaultValue = "0") int days) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(days > 0 ? days : 30);

        List<Loan> loans = loanService.findLoansMaturingBetween(startDate, endDate);
        return ResponseEntity.ok(loans);
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approveLoan(@PathVariable Long id, @RequestBody ApprovalRequest request) {
        try {
            Loan loan = loanService.approveLoan(id, request.getApprovedBy());
            return ResponseEntity.ok(loan);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectLoan(@PathVariable Long id, @RequestBody ApprovalRequest request) {
        try {
            Loan loan = loanService.rejectLoan(id, request.getApprovedBy());
            return ResponseEntity.ok(loan);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/disburse")
    public ResponseEntity<?> disburseLoan(@PathVariable Long id, @RequestBody ApprovalRequest request) {
        try {
            Loan loan = loanService.disburseLoan(id, request.getApprovedBy());
            return ResponseEntity.ok(loan);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/payment")
    public ResponseEntity<?> processPayment(@PathVariable Long id, @RequestBody PaymentRequest request) {
        try {
            Loan loan = loanService.processPayment(id, request.getPaymentAmount(), request.getTransactionId());
            return ResponseEntity.ok(loan);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateLoanStatus(@PathVariable Long id, @RequestBody StatusUpdateRequest request) {
        try {
            Loan loan = loanService.updateLoanStatus(id, request.getStatus());
            return ResponseEntity.ok(loan);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/delinquent")
    public ResponseEntity<?> markAsDelinquent(@PathVariable Long id, @RequestBody DelinquencyRequest request) {
        try {
            Loan loan = loanService.markAsDelinquent(id, request.getDaysDelinquent());
            return ResponseEntity.ok(loan);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLoan(@PathVariable Long id) {
        try {
            loanService.deleteLoan(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Credit scoring endpoints
    @GetMapping("/user/{userId}/credit-score")
    public ResponseEntity<?> getCreditScore(@PathVariable Integer userId) {
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

            CreditScoringService.CreditScore creditScore = loanService.getCreditScore(user);
            return ResponseEntity.ok(creditScore);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/preview")
    public ResponseEntity<?> previewLoanDecision(@Valid @RequestBody LoanPreviewRequest request) {
        try {
            User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + request.getUserId()));

            CreditScoringService.LoanApprovalDecision decision = loanService.previewLoanDecision(
                user, request.getLoanAmount(), request.getLoanType());
            return ResponseEntity.ok(decision);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // Request DTOs
    public static class CreateLoanRequest {
        private Integer userId;
        private Integer disbursementAccountId;
        private LoanType loanType;
        private BigDecimal principalAmount;
        private BigDecimal interestRate;
        private Integer termMonths;
        private BillingFrequency paymentFrequency;
        private String purpose;

        public Integer getUserId() { return userId; }
        public void setUserId(Integer userId) { this.userId = userId; }

        public Integer getDisbursementAccountId() { return disbursementAccountId; }
        public void setDisbursementAccountId(Integer disbursementAccountId) { this.disbursementAccountId = disbursementAccountId; }

        public LoanType getLoanType() { return loanType; }
        public void setLoanType(LoanType loanType) { this.loanType = loanType; }

        public BigDecimal getPrincipalAmount() { return principalAmount; }
        public void setPrincipalAmount(BigDecimal principalAmount) { this.principalAmount = principalAmount; }

        public BigDecimal getInterestRate() { return interestRate; }
        public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }

        public Integer getTermMonths() { return termMonths; }
        public void setTermMonths(Integer termMonths) { this.termMonths = termMonths; }

        public BillingFrequency getPaymentFrequency() { return paymentFrequency; }
        public void setPaymentFrequency(BillingFrequency paymentFrequency) { this.paymentFrequency = paymentFrequency; }

        public String getPurpose() { return purpose; }
        public void setPurpose(String purpose) { this.purpose = purpose; }
    }

    public static class ApprovalRequest {
        private String approvedBy;

        public String getApprovedBy() { return approvedBy; }
        public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    }

    public static class PaymentRequest {
        private BigDecimal paymentAmount;
        private Long transactionId;

        public BigDecimal getPaymentAmount() { return paymentAmount; }
        public void setPaymentAmount(BigDecimal paymentAmount) { this.paymentAmount = paymentAmount; }

        public Long getTransactionId() { return transactionId; }
        public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }
    }

    public static class StatusUpdateRequest {
        private LoanStatus status;

        public LoanStatus getStatus() { return status; }
        public void setStatus(LoanStatus status) { this.status = status; }
    }

    public static class DelinquencyRequest {
        private Integer daysDelinquent;

        public Integer getDaysDelinquent() { return daysDelinquent; }
        public void setDaysDelinquent(Integer daysDelinquent) { this.daysDelinquent = daysDelinquent; }
    }

    public static class LoanPreviewRequest {
        private Integer userId;
        private BigDecimal loanAmount;
        private LoanType loanType;

        public Integer getUserId() { return userId; }
        public void setUserId(Integer userId) { this.userId = userId; }

        public BigDecimal getLoanAmount() { return loanAmount; }
        public void setLoanAmount(BigDecimal loanAmount) { this.loanAmount = loanAmount; }

        public LoanType getLoanType() { return loanType; }
        public void setLoanType(LoanType loanType) { this.loanType = loanType; }
    }

    // Manual payment processing endpoint
    @PostMapping("/{id}/make-payment")
    public ResponseEntity<?> makeLoanPayment(@PathVariable Long id, @RequestBody LoanPaymentRequest request) {
        try {
            Loan loan = loanPaymentService.processManualPayment(id, request.getPaymentAmount(), request.getDescription());
            return ResponseEntity.ok(loan);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    public static class LoanPaymentRequest {
        private BigDecimal paymentAmount;
        private String description;

        public BigDecimal getPaymentAmount() { return paymentAmount; }
        public void setPaymentAmount(BigDecimal paymentAmount) { this.paymentAmount = paymentAmount; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}