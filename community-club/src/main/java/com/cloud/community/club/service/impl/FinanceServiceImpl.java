package com.cloud.community.club.service.impl;

import com.cloud.community.club.service.FinanceService;
import com.cloud.community.core.entity.Club;
import com.cloud.community.core.entity.ClubFinance;
import com.cloud.community.core.repository.ClubFinanceRepository;
import com.cloud.community.core.repository.ClubRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FinanceServiceImpl implements FinanceService {

    private final ClubFinanceRepository financeRepository;
    private final ClubRepository clubRepository;

    @Override
    public boolean hasPendingTransactions(Long clubId) {
        return financeRepository.existsByClubIdAndStatus(clubId, "PENDING");
    }

    @Override
    @Transactional
    public ClubFinance createTransaction(ClubFinance transaction) {
        transaction.setStatus("PENDING");
        return financeRepository.save(transaction);
    }

    @Override
    @Transactional
    public void approveTransaction(Long transactionId, Long approverId) {
        ClubFinance transaction = financeRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        
        if (!"PENDING".equals(transaction.getStatus())) {
            throw new RuntimeException("Transaction is not pending");
        }

        transaction.setStatus("APPROVED");
        transaction.setApproverId(approverId);
        financeRepository.save(transaction);

        // Update club balance
        Club club = transaction.getClub();
        BigDecimal amount = transaction.getAmount();
        BigDecimal currentBalance = club.getBalance() == null ? BigDecimal.ZERO : club.getBalance();
        
        if ("EXPENSE".equals(transaction.getType())) {
            club.setBalance(currentBalance.subtract(amount));
        } else {
            club.setBalance(currentBalance.add(amount));
        }
        clubRepository.save(club);
    }

    @Override
    @Transactional
    public void rejectTransaction(Long transactionId, Long approverId) {
        ClubFinance transaction = financeRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (!"PENDING".equals(transaction.getStatus())) {
            throw new RuntimeException("Transaction is not pending");
        }

        transaction.setStatus("REJECTED");
        transaction.setApproverId(approverId);
        financeRepository.save(transaction);
    }

    @Override
    public List<ClubFinance> getClubTransactions(Long clubId) {
        return financeRepository.findByClubId(clubId);
    }

    @Override
    public BigDecimal getClubBalance(Long clubId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new RuntimeException("Club not found"));
        return club.getBalance() == null ? BigDecimal.ZERO : club.getBalance();
    }
}
