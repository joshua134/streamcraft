package live.streamcraft.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tbl_chat", indexes = {
    @Index(name = "idx_chat_stream", columnList = "stream_id"),
    @Index(name = "idx_chat_user", columnList = "user_id"),
    @Index(name = "idx_chat_created_at", columnList = "created_at"),
    @Index(name = "idx_chat_stream_created", columnList = "stream_id, created_at"),
    @Index(name = "idx_chat_deleted", columnList = "is_deleted")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class Chat extends BaseEntity {
	@Column(nullable = false, length = 500)
    private String message;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stream_id", nullable = false)
    private Stream stream;
}
