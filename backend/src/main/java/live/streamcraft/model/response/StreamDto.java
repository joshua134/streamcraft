package live.streamcraft.model.response;

import java.time.LocalDateTime;

import live.streamcraft.model.enums.StreamStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StreamDto {
	private String id;
    private String name;
    private String description;
    private StreamStatus status;
    private boolean isLive;
    private boolean chatEnabled;
    private boolean chatMuted;
    private boolean featured;
    private String streamKey;
    private String playbackUrl;
    private String rtmpUrl;
    private String webRTCUrl;
    private String streamerId;
    private String streamerName;
    private String categoryId;
    private String categoryName;
    private String hlsUrl;
    private long viewerCount;
    private Integer durationMinutes;
    private LocalDateTime scheduledStartTime;
    private LocalDateTime actualStartTime;
    private LocalDateTime actualEndTime;
    private LocalDateTime createdAt;
    private StreamerDto streamer;
}