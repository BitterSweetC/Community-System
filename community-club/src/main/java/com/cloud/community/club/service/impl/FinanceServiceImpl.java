package com.cloud.community.club.service.impl;

import com.cloud.community.club.service.FinanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FinanceServiceImpl implements FinanceService {

    @Override
    public boolean hasPendingTransactions(Long clubId) {
        log.info("Checking financial transactions for club {}", clubId);
        // Mock implementation: always return false (no pending transactions)
        return false;
    }
}
