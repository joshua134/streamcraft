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

import live.streamcraft.entity.PaymentSubscription;

@Repository
public interface PaymentSubscriptionRepository extends JpaRepository<PaymentSubscription, String> {
	@Query("SELECT ps FROM PaymentSubscription ps WHERE ps.user.id = :userId " +
           "AND ps.status = 'ACTIVE' AND ps.expiryDate > :now AND ps.deleted = false " +
           "ORDER BY ps.expiryDate DESC")
    Optional<PaymentSubscription> findActiveSubscription(@Param("userId") String userId, @Param("now") LocalDateTime now);
    
    @Query("SELECT ps FROM PaymentSubscription ps WHERE ps.status = 'ACTIVE' " +
           "AND ps.expiryDate < :now AND ps.deleted = false")
    List<PaymentSubscription> findExpiredActiveSubscriptions(@Param("now") LocalDateTime now);
    
    @Modifying
    @Transactional
    @Query("UPDATE PaymentSubscription ps SET ps.status = 'EXPIRED' WHERE ps.id IN :ids")
    void markSubscriptionsAsExpired(@Param("ids") List<String> ids);
    
    @Query("SELECT ps FROM PaymentSubscription ps WHERE ps.user.id = :userId AND ps.deleted = false ORDER BY ps.createdAt DESC")
    Page<PaymentSubscription> findSubscriptionHistory(@Param("userId") String userId, Pageable pageable);
}
