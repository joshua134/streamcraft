package live.streamcraft.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import live.streamcraft.entity.Notification;
import live.streamcraft.entity.User;
import live.streamcraft.exception.NotFoundException;
import live.streamcraft.exception.UnauthorizedException;
import live.streamcraft.model.enums.NotificationPriority;
import live.streamcraft.model.enums.NotificationType;
import live.streamcraft.model.request.NotificationCreateRequest;
import live.streamcraft.model.response.NotificationDto;
import live.streamcraft.model.response.NotificationMarkResult;
import live.streamcraft.model.response.NotificationSummaryDto;
import live.streamcraft.model.response.NotificationWebSocketPayload;
import live.streamcraft.model.response.UnreadCountUpdate;
import live.streamcraft.model.response.WebSocketEvent;
import live.streamcraft.repository.NotificationRepository;
import live.streamcraft.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@lombok.RequiredArgsConstructor
public class NotificationService {
	private final NotificationRepository notificationRepository;
	private final UserRepository userRepository;
	private final SimpMessagingTemplate messagingTemplate;

	public NotificationDto createNotification(String userId, NotificationCreateRequest request) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new NotFoundException("User not found: " + userId));

		Notification notification = Notification.builder()
				.user(user)
				.title(request.getTitle())
				.message(request.getMessage())
				.type(request.getType())
				.priority(request.getPriority() != null ? request.getPriority() : NotificationPriority.MEDIUM)
				.relatedEntityId(request.getRelatedEntityId())
				.actionUrl(request.getActionUrl())
				.metadata(request.getMetadata() != null ? request.getMetadata() : new HashMap<>())
				.expiresAt(request.getExpiresAt())
				.read(false)
				.delivered(false)
				.dismissed(false)
				.aggregatedCount(1)
				.build();

		Notification saved = notificationRepository.save(notification);
		log.info("Notification created for user {}: type={}", userId, request.getType());

		// Send real-time notification via WebSocket
		sendRealtimeNotification(userId, toWebSocketPayload(saved));

		// Update unread count
		sendUnreadCountUpdate(userId);

		return toResponse(saved);
	}

	/**
	 * Create notification for multiple users (broadcast)
	 */
	@Async
	public void createBulkNotification(List<String> userIds, NotificationCreateRequest request) {
		List<Notification> notifications = new ArrayList<>();

		for (String userId : userIds) {
			userRepository.findById(userId).ifPresent(user -> {
				Notification notification = Notification.builder()
						.user(user)
						.title(request.getTitle())
						.message(request.getMessage())
						.type(request.getType())
						.priority(request.getPriority() != null ? request.getPriority() : NotificationPriority.MEDIUM)
						.relatedEntityId(request.getRelatedEntityId())
						.actionUrl(request.getActionUrl())
						.metadata(request.getMetadata() != null ? request.getMetadata() : new HashMap<>())
						.expiresAt(request.getExpiresAt())
						.read(false)
						.delivered(false)
						.dismissed(false)
						.aggregatedCount(1)
						.build();
				notifications.add(notification);
			});
		}

		if (!notifications.isEmpty()) {
			notificationRepository.saveAll(notifications);
			log.info("Created {} notifications for {} users", notifications.size(), userIds.size());

			notifications.forEach(notification -> {
				sendRealtimeNotification(notification.getUser().getId(), toWebSocketPayload(notification));
				sendUnreadCountUpdate(notification.getUser().getId());
			});
		}
	}

	public NotificationDto createAggregatedNotification(String userId, NotificationType type,String title, String message, 
			int count, Map<String, Object> metadata) {
		LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

		List<Notification> recentSimilar = notificationRepository.findByUserIdAndTypeAndCreatedAtAfter(userId, type, 
				oneHourAgo);

		if (!recentSimilar.isEmpty()) {
			// Update existing notification
			Notification existing = recentSimilar.get(0);
			existing.setAggregatedCount(existing.getAggregatedCount() + count);
			existing.setMessage(String.format(message, existing.getAggregatedCount()));
			existing.setUpdatedAt(LocalDateTime.now());

			Notification updated = notificationRepository.save(existing);
			log.info("Updated aggregated notification for user {}: count={}", userId, updated.getAggregatedCount());

			// Send update via WebSocket (refresh the notification)
			sendRealtimeNotification(userId, toWebSocketPayload(updated));
			return toResponse(updated);
		} else {
			// Create new notification with aggregated count
			return createNotification(userId, NotificationCreateRequest.builder()
					.title(title)
					.message(String.format(message, count))
					.type(type)
					.priority(NotificationPriority.MEDIUM)
					.metadata(metadata)
					.build());
		}
	}

	@Transactional(readOnly = true)
	public Page<NotificationDto> getUserNotifications(String userId, Pageable pageable) {
		return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
				.map(this::toResponse);
	}

	@Transactional(readOnly = true)
	public List<NotificationDto> getUnreadNotifications(String userId) {
		return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId)
				.stream()
				.limit(50)
				.map(this::toResponse)
				.collect(Collectors.toList());
	}

	@Transactional
	public NotificationSummaryDto getNotificationSummary(String userId) {
		long totalUnread = notificationRepository.countByUserIdAndReadFalse(userId);
		long highPriorityUnread = notificationRepository.countHighPriorityUnread(userId);
		long todayCount = notificationRepository.countTodayNotifications(userId, 
				LocalDateTime.now().withHour(0).withMinute(0).withSecond(0));

		List<Object[]> typeCounts = notificationRepository.countUnreadByType(userId);
		Map<String, Long> countByType = typeCounts.stream()
				.collect(Collectors.toMap(
						arr -> ((NotificationType) arr[0]).name(),
						arr -> (Long) arr[1]
						));

		return NotificationSummaryDto.builder()
				.totalUnread(totalUnread)
				.highPriorityUnread(highPriorityUnread)
				.todayCount(todayCount)
				.countByType(countByType)
				.build();
	}

	public NotificationMarkResult markAsRead(String userId, String notificationId) {
		int updated = notificationRepository.markAsRead(notificationId, userId, LocalDateTime.now());

		if (updated > 0) {
			sendUnreadCountUpdate(userId);
			return new NotificationMarkResult(updated, LocalDateTime.now());
		}
		throw new NotFoundException("Notification not found");
	}

	public NotificationMarkResult markAllAsRead(String userId) {
		int updated = notificationRepository.markAllAsRead(userId, LocalDateTime.now());
		sendUnreadCountUpdate(userId);

		return new NotificationMarkResult(updated, LocalDateTime.now());
	}

	public void deleteNotification(String userId, String notificationId) {
		Notification notification = notificationRepository.findById(notificationId)
				.orElseThrow(() -> new NotFoundException("Notification not found"));

		if (!notification.getUser().getId().equals(userId)) {
			throw new UnauthorizedException("Not authorized to delete this notification");
		}

		notification.setDeleted(true);
		notification.setDeletedAt(LocalDateTime.now());
		notificationRepository.save(notification);

		sendUnreadCountUpdate(userId);
	}

	private void sendRealtimeNotification(String userId, NotificationWebSocketPayload payload) {
		WebSocketEvent event = new WebSocketEvent("NOTIFICATION",UUID.randomUUID().toString(),LocalDateTime.now(),payload);

		// Send to user's personal queue
		messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", event);

		messagingTemplate.convertAndSend("/topic/user/" + userId + "/notifications", event);

		log.debug("Sent real-time notification via WebSocket to user: {}", userId);
	}

	private void sendUnreadCountUpdate(String userId) {
		long unreadCount = notificationRepository.countByUserIdAndReadFalse(userId);
		long highPriorityCount = notificationRepository.countHighPriorityUnread(userId);

		UnreadCountUpdate update = new UnreadCountUpdate(unreadCount,highPriorityCount,LocalDateTime.now());

		messagingTemplate.convertAndSendToUser(userId, "/queue/unread-count", update);
		messagingTemplate.convertAndSend("/topic/user/" + userId + "/unread-count", update);
	}

	private NotificationWebSocketPayload toWebSocketPayload(Notification notification) {
		return NotificationWebSocketPayload.builder()
				.id(notification.getId())
				.title(notification.getTitle())
				.message(notification.getMessage())
				.type(notification.getType().name())
				.priority(notification.getPriority().name())
				.actionUrl(notification.getActionUrl())
				.metadata(notification.getMetadata())
				.createdAt(notification.getCreatedAt())
				.aggregatedCount(notification.getAggregatedCount())
				.build();
	}

	private NotificationDto toResponse(Notification notification) {
		return NotificationDto.builder()
				.id(notification.getId())
				.userId(notification.getUser().getId())
				.title(notification.getTitle())
				.message(notification.getMessage())
				.type(notification.getType().name())
				.priority(notification.getPriority().name())
				.read(notification.isRead())
				.delivered(notification.isDelivered())
				.dismissed(notification.isDismissed())
				.createdAt(notification.getCreatedAt())
				.readAt(notification.getReadAt())
				.actionUrl(notification.getActionUrl())
				.metadata(notification.getMetadata())
				.aggregatedCount(notification.getAggregatedCount())
				.build();
	}
}
