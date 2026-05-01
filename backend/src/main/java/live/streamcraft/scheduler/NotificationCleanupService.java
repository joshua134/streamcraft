package live.streamcraft.scheduler;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import live.streamcraft.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationCleanupService {
	 private final NotificationRepository notificationRepository;
	    
	 // clean up notifications 2 am every day
	 @Scheduled(cron = "0 0 2 * * ?")
	 @Transactional
	 public void cleanupExpiredNotifications() {
		 log.info("Starting cleanup of expired notifications");
		 LocalDateTime expiryCutoff = LocalDateTime.now().minusDays(30);
		 int deleted = notificationRepository.softDeleteExpiredNotifications(expiryCutoff, LocalDateTime.now());
		 log.info("Cleaned up {} expired notifications", deleted);
	 }
}
