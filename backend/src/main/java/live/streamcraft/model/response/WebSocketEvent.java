package live.streamcraft.model.response;

import java.time.LocalDateTime;

/**
 * @param event type ("NOTIFICATION", "STREAM_UPDATE", "CHAT_MESSAGE")
 * @param event id
 * @param timestamp
 * @param payload
 */
public record WebSocketEvent(String eventType,String eventId,LocalDateTime timestamp,Object payload) {
	 // 
}
