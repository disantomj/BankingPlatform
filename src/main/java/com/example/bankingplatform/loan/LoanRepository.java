package com.example.bankingplatform.loan;

import com.example.bankingplatform.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    Optional<Loan> findByLoanReference(String loanReference);

    boolean existsByLoanReference(String loanReference);

    List<Loan> findByUserOrderByCreatedAtDesc(User user);

    List<Loan> findByUserAndStatusOrderByCreatedAtDesc(User user, LoanStatus status);

    List<Loan> findByStatusOrderByCreatedAtDesc(LoanStatus status);

    List<Loan> findByLoanTypeOrderByCreatedAtDesc(LoanType loanType);

    List<Loan> findByLoanTypeAndStatusOrderByCreatedAtDesc(LoanType loanType, LoanStatus status);

    List<Loan> findByMaturityDateBetweenOrderByMaturityDateAsc(LocalDate startDate, LocalDate endDate);

    List<Loan> findByApplicationDateBetweenOrderByCreatedAtDesc(LocalDate startDate, LocalDate endDate);

    List<Loan> findByStatusAndDaysDelinquentGreaterThanOrderByDaysDelinquentDesc(LoanStatus status, Integer days);

    List<Loan> findByUserAndStatusAndDaysDelinquentGreaterThanOrderByDaysDelinquentDesc(User user, LoanStatus status, Integer days);

    List<Loan> findTop10ByUserOrderByCreatedAtDesc(User user);

    List<Loan> findByRiskGradeOrderByCreatedAtDesc(String riskGrade);
}