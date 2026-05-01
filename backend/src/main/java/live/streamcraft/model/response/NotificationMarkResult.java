package live.streamcraft.model.response;

import java.time.LocalDateTime;

public record NotificationMarkResult(int updatedCount,LocalDateTime updatedAt) {
}
