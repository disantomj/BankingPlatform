package com.example.bankingplatform.loan;

import com.example.bankingplatform.account.Account;
import com.example.bankingplatform.account.AccountRepository;
import com.example.bankingplatform.transaction.Transaction;
import com.example.bankingplatform.transaction.TransactionRepository;
import com.example.bankingplatform.transaction.TransactionType;
import com.example.bankingplatform.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CreditScoringService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LoanRepository loanRepository;

    @Autowired
    public CreditScoringService(AccountRepository accountRepository,
                               TransactionRepository transactionRepository,
                               LoanRepository loanRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.loanRepository = loanRepository;
    }

    public CreditScore calculateCreditScore(User user) {
        CreditScore score = new CreditScore();
        score.setUserId(user.getId());

        // Get user's accounts and transaction history
        List<Account> accounts = accountRepository.findByUser(user);
        List<Transaction> transactions = transactionRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        List<Loan> loans = loanRepository.findByUserOrderByCreatedAtDesc(user);

        // Calculate various factors
        int accountHistoryScore = calculateAccountHistoryScore(accounts, transactions);
        int balanceStabilityScore = calculateBalanceStabilityScore(accounts, transactions);
        int transactionPatternScore = calculateTransactionPatternScore(transactions);
        int existingDebtScore = calculateExistingDebtScore(loans);
        int incomeStabilityScore = calculateIncomeStabilityScore(transactions);

        // Weighted average
        int finalScore = (int) Math.round(
            accountHistoryScore * 0.25 +
            balanceStabilityScore * 0.20 +
            transactionPatternScore * 0.20 +
            existingDebtScore * 0.25 +
            incomeStabilityScore * 0.10
        );

        score.setScore(Math.max(300, Math.min(850, finalScore))); // Clamp between 300-850
        score.setRiskLevel(determineRiskLevel(score.getScore()));
        score.setAccountHistoryScore(accountHistoryScore);
        score.setBalanceStabilityScore(balanceStabilityScore);
        score.setTransactionPatternScore(transactionPatternScore);
        score.setExistingDebtScore(existingDebtScore);
        score.setIncomeStabilityScore(incomeStabilityScore);

        return score;
    }

    private int calculateAccountHistoryScore(List<Account> accounts, List<Transaction> transactions) {
        if (accounts.isEmpty()) return 300;

        // Base score for having accounts
        int score = 500;

        // Bonus for multiple accounts
        if (accounts.size() > 1) score += 50;
        if (accounts.size() > 2) score += 25;

        // Account age bonus (based on oldest transaction)
        if (!transactions.isEmpty()) {
            LocalDateTime oldestTransaction = transactions.stream()
                .map(Transaction::getCreatedAt)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());

            long monthsOfHistory = java.time.temporal.ChronoUnit.MONTHS.between(oldestTransaction, LocalDateTime.now());

            if (monthsOfHistory > 24) score += 100; // 2+ years
            else if (monthsOfHistory > 12) score += 75; // 1+ year
            else if (monthsOfHistory > 6) score += 50; // 6+ months
            else if (monthsOfHistory > 3) score += 25; // 3+ months
        }

        return Math.min(850, score);
    }

    private int calculateBalanceStabilityScore(List<Account> accounts, List<Transaction> transactions) {
        if (accounts.isEmpty()) return 300;

        // Calculate average balance across all accounts
        BigDecimal totalBalance = accounts.stream()
            .map(Account::getBalance)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        int score = 400;

        // Higher balances = better score
        if (totalBalance.compareTo(new BigDecimal("10000")) >= 0) score += 150;
        else if (totalBalance.compareTo(new BigDecimal("5000")) >= 0) score += 100;
        else if (totalBalance.compareTo(new BigDecimal("1000")) >= 0) score += 75;
        else if (totalBalance.compareTo(new BigDecimal("500")) >= 0) score += 50;
        else if (totalBalance.compareTo(BigDecimal.ZERO) > 0) score += 25;

        // Penalty for negative balances or closed accounts
        long negativeBalanceAccounts = accounts.stream()
            .mapToLong(acc -> acc.getBalance().compareTo(BigDecimal.ZERO) < 0 ? 1 : 0)
            .sum();

        score -= (int) (negativeBalanceAccounts * 50);

        return Math.max(300, Math.min(850, score));
    }

    private int calculateTransactionPatternScore(List<Transaction> transactions) {
        if (transactions.isEmpty()) return 400;

        int score = 500;

        // Recent transaction activity (last 3 months)
        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
        long recentTransactions = transactions.stream()
            .filter(t -> t.getCreatedAt().isAfter(threeMonthsAgo))
            .count();

        // Regular activity is good
        if (recentTransactions > 50) score += 75;
        else if (recentTransactions > 20) score += 50;
        else if (recentTransactions > 10) score += 25;
        else if (recentTransactions < 3) score -= 50; // Very low activity

        return Math.max(300, Math.min(850, score));
    }

    private int calculateExistingDebtScore(List<Loan> loans) {
        int score = 700; // Start high, reduce for debt

        if (loans.isEmpty()) return score;

        // Calculate total outstanding debt
        BigDecimal totalDebt = loans.stream()
            .filter(loan -> loan.getStatus() == LoanStatus.ACTIVE)
            .map(Loan::getCurrentBalance)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Penalty for high debt levels
        if (totalDebt.compareTo(new BigDecimal("50000")) >= 0) score -= 200;
        else if (totalDebt.compareTo(new BigDecimal("20000")) >= 0) score -= 150;
        else if (totalDebt.compareTo(new BigDecimal("10000")) >= 0) score -= 100;
        else if (totalDebt.compareTo(new BigDecimal("5000")) >= 0) score -= 50;

        // Check for delinquent loans
        long delinquentLoans = loans.stream()
            .filter(loan -> loan.getStatus() == LoanStatus.ACTIVE)
            .filter(loan -> loan.getDaysDelinquent() > 0)
            .count();

        score -= (int) (delinquentLoans * 75);

        // Check for defaulted loans
        long defaultedLoans = loans.stream()
            .filter(loan -> loan.getStatus() == LoanStatus.DEFAULTED)
            .count();

        score -= (int) (defaultedLoans * 150);

        return Math.max(300, Math.min(850, score));
    }

    private int calculateIncomeStabilityScore(List<Transaction> transactions) {
        int score = 500;

        // Look for regular deposit patterns (income)
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
        List<Transaction> recentDeposits = transactions.stream()
            .filter(t -> t.getCreatedAt().isAfter(sixMonthsAgo))
            .filter(t -> t.getTransactionType() == TransactionType.DEPOSIT)
            .toList();

        if (recentDeposits.isEmpty()) return 400;

        // Calculate average monthly deposits
        BigDecimal totalDeposits = recentDeposits.stream()
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgMonthlyDeposits = totalDeposits.divide(new BigDecimal("6"), 2, RoundingMode.HALF_UP);

        // Higher regular income = better score
        if (avgMonthlyDeposits.compareTo(new BigDecimal("5000")) >= 0) score += 100;
        else if (avgMonthlyDeposits.compareTo(new BigDecimal("3000")) >= 0) score += 75;
        else if (avgMonthlyDeposits.compareTo(new BigDecimal("2000")) >= 0) score += 50;
        else if (avgMonthlyDeposits.compareTo(new BigDecimal("1000")) >= 0) score += 25;

        return Math.max(300, Math.min(850, score));
    }

    private RiskLevel determineRiskLevel(int score) {
        if (score >= 750) return RiskLevel.LOW;
        else if (score >= 650) return RiskLevel.MEDIUM;
        else if (score >= 550) return RiskLevel.HIGH;
        else return RiskLevel.VERY_HIGH;
    }

    public LoanApprovalDecision shouldApproveLoan(User user, BigDecimal loanAmount, LoanType loanType) {
        CreditScore creditScore = calculateCreditScore(user);

        // Calculate debt-to-income ratio
        List<Account> accounts = accountRepository.findByUser(user);
        BigDecimal totalBalance = accounts.stream()
            .map(Account::getBalance)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Loan> existingLoans = loanRepository.findByUserAndStatusOrderByCreatedAtDesc(user, LoanStatus.ACTIVE);
        BigDecimal existingDebt = existingLoans.stream()
            .map(Loan::getCurrentBalance)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate maximum loan amount based on account balance and credit score
        BigDecimal maxLoanAmount = calculateMaxLoanAmount(totalBalance, creditScore.getScore(), loanType);

        LoanApprovalDecision decision = new LoanApprovalDecision();
        decision.setCreditScore(creditScore);
        decision.setRequestedAmount(loanAmount);
        decision.setMaxApprovedAmount(maxLoanAmount);
        decision.setExistingDebt(existingDebt);
        decision.setTotalBalance(totalBalance);

        // Decision logic
        if (creditScore.getScore() < 500) {
            decision.setApproved(false);
            decision.setReason("Credit score too low (minimum 500 required)");
        } else if (loanAmount.compareTo(maxLoanAmount) > 0) {
            decision.setApproved(false);
            decision.setReason("Requested amount exceeds maximum approved amount of " + maxLoanAmount);
        } else if (creditScore.getRiskLevel() == RiskLevel.VERY_HIGH && loanAmount.compareTo(new BigDecimal("5000")) > 0) {
            decision.setApproved(false);
            decision.setReason("High risk profile limits loan amount to $5,000");
        } else {
            decision.setApproved(true);
            decision.setReason("Loan approved based on credit profile");
        }

        return decision;
    }

    private BigDecimal calculateMaxLoanAmount(BigDecimal totalBalance, int creditScore, LoanType loanType) {
        // Base multiplier based on credit score
        BigDecimal multiplier = switch (creditScore / 50) {
            case 17, 16, 15 -> new BigDecimal("10.0"); // 750-850: 10x balance
            case 14, 13 -> new BigDecimal("7.5"); // 650-749: 7.5x balance
            case 12, 11 -> new BigDecimal("5.0"); // 550-649: 5x balance
            case 10 -> new BigDecimal("3.0"); // 500-549: 3x balance
            default -> new BigDecimal("1.0"); // <500: 1x balance
        };

        // Adjust for loan type
        BigDecimal typeMultiplier = switch (loanType) {
            case PERSONAL -> new BigDecimal("1.0");
            case AUTO -> new BigDecimal("1.5");
            case MORTGAGE -> new BigDecimal("3.0");
            case BUSINESS -> new BigDecimal("2.0");
            case STUDENT -> new BigDecimal("1.2");
            default -> new BigDecimal("1.0");
        };

        BigDecimal maxAmount = totalBalance.multiply(multiplier).multiply(typeMultiplier);

        // Cap amounts by loan type
        BigDecimal cap = switch (loanType) {
            case PERSONAL -> new BigDecimal("50000");
            case AUTO -> new BigDecimal("100000");
            case MORTGAGE -> new BigDecimal("500000");
            case BUSINESS -> new BigDecimal("250000");
            case STUDENT -> new BigDecimal("75000");
            default -> new BigDecimal("25000");
        };

        return maxAmount.min(cap);
    }

    public static class CreditScore {
        private Integer userId;
        private int score;
        private RiskLevel riskLevel;
        private int accountHistoryScore;
        private int balanceStabilityScore;
        private int transactionPatternScore;
        private int existingDebtScore;
        private int incomeStabilityScore;

        // Getters and setters
        public Integer getUserId() { return userId; }
        public void setUserId(Integer userId) { this.userId = userId; }

        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }

        public RiskLevel getRiskLevel() { return riskLevel; }
        public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }

        public int getAccountHistoryScore() { return accountHistoryScore; }
        public void setAccountHistoryScore(int accountHistoryScore) { this.accountHistoryScore = accountHistoryScore; }

        public int getBalanceStabilityScore() { return balanceStabilityScore; }
        public void setBalanceStabilityScore(int balanceStabilityScore) { this.balanceStabilityScore = balanceStabilityScore; }

        public int getTransactionPatternScore() { return transactionPatternScore; }
        public void setTransactionPatternScore(int transactionPatternScore) { this.transactionPatternScore = transactionPatternScore; }

        public int getExistingDebtScore() { return existingDebtScore; }
        public void setExistingDebtScore(int existingDebtScore) { this.existingDebtScore = existingDebtScore; }

        public int getIncomeStabilityScore() { return incomeStabilityScore; }
        public void setIncomeStabilityScore(int incomeStabilityScore) { this.incomeStabilityScore = incomeStabilityScore; }
    }

    public static class LoanApprovalDecision {
        private boolean approved;
        private String reason;
        private CreditScore creditScore;
        private BigDecimal requestedAmount;
        private BigDecimal maxApprovedAmount;
        private BigDecimal existingDebt;
        private BigDecimal totalBalance;

        // Getters and setters
        public boolean isApproved() { return approved; }
        public void setApproved(boolean approved) { this.approved = approved; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }

        public CreditScore getCreditScore() { return creditScore; }
        public void setCreditScore(CreditScore creditScore) { this.creditScore = creditScore; }

        public BigDecimal getRequestedAmount() { return requestedAmount; }
        public void setRequestedAmount(BigDecimal requestedAmount) { this.requestedAmount = requestedAmount; }

        public BigDecimal getMaxApprovedAmount() { return maxApprovedAmount; }
        public void setMaxApprovedAmount(BigDecimal maxApprovedAmount) { this.maxApprovedAmount = maxApprovedAmount; }

        public BigDecimal getExistingDebt() { return existingDebt; }
        public void setExistingDebt(BigDecimal existingDebt) { this.existingDebt = existingDebt; }

        public BigDecimal getTotalBalance() { return totalBalance; }
        public void setTotalBalance(BigDecimal totalBalance) { this.totalBalance = totalBalance; }
    }

    public enum RiskLevel {
        LOW, MEDIUM, HIGH, VERY_HIGH
    }
}