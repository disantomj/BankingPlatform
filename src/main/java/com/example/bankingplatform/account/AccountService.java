package com.example.bankingplatform.account;

import com.example.bankingplatform.audit.AuditAction;
import com.example.bankingplatform.audit.AuditService;
import com.example.bankingplatform.audit.AuditSeverity;
import com.example.bankingplatform.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AccountService {

    private final AccountRepository accountRepository;
    private final AuditService auditService;

    @Autowired
    public AccountService(AccountRepository accountRepository, AuditService auditService) {
        this.accountRepository = accountRepository;
        this.auditService = auditService;
    }

    public Account createAccount(User user, AccountType accountType, String accountName,
                               BigDecimal initialBalance, String currency) {
        String accountNum = generateAccountNumber();

        Account account = new Account(
            accountNum,
            accountType,
            user,
            initialBalance,
            initialBalance, // availableBalance equals balance initially
            BigDecimal.ZERO, // minimumBalance
            BigDecimal.ZERO, // overdraftLimit
            AccountStatus.PENDING_APPROVAL,
            accountName,
            currency
        );

        Account savedAccount = accountRepository.save(account);

        // Log account creation
        auditService.logEntityAction(
            user.getId(),
            user.getUsername(),
            AuditAction.ACCOUNT_CREATED,
            AuditSeverity.MEDIUM,
            "Account created successfully - " + accountType + " account",
            "Account",
            savedAccount.getId().toString()
        );

        return savedAccount;
    }

    @Transactional(readOnly = true)
    public Optional<Account> findById(Integer id) {
        return accountRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Account> findByAccountNumber(String accountNum) {
        return accountRepository.findByAccountNum(accountNum);
    }

    @Transactional(readOnly = true)
    public List<Account> findAccountsByUser(User user) {
        return accountRepository.findByUser(user);
    }

    @Transactional(readOnly = true)
    public List<Account> findActiveAccountsByUser(User user) {
        return accountRepository.findByUserAndStatus(user, AccountStatus.ACTIVE);
    }

    public Account updateAccountStatus(Integer accountId, AccountStatus newStatus) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found with id: " + accountId));

        AccountStatus oldStatus = account.getStatus();
        account.setStatus(newStatus);

        if (newStatus == AccountStatus.CLOSED) {
            account.setClosedAt(LocalDateTime.now());
        }

        Account savedAccount = accountRepository.save(account);

        // Log status change
        auditService.logChange(
            account.getUser().getId(),
            account.getUser().getUsername(),
            AuditAction.ACCOUNT_STATUS_CHANGED,
            AuditSeverity.MEDIUM,
            "Account status changed from " + oldStatus + " to " + newStatus,
            "Account",
            accountId.toString(),
            oldStatus.toString(),
            newStatus.toString()
        );

        return savedAccount;
    }

    public Account updateBalance(Integer accountId, BigDecimal newBalance) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found with id: " + accountId));

        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Balance cannot be negative");
        }

        BigDecimal oldBalance = account.getBalance();
        account.setBalance(newBalance);
        account.setAvailableBalance(newBalance); // Simplified - would need more complex logic for holds, etc.

        Account savedAccount = accountRepository.save(account);

        // Log balance update
        auditService.logChange(
            account.getUser().getId(),
            account.getUser().getUsername(),
            AuditAction.BALANCE_UPDATED,
            AuditSeverity.HIGH,
            "Account balance updated from " + oldBalance + " to " + newBalance,
            "Account",
            accountId.toString(),
            oldBalance.toString(),
            newBalance.toString()
        );

        return savedAccount;
    }

    public void deleteAccount(Integer accountId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found with id: " + accountId));

        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalStateException("Cannot delete account with non-zero balance");
        }

        // Log account deletion
        auditService.logEntityAction(
            account.getUser().getId(),
            account.getUser().getUsername(),
            AuditAction.ACCOUNT_DELETED,
            AuditSeverity.HIGH,
            "Account deleted - " + account.getAccountType() + " account " + account.getAccountNum(),
            "Account",
            accountId.toString()
        );

        accountRepository.delete(account);
    }

    public List<Account> findAllAccounts() {
        return accountRepository.findAll();
    }

    private String generateAccountNumber() {
        String accountNum;
        do {
            accountNum = "ACC" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        } while (accountRepository.existsByAccountNum(accountNum));

        return accountNum;
    }
}