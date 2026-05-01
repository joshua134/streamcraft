package live.streamcraft.model.response;

import java.time.LocalDateTime;
import java.util.Map;

@lombok.Setter
@lombok.Getter
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class NotificationSummaryDto {
	private long totalUnread;
    private long highPriorityUnread;
    private long todayCount;
    private Map<String, Long> countByType;
}
