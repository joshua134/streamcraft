package live.streamcraft.v1.controller;

import java.util.Arrays;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import live.streamcraft.exception.UnauthorizedException;
import live.streamcraft.model.request.ActivateAccountRequest;
import live.streamcraft.model.request.CheckAccountRequest;
import live.streamcraft.model.request.LoginRequest;
import live.streamcraft.model.request.PasswordResetCodeRequest;
import live.streamcraft.model.request.PasswordResetRequest;
import live.streamcraft.model.request.RegisterRequest;
import live.streamcraft.model.request.ResendActivationCode;
import live.streamcraft.model.response.AppResponse;
import live.streamcraft.model.response.LoginToken;
import live.streamcraft.security.UserPrincipal;
import live.streamcraft.service.AuthService;
import live.streamcraft.service.RefreshService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AuthController {
	private final AuthService authService;
	private final RefreshService refreshService;
	
	@PostMapping("/login")
	public ResponseEntity<AppResponse<String>> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response, 
			HttpServletRequest httpRequest) {
		AppResponse<LoginToken> serviceResponse = authService.loginUser(request.getEmail(), request.getPassword(), httpRequest);
		addRefreshCookie(response, serviceResponse.getData().refreshToken());
		return ResponseEntity.ok(AppResponse.success("Logged in", serviceResponse.getData().accessToken()));
	}

	@PostMapping("/resend-activation-code")
	public ResponseEntity<AppResponse<String>> resendActivationCode(@Valid @RequestBody ResendActivationCode request){
		return ResponseEntity.ok(authService.resendActivationCode(request.getEmail()));
	}
	
	@PostMapping("/resend-password-reset-code")
	public ResponseEntity<AppResponse<String>> resendPasswordResetCode(@Valid @RequestBody PasswordResetCodeRequest request){
		return ResponseEntity.ok(authService.resendPasswordResetCode(request.getEmail()));
	}
	
	@PostMapping("/register")
	public ResponseEntity<AppResponse<String>> register(@Valid @RequestBody RegisterRequest request) {
		AppResponse<String> resp = authService.registerUser(request.getEmail(), request.getUname(), request.getPassword(), 
				request.getConfirmPassword());
		return ResponseEntity.ok(resp);
	}
	
	@PostMapping("/check-account")
	public ResponseEntity<AppResponse<String>> checkAccount(@Valid @RequestBody CheckAccountRequest request) {
		AppResponse<String> resp = authService.checkAccountForReset(request.getEmail());
		return ResponseEntity.ok(resp);
	}
	
	@PostMapping("/activate-account")
	public ResponseEntity<AppResponse<String>> activateAccount(@Valid @RequestBody ActivateAccountRequest request){
		AppResponse<String> resp = authService.activateAccount(request.getCode());
		return ResponseEntity.ok(resp);
	}
	
	@PostMapping("/reset-password")
	public ResponseEntity<AppResponse<String>> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
		AppResponse<String> resp = authService.resetPassword(request.getCode(), request.getPassword(), request.getConfirmPassword());
		return ResponseEntity.ok(resp);
	}
	
	private void addRefreshCookie(HttpServletResponse response, String refreshToken) {
		Cookie cookie  = new Cookie("refreshToken", refreshToken);
		cookie.setHttpOnly(true);
		cookie.setSecure(true);
		cookie.setPath("/");
		cookie.setMaxAge(7 * 24 * 60 * 60);
		cookie.setAttribute("SameSite", "Strict");
		response.addCookie(cookie);
	}
	
	private String extractRefreshCookie(HttpServletRequest request) {
		if(request.getCookies() == null) return null;
	
		return Arrays.stream(request.getCookies())
				.filter(c -> "refreshToken".equals(c.getName()))
				.map(Cookie::getValue)
				.findFirst()
				.orElseThrow(() -> new UnauthorizedException("No refresh token"));
	}
	
	@PostMapping("/refresh")
	public ResponseEntity<AppResponse<String>> refresh(HttpServletRequest request, HttpServletResponse response){
		String refreshToken  = extractRefreshCookie(request);
		AppResponse<LoginToken> resp = refreshService.refresh(refreshToken);
		addRefreshCookie(response, resp.getData().refreshToken());
		return ResponseEntity.ok(AppResponse.success("Refreshed", resp.getData().accessToken()));
	}
	
	@PostMapping("/logout")
	public ResponseEntity<AppResponse<String>> logout(Authentication authentication,HttpServletRequest request, HttpServletResponse response) {
	    String refreshToken = extractRefreshCookie(request);
	    refreshService.logout(refreshToken);
	    clearCookie(response);
	    return ResponseEntity.ok(AppResponse.success("Logged out"));
	}
	
	
	@PostMapping("/logout-all")
	public ResponseEntity<AppResponse<String>> logout(Authentication authentication, HttpServletResponse response) {
	    UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
	    refreshService.logoutAll(user.getUser().getId());
	    clearCookie(response);
	    return ResponseEntity.ok(AppResponse.success("Logged out all devices"));
	}

	private void clearCookie(HttpServletResponse response) {
		Cookie cookie = new Cookie("refreshToken", null);
		cookie.setHttpOnly(true);
		cookie.setSecure(true);
		cookie.setPath("/");
		cookie.setMaxAge(0);
		response.addCookie(cookie);
	}

}
