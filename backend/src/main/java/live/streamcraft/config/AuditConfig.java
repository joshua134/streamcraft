package live.streamcraft.config;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import live.streamcraft.entity.User;
import live.streamcraft.security.UserPrincipal;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableAsync
@EnableScheduling
public class AuditConfig {
	@Bean
	AuditorAware<User> auditorProvider(){
		return () -> {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			
			if (auth == null || !auth.isAuthenticated()) {
	            return Optional.empty();
	        }

			if(auth.getPrincipal() instanceof UserPrincipal principal) {
				return Optional.ofNullable(principal.getUser());
			}
			
			return Optional.empty();
		};
	}
}
