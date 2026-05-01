package live.streamcraft.model.response;

import java.time.LocalDateTime;

/**
 *  @param chat id
 *  @param stream id
 *  @param user id
 *  @param chat message
 *  @param chat user username
 *  @param chat user avatarUrl
 *  @param chat createdat
 */
public record ChatMessageDto(String id, String streamId, String userId, String message, String username, String userAvatarUrl, 
		LocalDateTime createdAt) {
	
}
