package live.streamcraft.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import live.streamcraft.entity.Notification;
import live.streamcraft.model.enums.NotificationEntityType;
import live.streamcraft.model.enums.NotificationPriority;
import live.streamcraft.model.enums.NotificationType;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {
    
    Page<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    
    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(String userId);
    
    long countByUserIdAndReadFalse(String userId);
    
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.read = false " +
           "AND n.priority = 'HIGH' OR n.priority = 'URGENT'")
    long countHighPriorityUnread(@Param("userId") String userId);
    
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId " +
           "AND n.createdAt >= :startOfDay")
    long countTodayNotifications(@Param("userId") String userId, @Param("startOfDay") LocalDateTime startOfDay);
    
    @Query("SELECT n.type, COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.read = false " +
           "GROUP BY n.type")
    List<Object[]> countUnreadByType(@Param("userId") String userId);
    
    List<Notification> findByUserIdAndReadFalseAndPriorityInOrderByCreatedAtDesc(String userId, List<NotificationPriority> priorities);
    
    @Query("SELECT n FROM Notification n WHERE n.expiresAt < :now AND n.deleted = false")
    List<Notification> findExpiredNotifications(@Param("now") LocalDateTime now);
    
    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.read = true, n.readAt = :now WHERE n.id = :id AND n.user.id = :userId")
    int markAsRead(@Param("id") String id, @Param("userId") String userId, @Param("now") LocalDateTime now);
    
    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.read = true, n.readAt = :now WHERE n.user.id = :userId AND n.read = false")
    int markAllAsRead(@Param("userId") String userId, @Param("now") LocalDateTime now);
    
    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.delivered = true, n.deliveredAt = :now WHERE n.id = :id")
    int markAsDelivered(@Param("id") String id, @Param("now") LocalDateTime now);
    
    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.deleted = true, n.deletedAt = :now WHERE n.expiresAt < :cutoffDate AND n.deleted = false")
    int softDeleteExpiredNotifications(@Param("cutoffDate") LocalDateTime cutoffDate, @Param("now") LocalDateTime now);
    
    List<Notification> findByRelatedEntityIdAndRelatedEntityTypeOrderByCreatedAtDesc(String entityId, NotificationEntityType entityType);
    
    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.deleted = true, n.deletedAt = :now WHERE n.user.id = :userId")
    int deleteAllByUserId(@Param("userId") String userId, @Param("now") LocalDateTime now);
    
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.type = :type AND n.createdAt > :since " +
           "AND n.deleted = false ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdAndTypeAndCreatedAtAfter(@Param("userId") String userId,@Param("type") NotificationType type,
            @Param("since") LocalDateTime since);
    
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.type = :type AND n.deleted = false")
    List<Notification> findByUserIdAndType(@Param("userId") String userId, @Param("type") NotificationType type);
    
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId " +
           "AND n.read = false AND n.priority IN :priorities ORDER BY n.priority DESC, n.createdAt DESC")
    List<Notification> findUnreadByPriority(@Param("userId") String userId,@Param("priorities") List<NotificationPriority> priorities);
    
    @Query("SELECT n.priority, COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.read = false AND n.deleted = false " +
           "GROUP BY n.priority")
    List<Object[]> countUnreadByPriority(@Param("userId") String userId);
    
    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.deleted = true, n.deletedAt = :now " +
           "WHERE n.user.id = :userId AND n.read = true AND n.createdAt < :cutoffDate AND n.deleted = false")
    int softDeleteOldReadNotifications(@Param("userId") String userId,@Param("cutoffDate") LocalDateTime cutoffDate,
                                        @Param("now") LocalDateTime now);
}
