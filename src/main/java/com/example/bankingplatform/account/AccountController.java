package com.example.bankingplatform.account;

import com.example.bankingplatform.user.User;
import com.example.bankingplatform.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    private final UserRepository userRepository;

    @Autowired
    public AccountController(AccountService accountService, UserRepository userRepository) {
        this.accountService = accountService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<?> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        try {
            // Find user by ID
            User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + request.getUserId()));

            Account account = accountService.createAccount(
                user,
                request.getAccountType(),
                request.getAccountName(),
                request.getInitialBalance(),
                request.getCurrency()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(account);
        } catch (Exception e) {
            e.printStackTrace(); // Log the full stack trace
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Account> getAccount(@PathVariable Integer id) {
        return accountService.findById(id)
            .map(account -> ResponseEntity.ok(account))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/number/{accountNum}")
    public ResponseEntity<Account> getAccountByNumber(@PathVariable String accountNum) {
        return accountService.findByAccountNumber(accountNum)
            .map(account -> ResponseEntity.ok(account))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or authentication.name == @userRepository.findById(#userId).get().username")
    public ResponseEntity<List<Account>> getAccountsByUser(@PathVariable Integer userId) {
        User user = new User();
        user.setId(userId);

        List<Account> accounts = accountService.findAccountsByUser(user);
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/user/{userId}/active")
    @PreAuthorize("hasRole('ADMIN') or authentication.name == @userRepository.findById(#userId).get().username")
    public ResponseEntity<List<Account>> getActiveAccountsByUser(@PathVariable Integer userId) {
        User user = new User();
        user.setId(userId);

        List<Account> accounts = accountService.findActiveAccountsByUser(user);
        return ResponseEntity.ok(accounts);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Account> updateAccountStatus(
            @PathVariable Integer id,
            @RequestBody UpdateStatusRequest request) {
        try {
            Account account = accountService.updateAccountStatus(id, request.getStatus());
            return ResponseEntity.ok(account);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // REMOVED: Direct balance update endpoint - balances should only change through transactions
    // Use /api/transactions/deposit or /api/transactions/transfer instead

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Integer id) {
        try {
            accountService.deleteAccount(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Admin endpoint to get all accounts system-wide
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Account>> getAllAccounts() {
        try {
            List<Account> accounts = accountService.findAllAccounts();
            return ResponseEntity.ok(accounts);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Request DTOs
    public static class CreateAccountRequest {
        @NotNull
        private Integer userId;
        @NotNull
        private AccountType accountType;
        @NotBlank
        private String accountName;
        private BigDecimal initialBalance;
        @NotBlank
        private String currency;

        // Getters and setters
        public Integer getUserId() { return userId; }
        public void setUserId(Integer userId) { this.userId = userId; }

        public AccountType getAccountType() { return accountType; }
        public void setAccountType(AccountType accountType) { this.accountType = accountType; }

        public String getAccountName() { return accountName; }
        public void setAccountName(String accountName) { this.accountName = accountName; }

        public BigDecimal getInitialBalance() { return initialBalance; }
        public void setInitialBalance(BigDecimal initialBalance) { this.initialBalance = initialBalance; }

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
    }

    public static class UpdateStatusRequest {
        private AccountStatus status;

        public AccountStatus getStatus() { return status; }
        public void setStatus(AccountStatus status) { this.status = status; }
    }

    // REMOVED: UpdateBalanceRequest - no longer needed since direct balance updates are disabled
}