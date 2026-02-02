package com.cloud.community.club.service;

public interface FinanceService {
    /**
     * Check if the club has any unfinished financial transactions.
     * @param clubId Club ID
     * @return true if there are pending transactions, false otherwise.
     */
    boolean hasPendingTransactions(Long clubId);
}
