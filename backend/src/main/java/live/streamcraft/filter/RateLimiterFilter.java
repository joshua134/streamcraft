package live.streamcraft.filter;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import live.streamcraft.exception.TooManyRequestsException;
import live.streamcraft.service.RateLimiterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimiterFilter extends OncePerRequestFilter {
	private final RateLimiterService rateLimiterService;
	
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String ip = getClientIp(request);
		String path = request.getRequestURI();
		
		String key = ip + ":" + path;
        try {
        	rateLimiterService.validate(key);
        }catch(TooManyRequestsException ex) {
        	response.setStatus(429);
        	String json = String.format("{\"status\": false, \"message\": \"%s\"}", "Too many requests");
        	response.getWriter().write(json);
        	return;
        }
        
        filterChain.doFilter(request, response);
	}

	private String getClientIp(HttpServletRequest request) {
		String xfHeader = request.getHeader("X-Forwarded-For");
		return (xfHeader == null) ? request.getRemoteAddr() : xfHeader.split(",")[0];
	}
}
