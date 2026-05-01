package live.streamcraft.entity;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import live.streamcraft.model.enums.NotificationEntityType;
import live.streamcraft.model.enums.NotificationPriority;
import live.streamcraft.model.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tbl_notification", indexes = {
		  @Index(name = "idx_notification_user", columnList = "user_id"),
		    @Index(name = "idx_notification_read", columnList = "is_read"),
		    @Index(name = "idx_notification_created", columnList = "created_at"),
		    @Index(name = "idx_notification_user_read", columnList = "user_id, is_read"),
		    @Index(name = "idx_notification_user_created", columnList = "user_id, created_at"),
		    @Index(name = "idx_notification_type", columnList = "type"),
		    @Index(name = "idx_notification_deleted", columnList = "is_deleted")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Notification extends BaseEntity {
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false, length = 200)
    private String title;
    
    @Column(nullable = false, length = 2000)
    private String message;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private NotificationPriority priority = NotificationPriority.MEDIUM;
    
    @Builder.Default
    @Column(name = "is_read")
    private boolean read = false;
    
    private LocalDateTime readAt;
    
    private String relatedEntityId;
    
    @Enumerated(EnumType.STRING)
    private NotificationEntityType relatedEntityType;
    
    @Column(length = 500)
    private String actionUrl;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    private LocalDateTime expiresAt;
    
    @Builder.Default
    @Column(name = "is_delivered")
    private boolean delivered = false;
    
    private LocalDateTime deliveredAt;
    
    @Builder.Default
    @Column(name = "is_dismissed")
    private boolean dismissed = false;
    
    @Builder.Default
    private int aggregatedCount = 1;
    
    @PrePersist
    protected void onCreate() {
        if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusDays(30);
        }
    }
    
    @Transient
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }
    
    @Transient
    public boolean isUnread() {
        return !read;
    }

}
