package com.cloud.community.club.task;

import com.cloud.community.core.entity.Club;
import com.cloud.community.core.repository.ClubRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ClubCleanupTask {

    private final ClubRepository clubRepository;

    @Scheduled(cron = "0 0 0 * * ?") // Daily at midnight
    @Transactional
    public void processClubDissolution() {
        log.info("Starting club dissolution cleanup task...");

        // 1. Process cooling-off period (7 days)
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<Club> dissolvingClubs = clubRepository.findByStatusAndDissolutionDateBefore(Club.STATUS_DISSOLVING, sevenDaysAgo);
        
        for (Club club : dissolvingClubs) {
            clubRepository.findByIdForUpdate(club.getId()).ifPresent(lockedClub -> {
                if (!Club.STATUS_DISSOLVING.equals(lockedClub.getStatus())) {
                    return;
                }
                if (lockedClub.getDissolutionDate() == null || lockedClub.getDissolutionDate().isAfter(sevenDaysAgo)) {
                    return;
                }
                log.info("Club {} ({}) cooling-off period ended. Moving to DISSOLVED.", lockedClub.getName(), lockedClub.getId());
                lockedClub.setStatus(Club.STATUS_DISSOLVED);
                // Update date to start the 30-day retention countdown
                lockedClub.setDissolutionDate(LocalDateTime.now());
                clubRepository.save(lockedClub);
            });
        }

        // 2. Process soft-deleted clubs (30 days)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<Club> dissolvedClubs = clubRepository.findByStatusAndDissolutionDateBefore(Club.STATUS_DISSOLVED, thirtyDaysAgo);

        for (Club club : dissolvedClubs) {
            try {
                clubRepository.findByIdForUpdate(club.getId()).ifPresent(lockedClub -> {
                    if (!Club.STATUS_DISSOLVED.equals(lockedClub.getStatus())) {
                        return;
                    }
                    if (lockedClub.getDissolutionDate() == null || lockedClub.getDissolutionDate().isAfter(thirtyDaysAgo)) {
                        return;
                    }
                    log.info("Club {} ({}) has been dissolved for 30 days. Performing hard delete.", lockedClub.getName(), lockedClub.getId());
                    // Hard delete
                    clubRepository.delete(lockedClub);
                });
            } catch (Exception e) {
                log.error("Failed to delete club {}: {}", club.getId(), e.getMessage());
            }
        }
        
        log.info("Club dissolution cleanup task finished.");
    }
}
