package live.streamcraft.model.request;

import java.time.LocalDateTime;
import java.util.Map;

import live.streamcraft.model.enums.NotificationPriority;
import live.streamcraft.model.enums.NotificationType;

@lombok.Setter
@lombok.Getter
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class NotificationCreateRequest {
	private String title;
    private String message;
    private NotificationType type;
    private NotificationPriority priority;
    private String relatedEntityId;
    private String actionUrl;
    private Map<String, Object> metadata;
    private LocalDateTime expiresAt;
}
