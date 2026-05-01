package live.streamcraft.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import live.streamcraft.entity.Stream;
import live.streamcraft.entity.StreamCategory;
import live.streamcraft.entity.User;
import live.streamcraft.exception.NotFoundException;
import live.streamcraft.exception.ServiceException;
import live.streamcraft.exception.StreamLimitExceededException;
import live.streamcraft.exception.UnauthorizedException;
import live.streamcraft.model.enums.StreamStatus;
import live.streamcraft.model.request.AntMediaWebhookPayload;
import live.streamcraft.model.request.StreamCreateRequest;
import live.streamcraft.model.request.StreamUpdateRequest;
import live.streamcraft.model.response.AntMediaBroadcast;
import live.streamcraft.model.response.StreamDto;
import live.streamcraft.model.response.StreamStatusEvent;
import live.streamcraft.model.response.StreamSummaryDto;
import live.streamcraft.model.response.StreamerDto;
import live.streamcraft.model.response.ViewerUpdateEvent;
import live.streamcraft.repository.StreamCategoryRepository;
import live.streamcraft.repository.StreamRepository;
import live.streamcraft.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class StreamService {

	private final StreamRepository streamRepository;
	private final UserRepository userRepository;
	private final StreamCategoryRepository categoryRepository;
	private final AntMediaService antMediaService;
	private final SimpMessagingTemplate messagingTemplate;

	@Value("${streaming.free.max-duration-minutes:20}")
	private int maxFreeDurationMinutes;

	@Value("${streaming.free.max-streams:4}")
	private int maxFreeStreams;

	@Transactional
	public StreamDto createStream(String userId, StreamCreateRequest request) {
		User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));

		if (!user.canCreateStream()) {
			throw new StreamLimitExceededException(
					String.format("Stream limit reached. Remaining free streams: %d", user.getRemainingFreeStreams())
					);
		}

		StreamCategory category = null;
		if (request.getCategoryId() != null) {
			category = categoryRepository.findById(request.getCategoryId())
					.orElseThrow(() -> new NotFoundException("Category not found: " + request.getCategoryId()));
		}

		String streamKey = generateStreamKey();

		Integer durationMinutes = request.getDurationMinutes();
		if (durationMinutes != null && !user.isSubscribed() && durationMinutes > maxFreeDurationMinutes) {
			durationMinutes = maxFreeDurationMinutes;
			log.info("Capped stream duration to {} minutes for free user: {}", maxFreeDurationMinutes, userId);
		}

		AntMediaBroadcast broadcast;
		try {
			broadcast = antMediaService.createBroadcast(streamKey, request.getName(), request.getDescription());
			log.info("Broadcast created in Ant Media: {}", broadcast != null ? broadcast.getStreamId() : streamKey);
		} catch (Exception e) {
			log.error("Failed to create broadcast: {}", e.getMessage());
			throw new ServiceException("Streaming server unavailable. Please try again.");
		}

		String rtmpUrl = antMediaService.getRtmpIngestUrl(streamKey);
		String hlsUrl = antMediaService.getHlsPlaybackUrl(streamKey);
		String webRtcUrl = antMediaService.getWebRtcPlaybackUrl(streamKey);

		Stream stream = Stream.builder()
				.title(request.getName())
				.description(request.getDescription())
				.category(category)
				.streamer(user)
				.status(StreamStatus.SCHEDULED)
				.isLive(false)
				.streamKey(streamKey)
				.chatEnabled(true)
				.chatMuted(false)
				.isFeatured(false)
				.viewerCount(0)
				.scheduledStartTime(request.getScheduledStartTime())
				.durationMinutes(durationMinutes != null ? durationMinutes : 0)
				.rtmpUrl(rtmpUrl)
				.hlsUrl(hlsUrl)
				.playbackUrl(webRtcUrl)
				.build();

		Stream savedStream = streamRepository.save(stream);
		userRepository.incrementStreamCount(userId);
		log.info("Stream created: {} by user: {}", savedStream.getId(), userId);
		return toStreamDto(savedStream);
	}

	public StreamDto startStream(String userId, String streamId) {
		Stream stream = getStreamAndVerifyOwnership(userId, streamId);

		if (stream.getStatus() != StreamStatus.SCHEDULED) {
			throw new IllegalStateException("Stream is already started or ended");
		}

		boolean started = antMediaService.startBroadcast(stream.getStreamKey());
		if (!started) {
			log.warn("Failed to start broadcast in Ant Media for stream: {}", streamId);
		}

		stream.setLive(true);
		stream.setActualStartTime(LocalDateTime.now());
		stream.setStatus(StreamStatus.LIVE);

		Stream updatedStream = streamRepository.save(stream);

		StreamStatusEvent statusEvent = StreamStatusEvent.builder()
				.status("LIVE")
				.streamId(streamId)
				.title(stream.getTitle())
				.startedAt(LocalDateTime.now())
				.build();

		messagingTemplate.convertAndSend("/topic/streams/" + streamId + "/status", statusEvent);
		messagingTemplate.convertAndSend("/topic/streams/live/updates", statusEvent);

		log.info("Stream started: {} by user: {}", streamId, userId);

		return toStreamDto(updatedStream);
	}

	public StreamDto endStream(String userId, String streamId) {
		Stream stream = getStreamAndVerifyOwnership(userId, streamId);

		if (stream.getStatus() != StreamStatus.LIVE) {
			throw new IllegalStateException("Stream is not live");
		}

		boolean stopped = antMediaService.stopBroadcast(stream.getStreamKey());
		if (!stopped) {
			log.warn("Failed to stop broadcast in Ant Media for stream: {}", streamId);
		}

		stream.setLive(false);
		stream.setActualEndTime(LocalDateTime.now());
		stream.setStatus(StreamStatus.ENDED);
		stream.setViewerCount(0);

		Stream updatedStream = streamRepository.save(stream);

		StreamStatusEvent statusEvent = StreamStatusEvent.builder()
				.status("ENDED")
				.streamId(streamId)
				.endedAt(LocalDateTime.now())
				.build();

		messagingTemplate.convertAndSend("/topic/streams/" + streamId + "/status", statusEvent);

		log.info("Stream ended: {} by user: {}", streamId, userId);

		return toStreamDto(updatedStream);
	}

	@Transactional(readOnly = true)
	public StreamDto getStreamById(String streamId) {
		Stream stream = streamRepository.findByIdWithDetails(streamId)
				.orElseThrow(() -> new NotFoundException("Stream not found: " + streamId));
		return toStreamDto(stream);
	}

	@Transactional(readOnly = true)
	public Page<StreamSummaryDto> getLiveStreams(Pageable pageable) {
		return streamRepository.findLiveStreams(pageable)
				.map(this::toStreamSummaryDto);
	}

	@Transactional(readOnly = true)
	public AntMediaBroadcast getBroadcastInfo(String userId, String streamId) {
		Stream stream = getStreamAndVerifyOwnership(userId, streamId);

		AntMediaBroadcast broadcastInfo = new AntMediaBroadcast();
		broadcastInfo.setStreamId(stream.getId());
		broadcastInfo.setName(stream.getTitle());
		broadcastInfo.setRtmpURL(stream.getRtmpUrl());
		broadcastInfo.setStatus(stream.getStatus().name());
		broadcastInfo.setLive(stream.isLive());

		return broadcastInfo;
	}

	public void incrementViewerCount(String streamId) {
		streamRepository.incrementViewerCount(streamId);

		streamRepository.findById(streamId).ifPresent(stream -> {
			ViewerUpdateEvent event = ViewerUpdateEvent.builder()
					.streamId(streamId)
					.viewerCount(stream.getViewerCount() + 1)
					.action("joined")
					.build();
			messagingTemplate.convertAndSend("/topic/streams/" + streamId + "/viewers", event);
		});
	}

	public void decrementViewerCount(String streamId) {
		streamRepository.decrementViewerCount(streamId);

		streamRepository.findById(streamId).ifPresent(stream -> {
			ViewerUpdateEvent event = ViewerUpdateEvent.builder()
					.streamId(streamId)
					.viewerCount(Math.max(0, stream.getViewerCount() - 1))
					.action("left")
					.build();
			messagingTemplate.convertAndSend("/topic/streams/" + streamId + "/viewers", event);
		});
	}

	public void handleWebhook(AntMediaWebhookPayload payload) {
		log.info("Webhook received: action={}, streamId={}", payload.getAction(), payload.getStreamId());

		if (payload.isStreamStarted()) {
			streamRepository.findByStreamKey(payload.getStreamId()).ifPresent(stream -> {
				stream.setLive(true);
				stream.setStatus(StreamStatus.LIVE);
				if (stream.getActualStartTime() == null) {
					stream.setActualStartTime(LocalDateTime.now());
				}
				streamRepository.save(stream);
				log.info("Webhook: Stream marked LIVE: {}", payload.getStreamId());
			});
		} else if (payload.isStreamEnded()) {
			streamRepository.findByStreamKey(payload.getStreamId()).ifPresent(stream -> {
				stream.setLive(false);
				stream.setStatus(StreamStatus.ENDED);
				stream.setActualEndTime(LocalDateTime.now());
				streamRepository.save(stream);
				log.info("Webhook: Stream marked ENDED: {}", payload.getStreamId());
			});
		}
	}

	private Stream getStreamAndVerifyOwnership(String userId, String streamId) {
		Stream stream = streamRepository.findByIdWithDetails(streamId)
				.orElseThrow(() -> new NotFoundException("Stream not found: " + streamId));

		if (!stream.getStreamer().getId().equals(userId)) {
			throw new UnauthorizedException("Not authorized to manage this stream");
		}
		return stream;
	}

	private String generateStreamKey() {
		return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
	}

	private StreamDto toStreamDto(Stream stream) {
		StreamerDto streamerDto = new StreamerDto(
				stream.getStreamer().getId(),
				stream.getStreamer().getUname(),
				stream.getStreamer().getAvatarUrl()
				);

		return StreamDto.builder()
				.id(stream.getId())
				.name(stream.getTitle())
				.description(stream.getDescription())
				.status(stream.getStatus())
				.isLive(stream.isLive())
				.streamKey(stream.getStreamKey())
				.rtmpUrl(stream.getRtmpUrl())
				.hlsUrl(stream.getHlsUrl())
				.webRTCUrl(stream.getPlaybackUrl())
				.viewerCount(stream.getViewerCount())
				.chatEnabled(stream.isChatEnabled())
				.chatMuted(stream.isChatMuted())
				.featured(stream.isFeatured())
				.streamerId(stream.getStreamer().getId())
				.streamerName(stream.getStreamer().getUname())
				.streamer(streamerDto)
				.categoryId(stream.getCategory() != null ? stream.getCategory().getId() : null)
				.categoryName(stream.getCategory() != null ? stream.getCategory().getName() : null)
				.createdAt(stream.getCreatedAt())
				.scheduledStartTime(stream.getScheduledStartTime())
				.actualStartTime(stream.getActualStartTime())
				.actualEndTime(stream.getActualEndTime())
				.durationMinutes(stream.getDurationMinutes())
				.build();
	}

	private StreamSummaryDto toStreamSummaryDto(Stream stream) {
		return new StreamSummaryDto(
				stream.getId(),
				stream.getTitle(),
				stream.getStatus(),
				stream.isLive(),
				stream.isFeatured(),
				stream.getViewerCount(),
				stream.getScheduledStartTime(),
				stream.getStreamer().getUname(),
				stream.getThumbnailUrl()
				);
	}

	public Stream findById(String streamId) {
		return streamRepository.findById(streamId).orElseThrow(() -> new NotFoundException("Stream not found"));
	}

	@Transactional
	public Stream save(Stream stream) {
		return streamRepository.save(stream);
	}

	@Transactional
	public void delete(Stream stream) {
		Stream foundStream = findById(stream.getId());
		foundStream.setDeleted(true);
		foundStream.setDeletedAt(LocalDateTime.now());
		save(foundStream);
	}

	@Transactional
	public StreamDto cancelStream(String userId, String streamId) {
		Stream stream = getStreamAndVerifyOwnership(userId, streamId);
		if (stream.getStatus() == StreamStatus.LIVE) {
			throw new ServiceException("Cannot cancel a live stream. End it instead.");
		}

		stream.setStatus(StreamStatus.CANCELLED);
		Stream updated = streamRepository.save(stream);

		log.info("Stream cancelled: {} by user: {}", streamId, userId);

		return toStreamDto(updated);
	}

	@Transactional(readOnly = true)
	public Page<StreamSummaryDto> getUserStreams(String userId, Pageable pageable) {
		return streamRepository.findByStreamerId(userId, pageable)
				.map(this::toStreamSummaryDto);
	}

	@Transactional
	public StreamDto adminForceEndStream(String streamId) {
		Stream stream = streamRepository.findByIdWithDetails(streamId)
				.orElseThrow(() -> new NotFoundException("Stream not found"));

		antMediaService.stopBroadcast(stream.getStreamKey());

		stream.setLive(false);
		stream.setStatus(StreamStatus.ENDED);
		stream.setActualEndTime(LocalDateTime.now());

		return toStreamDto(streamRepository.save(stream));
	}

	@Transactional
	public StreamDto toggleFeature(String streamId, boolean featured) {
		Stream stream = streamRepository.findById(streamId)
				.orElseThrow(() -> new NotFoundException("Stream not found"));

		stream.setFeatured(featured);
		return toStreamDto(streamRepository.save(stream));
	}

	@Transactional
	public StreamDto toggleChatMute(String streamId, boolean muted) {
		Stream stream = streamRepository.findById(streamId)
				.orElseThrow(() -> new NotFoundException("Stream not found"));

		stream.setChatMuted(muted);
		return toStreamDto(streamRepository.save(stream));
	}

	@Transactional
	public void adminDeleteStream(String adminId, String streamId) {
		Stream stream = streamRepository.findById(streamId).orElseThrow(() -> new NotFoundException("Stream not found"));

		antMediaService.deleteBroadcast(stream.getStreamKey());
		stream.setDeleted(true);
		stream.setDeletedAt(LocalDateTime.now());

		streamRepository.save(stream);

		log.info("Admin {} deleted stream {}", adminId, streamId);
	}

	@Transactional(readOnly = true)
	public Page<StreamSummaryDto> getUpcomingStreams(Pageable pageable) {
		LocalDateTime now = LocalDateTime.now();

		return streamRepository.findUpcomingStreams(now, pageable)
				.map(this::toStreamSummaryDto);
	}

	@Transactional
	public void deleteStream(String userId, String streamId) {
		Stream stream = streamRepository.findById(streamId).orElseThrow(() -> new NotFoundException("Stream not found"));

		if (!stream.getStreamer().getId().equals(userId)) {
			throw new UnauthorizedException("Not allowed to delete this stream");
		}

		if (stream.getStatus() == StreamStatus.LIVE) {
			throw new IllegalStateException("Cannot delete a live stream. End it first.");
		}

		antMediaService.deleteBroadcast(stream.getStreamKey());

		stream.setDeleted(true);
		stream.setDeletedAt(LocalDateTime.now());

		streamRepository.save(stream);

		log.info("User {} deleted stream {}", userId, streamId);
	}

	@Transactional(readOnly = true)
	public Page<StreamSummaryDto> getStreamsByCategory(String categoryId, Pageable pageable) {

		if (!categoryRepository.existsById(categoryId)) {
			throw new NotFoundException("Category not found");
		}

		return streamRepository.findActiveStreamsByCategory(categoryId, pageable).map(this::toStreamSummaryDto);
	}

	@Transactional(readOnly = true)
	public List<StreamSummaryDto> getTrendingStreams() {
		List<Stream> liveStreams = streamRepository.findLiveStreamsForTrending();
		LocalDateTime now = LocalDateTime.now();

		return liveStreams.stream().sorted((a, b) -> Double.compare(calculateTrendingScore(b, now), calculateTrendingScore(a, now)))
				.limit(20).map(this::toStreamSummaryDto).toList();
	}

	private double calculateTrendingScore(Stream s, LocalDateTime now) {
		double viewerScore = s.getViewerCount();
		double recencyBoost = 0;
		if (s.getActualStartTime() != null) {
			long minutes = Duration.between(s.getActualStartTime(), now).toMinutes();
			recencyBoost = Math.max(0, 100 - minutes); // newer streams get boost
		}

		double growthScore = 0;
		if (s.getMaxViewerCount() > 0 && s.getActualStartTime() != null) {
			long minutes = Math.max(1,Duration.between(s.getActualStartTime(), now).toMinutes());
			growthScore = (double) s.getViewerCount() / minutes;
		}

		return viewerScore + (growthScore * 2) + recencyBoost;
	}

	@Transactional
	public StreamDto updateStream(String userId, String streamId, StreamUpdateRequest req) {

		Stream stream = getStreamAndVerifyOwnership(userId, streamId);

		if (stream.getStatus() == StreamStatus.LIVE) {
			throw new ServiceException("Cannot update live stream metadata");
		}

		if (req.getTitle() != null) stream.setTitle(req.getTitle());
		if (req.getDescription() != null) stream.setDescription(req.getDescription());
		if (req.getScheduledStartTime() != null)
			stream.setScheduledStartTime(req.getScheduledStartTime());
		if (req.getCategoryId() != null) {
			StreamCategory category = categoryRepository.findById(req.getCategoryId())
					.orElseThrow(() -> new NotFoundException("Category not found"));
			stream.setCategory(category);
		}

		return toStreamDto(streamRepository.save(stream));
	}

	@Transactional
	public StreamDto toggleChat(String userId, String streamId) {
		Stream stream = getStreamAndVerifyOwnership(userId, streamId);
		stream.setChatEnabled(!stream.isChatEnabled());
		return toStreamDto(streamRepository.save(stream));
	}

	@Transactional
	public StreamDto muteUserInStream(String userId, String streamId, String targetUserId) {
		Stream stream = getStreamAndVerifyOwnership(userId, streamId);
		stream.getMutedUserIds().add(targetUserId);
		return toStreamDto(streamRepository.save(stream));
	}

	@Transactional
	public StreamDto unmuteUserInStream(String userId, String streamId, String targetUserId) {
		Stream stream = getStreamAndVerifyOwnership(userId, streamId);
		stream.getMutedUserIds().remove(targetUserId);
		return toStreamDto(streamRepository.save(stream));
	}

	@Transactional
	public StreamDto banUserFromStream(String userId, String streamId, String targetUserId) {
		Stream stream = getStreamAndVerifyOwnership(userId, streamId);
		stream.getBannedUserIds().add(targetUserId);
		return toStreamDto(streamRepository.save(stream));
	}

	@Transactional
	public StreamDto unbanUserFromStream(String userId, String streamId, String targetUserId) {
		Stream stream = getStreamAndVerifyOwnership(userId, streamId);
		stream.getBannedUserIds().remove(targetUserId);
		return toStreamDto(streamRepository.save(stream));
	}

	@Transactional(readOnly = true)
	public List<StreamSummaryDto> getActiveStreamsByStreamer(String userId) {
		List<Stream> streams = streamRepository.findActiveStreamsByStreamer(userId);
		return streams.stream().map(this::toStreamSummaryDto).toList();
	}

	@Transactional(readOnly = true)
	public Page<StreamSummaryDto> getEndedStreamsByStreamer(String userId, Pageable pageable) {
		return streamRepository.findEndedStreamsByStreamer(userId, pageable).map(this::toStreamSummaryDto);
	}
}
