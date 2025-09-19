package com.example.bankingplatform.account;

import com.example.bankingplatform.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Integer> {

    Optional<Account> findByAccountNum(String accountNum);

    List<Account> findByUser(User user);

    List<Account> findByUserAndStatus(User user, AccountStatus status);

    List<Account> findByAccountType(AccountType accountType);

    boolean existsByAccountNum(String accountNum);
}