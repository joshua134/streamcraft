package live.streamcraft.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AntMediaWebhookPayload {
	@JsonProperty("id")
	private String streamId;
	
	@JsonProperty("action")
	private String action;
	
	@JsonProperty("streamName")
	private String streamName;
	
	@JsonProperty("category")
	private String category;
	
	@JsonProperty("metadata")
    private String metadata;
    
    @JsonProperty("timestamp")
    private String timestamp;
    
    @JsonProperty("subscriberId")
    private String subscriberId; // For playStarted/playStopped events
    
    public boolean isStreamStarted() {
        return "liveStreamStarted".equals(action);
    }
    
    public boolean isStreamEnded() {
        return "liveStreamEnded".equals(action);
    }
    
    public boolean isPlayStarted() {
        return "playStarted".equals(action);
    }
    
    public boolean isPlayStopped() {
        return "playStopped".equals(action);
    }
}
