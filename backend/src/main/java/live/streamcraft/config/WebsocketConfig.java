package live.streamcraft.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebsocketConfig implements WebSocketMessageBrokerConfigurer  {
	
	@Value("${spring.websocket.allowed-origins}")
	private String[] allowedOrigins;
	
	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		registry.enableSimpleBroker("/topic", "/queue");
		registry.setApplicationDestinationPrefixes("/app");
		registry.setUserDestinationPrefix("/user");
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/ws")
		.setAllowedOriginPatterns(allowedOrigins)
		.withSockJS();

		registry.addEndpoint("/ws/chat")
		.setAllowedOriginPatterns(allowedOrigins)
		.withSockJS();
		
		registry.addEndpoint("/ws/notifications")
            .setAllowedOriginPatterns(allowedOrigins)
            .withSockJS();
	}

}
