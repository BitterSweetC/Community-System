package com.cloud.community.club.service;

import com.cloud.community.core.entity.Club;
import com.cloud.community.core.entity.Member;

import java.util.List;

public interface ClubService {
    Club createClub(Club club, Long userId);
    List<Club> getAllClubs();
    List<Club> getMyClubs(Long userId); // Clubs where user is admin/member or creator
    Club getClubById(Long id);
    void approveClub(Long clubId);
    Club updateClub(Long id, Club club);
    List<Club> searchClubs(String keyword);
    List<Club> searchClubs(String keyword, String category);

    /**
     * Get recommended clubs based on user interests or popularity.
     * @param userId current user ID (nullable)
     * @return list of recommended clubs
     */
    List<Club> getRecommendedClubs(Long userId);
    
    // Pagination
    org.springframework.data.domain.Page<Club> searchClubs(String keyword, String category, int page, int size);
    
    // Admin
    List<Club> getPendingClubs();
    void deleteClub(Long id);
    
    // Member management
    void addMember(Long clubId, Long userId, String role);
    void updateMemberRole(Long clubId, Long userId, String role);
    void removeMember(Long clubId, Long userId);
    List<Member> getClubMembers(Long clubId);

    // Dissolution
    void applyDissolution(Long clubId, Long userId, String reason);
    void withdrawDissolution(Long clubId, Long userId);
    void forceDissolve(Long clubId, Long adminId, String reason);
    void recoverClub(Long clubId, Long adminId);
}
