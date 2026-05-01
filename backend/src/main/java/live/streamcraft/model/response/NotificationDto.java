package live.streamcraft.model.response;

import java.time.LocalDateTime;
import java.util.Map;

@lombok.Setter
@lombok.Getter
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class NotificationDto {
	private String id;
    private String userId;
    private String title;
    private String message;
    private String type;
    private String priority;
    private boolean read;
    private boolean delivered;
    private boolean dismissed;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
    private String actionUrl;
    private Map<String, Object> metadata;
    private int aggregatedCount;
}
