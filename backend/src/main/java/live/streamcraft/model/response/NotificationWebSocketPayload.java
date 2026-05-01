package live.streamcraft.model.response;

import java.time.LocalDateTime;
import java.util.Map;

@lombok.Setter
@lombok.Getter
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
@lombok.Builder
public class NotificationWebSocketPayload {
	 private String id;
	 private String title;
	 private String message;
	 private String type;
	 private String priority;
	 private String actionUrl;
	 private Map<String, Object> metadata;
	 private LocalDateTime createdAt;
	 private int aggregatedCount;
}
