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
public class NotificationUpdateRequest {
	private Boolean read;
    private Boolean dismissed;
}
