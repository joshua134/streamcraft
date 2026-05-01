package live.streamcraft.model.request;

import java.util.List;

@lombok.Setter
@lombok.Getter
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class NotificationBulkRequest {
	private List<String> notificationIds;
    private Boolean read;
    private Boolean dismissed;
}
