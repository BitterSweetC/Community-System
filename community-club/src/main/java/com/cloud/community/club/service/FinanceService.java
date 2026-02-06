package com.cloud.community.club.service;

import com.cloud.community.core.entity.ClubFinance;
import java.math.BigDecimal;
import java.util.List;

public interface FinanceService {
    /**
     * Check if the club has any unfinished financial transactions.
     * @param clubId Club ID
     * @return true if there are pending transactions, false otherwise.
     */
    boolean hasPendingTransactions(Long clubId);

    ClubFinance createTransaction(ClubFinance transaction);
    void approveTransaction(Long transactionId, Long approverId);
    void rejectTransaction(Long transactionId, Long approverId);
    List<ClubFinance> getClubTransactions(Long clubId);
    BigDecimal getClubBalance(Long clubId);
}
