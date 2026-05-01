package live.streamcraft.entity;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import live.streamcraft.exception.StreamLimitExceededException;
import live.streamcraft.model.enums.StreamStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tbl_stream", indexes = {
    @Index(name = "idx_stream_status", columnList = "status"),
    @Index(name = "idx_stream_streamer", columnList = "streamer_id"),
    @Index(name = "idx_stream_category", columnList = "category_id"),
    @Index(name = "idx_stream_scheduled_start", columnList = "scheduledStartTime"),
    @Index(name = "idx_stream_created_at", columnList = "created_at"),
    @Index(name = "idx_stream_status_streamer", columnList = "status, streamer_id"),
    @Index(name = "idx_stream_is_live", columnList = "isLive, status"),
    @Index(name = "idx_stream_featured_live", columnList = "isFeatured, isLive, viewerCount"),
    @Index(name = "idx_stream_deleted_status", columnList="is_deleted, status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class Stream extends BaseEntity {
	@Column(nullable = false, length = 200)
    private String title;
    
    @Column(length = 1000)
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StreamStatus status;
    
    @Builder.Default
    private boolean isLive = false;
    
    private LocalDateTime actualStartTime;
    private LocalDateTime actualEndTime;
    private LocalDateTime scheduledStartTime;
    
    @Column(length = 500)
    private String streamKey;
    
    @Column(length = 500)
    private String playbackUrl;
    
    @Column(length = 500)
    private String hlsUrl;
    
    @Column(length = 500)
    private String rtmpUrl;
    
    @Builder.Default
    private int viewerCount = 0;
    
    @Builder.Default
    private int maxViewerCount = 0;
    
    @Builder.Default
    private boolean chatEnabled = true;
    
    @Builder.Default
    private boolean chatMuted = false;
    
    @Builder.Default
    private int durationMinutes = 0;
    
    @Builder.Default
    private boolean isFeatured = false;
    
    @Builder.Default
    private String thumbnailUrl = null;
    
    @Builder.Default
    private boolean isGloballyMuted = false; 
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "streamer_id", nullable = false)
    private User streamer;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private StreamCategory category;
    
    @OneToMany(mappedBy = "stream", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    private java.util.List<Chat> chatMessages = new java.util.ArrayList<>();
    
    @ElementCollection
    @CollectionTable(name = "stream_muted_users", 
        joinColumns = @JoinColumn(name = "stream_id"),
        indexes = @Index(name = "idx_muted_stream", columnList = "stream_id"))
    @Column(name = "user_id")
    @Builder.Default
    private java.util.Set<String> mutedUserIds = new java.util.HashSet<>();
    
    @ElementCollection
    @CollectionTable(name = "stream_banned_users",
        joinColumns = @JoinColumn(name = "stream_id"),
        indexes = @Index(name = "idx_banned_stream", columnList = "stream_id"))
    @Column(name = "user_id")
    @Builder.Default
    private java.util.Set<String> bannedUserIds = new java.util.HashSet<>();
    
    @Transient 
    public boolean isTimeLimitExceeded() {
    	if(actualStartTime == null) return false;
    	long minutesElapsed = Duration.between(actualStartTime, LocalDateTime.now()).toMinutes();
    	
    	User streamer = this.getStreamer();
    	if(streamer != null && !streamer.isSubscribed()) {
    		return minutesElapsed > 20;
    	}
    	return false;
    }
    
    @Transient
    public boolean canUserChat(String userId) {
    	if(isGloballyMuted) return false;
    	if(mutedUserIds.contains(userId)) return false;
    	if(bannedUserIds.contains(userId)) return false;
    	return chatEnabled;
    }
    
    public void updateFeaturedStatus(int totalViewerThreshold) {
    	this.isFeatured = this.viewerCount >= totalViewerThreshold && this.isLive;
    }
    
    @PrePersist
    @PreUpdate
    public void validateStreamLimits() {
        User streamer = this.getStreamer();
        if (streamer != null && !streamer.isSubscribed()) {
            if (streamer.getStreamsCreatedCount() >= 4 && this.isLive) {
                throw new StreamLimitExceededException("Free users can only create 4 streams");
            }
        }
    }
}
