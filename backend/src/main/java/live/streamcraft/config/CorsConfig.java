package live.streamcraft.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
	
	@Value("${spring.websocket.allowed-origins}")
	private String[] allowedOrigins;
	
	@Override
	public void addCorsMappings(CorsRegistry registry) {
		// Apply to all paths
		registry.addMapping("/**")
			.allowedOrigins(allowedOrigins)
			// Allowed HTTP methods
			.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") 
			// Allow all headers
            .allowedHeaders("*") 
            // Required for sending cookies or auth headers
            .allowCredentials(true) 
            // Cache preflight response for 1 hour
            .maxAge(3600);
	}
	
}
