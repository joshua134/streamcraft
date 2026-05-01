package live.streamcraft.model.response;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamStatusEvent {
    private String status;
    private String streamId;
    private String title;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Map<String, Object> metadata;
    
    public StreamStatusEvent(Map<String, Object> map) {
        this.status = (String) map.get("status");
        this.streamId = (String) map.get("streamId");
        this.title = (String) map.get("title");
        if (map.get("startedAt") instanceof String) {
            this.startedAt = LocalDateTime.parse((String) map.get("startedAt"));
        }
        if (map.get("endedAt") instanceof String) {
            this.endedAt = LocalDateTime.parse((String) map.get("endedAt"));
        }
    }
}
