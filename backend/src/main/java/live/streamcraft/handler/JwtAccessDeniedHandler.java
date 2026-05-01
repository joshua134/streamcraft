package live.streamcraft.handler;

import java.io.IOException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException, ServletException {
		 response.setContentType("application/json");
	     response.setStatus(HttpServletResponse.SC_FORBIDDEN);
	     String json = String.format("{\"status\": false, \"message\": \"%s\", \"data\": null}","Forbidden: You do not have the required permissions");

	     response.getWriter().write(json);
	}

}
