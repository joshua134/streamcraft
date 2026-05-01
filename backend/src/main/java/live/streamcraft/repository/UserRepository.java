package live.streamcraft.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import live.streamcraft.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    
    Optional<User> findByEmail(String email);
    Optional<User> findByUname(String uname);
    
    boolean existsByEmail(String email);
    boolean existsByUname(String uname);
    
    Optional<User> findByEmailVerificationCode(String code);
    Optional<User> findByPasswordResetCode(String code);
    
    
    @Query("SELECT CASE WHEN COUNT(s) < 4 THEN true ELSE false END FROM Stream s " +
           "WHERE s.streamer.id = :userId AND s.createdAt > :since AND s.deleted = false")
    boolean canCreateFreeStream(@Param("userId") String userId, @Param("since") LocalDateTime since);
    
    @Query("SELECT COUNT(s) FROM Stream s WHERE s.streamer.id = :userId " +
           "AND DATE(s.createdAt) = CURRENT_DATE AND s.deleted = false")
    long countTodayStreams(@Param("userId") String userId);
    
    
    @Query("SELECT u FROM User u WHERE u.subscriptionEndDate BETWEEN :start AND :end AND u.deleted = false AND u.enabled = true")
    List<User> findUsersWithSubscriptionEndingBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    @Query("SELECT u FROM User u WHERE u.subscriptionEndDate IS NOT NULL AND u.subscriptionEndDate < :now AND u.deleted = false")
    Page<User> findExpiredSubscriptions(@Param("now") LocalDateTime now, Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE u.subscriptionEndDate IS NOT NULL AND u.subscriptionEndDate > :now AND u.deleted = false")
    Page<User> findActiveSubscribers(@Param("now") LocalDateTime now, Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE u.subscriptionEndDate IS NOT NULL " +
           "AND u.subscriptionEndDate > :now AND u.deleted = false AND u.enabled = true")
    List<User> findActiveSubscribers(@Param("now") LocalDateTime now);
    
    @Query("SELECT u FROM User u WHERE u.subscriptionEndDate BETWEEN :start AND :end AND u.enabled = true AND u.deleted = false")
    List<User> findUsersWithSubscriptionExpiringBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    
    @Modifying
    @Query("UPDATE User u SET u.enabled = true WHERE u.deleted = false AND u.emailVerificationCode = :code AND u.emailVerificationCodeExpiry > :now")
    int verifyUser(@Param("code") String code, @Param("now") LocalDateTime now);
    
    @Modifying
    @Query("UPDATE User u SET u.password = :password, u.passwordResetCode = null, u.passwordResetCodeExpiry = null WHERE u.passwordResetCode = :code")
    int resetPassword(@Param("code") String code, @Param("password") String password);
    
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.streamsCreatedCount = u.streamsCreatedCount + 1 WHERE u.id = :userId")
    void incrementStreamCount(@Param("userId") String userId);
    
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.deleted = true, u.deletedAt = :now WHERE u.enabled = false " +
           "AND u.createdAt < :cutoffDate AND u.deleted = false")
    int softDeleteUnverifiedAccounts(@Param("cutoffDate") LocalDateTime cutoffDate, @Param("now") LocalDateTime now);
    
    
    @Query("SELECT u FROM User u WHERE u.enabled = true AND u.deleted = false")
    Page<User> findByEnabledTrue(Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE u.enabled = false AND u.deleted = false")
    Page<User> findByEnabledFalse(Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE u.locked = true AND u.deleted = false")
    Page<User> findByLockedTrue(Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE u.locked = false AND u.deleted = false")
    Page<User> findByLockedFalse(Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE u.subscriptionEndDate IS NOT NULL AND u.subscriptionEndDate > :date AND u.deleted = false")
    Page<User> findBySubscriptionEndDateAfter(@Param("date") LocalDateTime date, Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE u.role.name = :roleName AND u.deleted = false")
    Page<User> findByRoleName(@Param("roleName") String roleName, Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE u.deleted = false")
    Page<User> findAllActive(Pageable pageable);
    
    
    @Query("SELECT u FROM User u WHERE (LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.uname) LIKE LOWER(CONCAT('%', :search, '%'))) AND u.deleted = false")
    List<User> findByEmailContainingOrUnameContaining(@Param("search") String search);
    
    @Query("SELECT u FROM User u WHERE u.email LIKE %:email% OR u.uname LIKE %:uname%")
    List<User> findByEmailContainingOrUnameContaining(@Param("email") String email, @Param("uname") String uname);
    
    
    @Query("SELECT u FROM User u WHERE u.createdAt >= :since AND u.deleted = false ORDER BY u.createdAt DESC")
    List<User> findRecentUsers(@Param("since") LocalDateTime since, Pageable pageable);
    
    Page<User> findByCreatedAtAfter(LocalDateTime thirtyDaysAgo, Pageable pageable);
    
    Page<User> findByEnabledFalseAndEmailVerificationCodeIsNotNull(Pageable pageable);
    
    
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.enabled = true AND u.locked = false AND u.deleted = false")
    Optional<User> findActiveUserByEmail(@Param("email") String email);
    
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.deleted = false")
    boolean isAnyUserExists();
    
    
    @Query("SELECT u FROM User u WHERE u.role.name = :roleName AND u.deleted = false AND u.enabled = true")
    List<User> findByRoleName(@Param("roleName") String roleName);
    
    @Query("SELECT u FROM User u WHERE u.role.name IN ('ADMIN', 'MODERATOR') AND u.deleted = false AND u.enabled = true")
    List<User> findModeratorsAndAdmins();
    
    
    @Query("SELECT u FROM User u WHERE u.deleted = false AND u.enabled = true")
    List<User> findAllActiveUsers();
    
    @Query("SELECT u FROM User u WHERE u.deleted = false AND u.enabled = true")
    List<User> findUsersWithNotificationEnabled();
    
    default List<User> getAllAdmins() {
        return findByRoleName("ADMIN");
    }
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.deleted = false")
    long count();
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.enabled = true AND u.deleted = false")
    long countByEnabledTrue();
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.enabled = false AND u.deleted = false")
    long countByEnabledFalse();
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.locked = true AND u.deleted = false")
    long countByLockedTrue();
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.enabled = false AND u.emailVerificationCode IS NOT NULL AND u.deleted = false")
    long countByEnabledFalseAndEmailVerificationCodeIsNotNull();
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.subscriptionEndDate IS NOT NULL AND u.subscriptionEndDate > :now AND u.deleted = false")
    long countBySubscriptionEndDateAfter(@Param("now") LocalDateTime now);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.role.name = :roleName AND u.deleted = false")
    long countByRoleName(@Param("roleName") String roleName);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt > :date AND u.deleted = false")
    long countByCreatedAtAfter(@Param("date") LocalDateTime date);
    
    @Query("SELECT COUNT(s) FROM Stream s WHERE s.streamer.id = :userId AND s.createdAt > :since AND s.deleted = false")
    long countStreamsSince(@Param("userId") String userId, @Param("since") LocalDateTime since);
}