package live.streamcraft.model.response;

import java.time.LocalDateTime;

public record UnreadCountUpdate(long unreadCount,long highPriorityCount,LocalDateTime updatedAt) {

}
