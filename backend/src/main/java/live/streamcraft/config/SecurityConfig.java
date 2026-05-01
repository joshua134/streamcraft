package live.streamcraft.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import live.streamcraft.filter.JwtAuthenticationFilter;
import live.streamcraft.filter.RateLimiterFilter;
import live.streamcraft.handler.JwtAccessDeniedHandler;
import live.streamcraft.handler.JwtAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
	private final UserDetailsService userDetailsService;
	private final JwtAuthenticationFilter jwtAuthFilter;
	private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
	private final RateLimiterFilter rateLimiterFilter;
	
	
	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
		.csrf(csrf -> csrf.disable())
		.cors(Customizer.withDefaults())
		.authorizeHttpRequests(auth -> auth
				.requestMatchers("/api/v1/auth/**", "/error").permitAll()

				.requestMatchers(HttpMethod.GET, "/api/v1/streams/{streamId}").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/v1/streams/live").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/v1/streams/upcoming").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/v1/streams/category/**").permitAll()

				.requestMatchers("/ws/**").permitAll()

				.requestMatchers(HttpMethod.POST, "/api/v1/streams").authenticated()
				.requestMatchers(HttpMethod.PUT, "/api/v1/streams/**").authenticated()
				.requestMatchers(HttpMethod.DELETE, "/api/v1/streams/**").authenticated()
				.requestMatchers("/api/v1/streams/my-streams/**").authenticated()
				.requestMatchers("/api/v1/streams/*/start").authenticated()
				.requestMatchers("/api/v1/streams/*/end").authenticated()
				.requestMatchers("/api/v1/streams/*/cancel").authenticated()
				.requestMatchers("/api/v1/streams/*/toggle-chat").authenticated()
				.requestMatchers("/api/v1/streams/*/mute/**").authenticated()
				.requestMatchers("/api/v1/streams/*/unmute/**").authenticated()
				.requestMatchers("/api/v1/streams/*/ban/**").authenticated()
				.requestMatchers("/api/v1/streams/*/unban/**").authenticated()
				.requestMatchers("/api/v1/streams/*/broadcast-info").authenticated()

				.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
				.requestMatchers("/api/v1/moderator/**").hasAnyRole("ADMIN", "MODERATOR")

				.anyRequest().authenticated()
		)
		.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
		.exceptionHandling(exception -> exception
			.authenticationEntryPoint(jwtAuthenticationEntryPoint)
			.accessDeniedHandler(jwtAccessDeniedHandler)
				)
		
		.authenticationProvider(authenticationProvider())
		;
		
		http.addFilterBefore(rateLimiterFilter, UsernamePasswordAuthenticationFilter.class);
		http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
		
		return http.build();
	}
	
	@Bean
    DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
//        authProvider.setUserDetailsService();
        authProvider.setPasswordEncoder(passwordEncoder());
        authProvider.setForcePrincipalAsString(false);
        return authProvider;
    }
	
    
//    @Bean
//    AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
//        return config.getAuthenticationManager();
//    }
    
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder builder = http.getSharedObject(AuthenticationManagerBuilder.class);
        // This forces the manager to use your provider which returns UserPrincipal
        builder.authenticationProvider(authenticationProvider());
        return builder.build();
    }
}
