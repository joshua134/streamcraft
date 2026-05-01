package live.streamcraft.scheduler;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import live.streamcraft.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupScheduler {
	private final RefreshTokenRepository refreshTokenRepository;
	
	// seconds, minutes, hours(24), day(1,all),  month(2, all), day number()
	@Scheduled(cron = "0 0 2 * * *")
	@Transactional
	public void cleanExpiredRefreshTokens() {
		LocalDateTime now  = LocalDateTime.now();
		refreshTokenRepository.deleteExpiredTokens(now);
		log.info("Cleaned up expired refresh tokens");
	}
}
