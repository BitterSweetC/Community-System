package com.cloud.community.club.service.impl;

import com.cloud.community.club.service.FinanceService;
import com.cloud.community.core.entity.Club;
import com.cloud.community.core.entity.ClubFinance;
import com.cloud.community.core.exception.BusinessException;
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
        if ("EXPENSE".equals(transaction.getType())) {
            Club club = clubRepository.findById(transaction.getClubId())
                    .orElseThrow(() -> new RuntimeException("Club not found"));
            BigDecimal balance = club.getBalance() == null ? BigDecimal.ZERO : club.getBalance();
            if (balance.compareTo(transaction.getAmount()) < 0) {
                throw new RuntimeException("社团余额不足，当前余额：" + balance);
            }
        }
        transaction.setStatus("PENDING");
        return financeRepository.save(transaction);
    }

    @Override
    @Transactional
    public void approveTransaction(Long transactionId, Long approverId) {
        // Lock transaction to prevent double approval
        ClubFinance transaction = financeRepository.findByIdForUpdate(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        
        if (!"PENDING".equals(transaction.getStatus())) {
            throw new BusinessException(40931, "该财务申请已处理，请勿重复审批");
        }

        transaction.setStatus("APPROVED");
        transaction.setApproverId(approverId);
        financeRepository.save(transaction);

        // Lock club to safely update balance
        Club club = clubRepository.findByIdForUpdate(transaction.getClubId())
                .orElseThrow(() -> new RuntimeException("Club not found"));
        
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
        // Lock transaction to prevent double rejection
        ClubFinance transaction = financeRepository.findByIdForUpdate(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (!"PENDING".equals(transaction.getStatus())) {
            throw new BusinessException(40931, "该财务申请已处理，请勿重复审批");
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
