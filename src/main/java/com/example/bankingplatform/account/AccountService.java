package com.example.bankingplatform.account;

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

    @Autowired
    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
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

        return accountRepository.save(account);
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

        account.setStatus(newStatus);

        if (newStatus == AccountStatus.CLOSED) {
            account.setClosedAt(LocalDateTime.now());
        }

        return accountRepository.save(account);
    }

    public Account updateBalance(Integer accountId, BigDecimal newBalance) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found with id: " + accountId));

        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Balance cannot be negative");
        }

        account.setBalance(newBalance);
        account.setAvailableBalance(newBalance); // Simplified - would need more complex logic for holds, etc.

        return accountRepository.save(account);
    }

    public void deleteAccount(Integer accountId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found with id: " + accountId));

        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalStateException("Cannot delete account with non-zero balance");
        }

        accountRepository.delete(account);
    }

    private String generateAccountNumber() {
        String accountNum;
        do {
            accountNum = "ACC" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        } while (accountRepository.existsByAccountNum(accountNum));

        return accountNum;
    }
}