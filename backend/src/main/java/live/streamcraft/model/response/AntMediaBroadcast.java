package live.streamcraft.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AntMediaBroadcast {
	private String streamId;
	private String name;
	private String description;
	private String status;
	private String type;
	private boolean publish;

	@JsonProperty("rtmpURL")
	private String rtmpURL;

	@JsonProperty("hlsViewerCount")
	private int hlsViewerCount;

	@JsonProperty("webRTCViewerCount")
	private int webRTCViewerCount;

	@JsonProperty("rtmpViewerCount")
	private int rtmpViewerCount;

	private Integer durationMinutes;
	private Integer remainingTime;
	private boolean isLive;

	// Helper method for total viewers
	public int getTotalViewerCount() {
		return hlsViewerCount + webRTCViewerCount + rtmpViewerCount;
	}

	public boolean isLive() {
		return "broadcasting".equalsIgnoreCase(status);
	}
}
