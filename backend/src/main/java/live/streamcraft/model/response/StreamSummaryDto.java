package live.streamcraft.model.response;

import java.time.LocalDateTime;

import live.streamcraft.model.enums.StreamStatus;


public record StreamSummaryDto(String id, String name, StreamStatus status, boolean isLive, boolean featured, long viewCount, 
		LocalDateTime scheduledStartTime, String streamerName, String thumbnailUrl) {}