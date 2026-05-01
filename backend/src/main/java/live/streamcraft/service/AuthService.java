package live.streamcraft.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;
import live.streamcraft.entity.RefreshToken;
import live.streamcraft.entity.Role;
import live.streamcraft.entity.User;
import live.streamcraft.exception.AccountHideMessageException;
import live.streamcraft.exception.DuplicateException;
import live.streamcraft.exception.InvalidCredentialsException;
import live.streamcraft.exception.InvalidTokenException;
import live.streamcraft.exception.NotFoundException;
import live.streamcraft.exception.ServiceException;
import live.streamcraft.exception.UnauthorizedException;
import live.streamcraft.model.response.AppResponse;
import live.streamcraft.model.response.LoginToken;
import live.streamcraft.repository.RefreshTokenRepository;
import live.streamcraft.repository.RoleRepository;
import live.streamcraft.repository.UserRepository;
import live.streamcraft.security.UserPrincipal;
import live.streamcraft.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
	private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenRepository refreshTokenRepository;
    
    @Value("${user.free-stream-limit:4}")
    private int FREE_STREAM_LIMIT;
    
    @Value("${user.new-days:7}")
    private int NEW_USER_DAYS;
    
    public AppResponse<LoginToken> loginUser(String email, String password, HttpServletRequest request) {
        log.info("Starting login flow for email: {}", email);

        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));

        if (!(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            log.error("Login failed: Principal is type {}, expected UserPrincipal",authentication.getPrincipal().getClass().getName());
            throw new ServiceException("Authentication returned invalid principal type");
        }

        User user = principal.getUser();
        log.info("User authenticated: {} (ID: {})", user.getEmail(), user.getId());

        String accessToken = jwtService.generateAccessToken(user);
        String refreshTokenValue = UUID.randomUUID().toString();

        RefreshToken refreshEntity = RefreshToken.builder()
                .tokenHash(CodeGenerator.hashToken(refreshTokenValue))
                .user(user)
                .expiryDate(LocalDateTime.now().plusDays(NEW_USER_DAYS))
                .revoked(false)
                .device(request.getHeader("User-Agent"))
                .ipAddress(request.getRemoteAddr())
                .lastUsedAt(LocalDateTime.now())
                .build();

        refreshTokenRepository.save(refreshEntity);
        log.info("Refresh token saved successfully for user: {}", user.getEmail());

        LoginToken loginToken = new LoginToken(accessToken, refreshTokenValue);
        log.info("Login completed for user: {}", user.getEmail());
        
        return AppResponse.success("Logged in successfully", loginToken);
    }


	@Transactional
	public AppResponse<String> registerUser(String email, String uname, String password, String confirmPassword) {
		if (!password.equals(confirmPassword)) {
            throw new InvalidCredentialsException("Passwords do not match");
        }
		
		if (userRepository.existsByEmail(email)) {
			throw new DuplicateException("Email already registered");
		}

		if (userRepository.existsByUname(uname)) {
			throw new DuplicateException("Username already taken");
		}
		
		Role userRole = roleRepository.findByName("USER") .orElseThrow(() -> new NotFoundException("Default role not found"));
		
		String activationCode = CodeGenerator.code();
		
		User user = User.builder().uname(uname).email(email)
				.password(passwordEncoder.encode(password))
				.role(userRole)
				.enabled(false)
				.locked(false)
				.streamsCreatedCount(0)
				.emailVerificationCode(activationCode)
				.emailVerificationCodeExpiry(LocalDateTime.now().plusHours(1))
				.build();
		
		User savedUser = userRepository.save(user);
		
		log.info("registration activation code is "+ savedUser.getEmailVerificationCode());
		
		emailService.sendActivationEmail(savedUser.getEmail(), savedUser.getUname(), savedUser.getEmailVerificationCode());
		
		return AppResponse.success("Registration successful. Please check your email INBOX/SPAM folder for verification code. Remember code expires after 1 hour.");
	}
	
	 @ExceptionHandler(BindException.class)
	 public ResponseEntity<AppResponse<Map<String, String>>> handleBindException(BindException ex) {

		 System.out.println("=== BindException HANDLER CALLED ===");

		 Map<String, String> errors = new HashMap<>();
		 ex.getBindingResult().getFieldErrors().forEach(error -> 
		 errors.put(error.getField(), error.getDefaultMessage())
				 );

		 log.error("BindException - Validation failed: {}", errors);

		 String firstMessage = errors.values().iterator().next();
		 return ResponseEntity.badRequest().body(AppResponse.error(firstMessage, errors));
	 }
	
	@Transactional
	public AppResponse<String> checkAccountForReset(String email) {
		final String message = "If account is present, an email has been sent containing the password reset code.";
        User user = userRepository.findByEmail(email).orElseThrow(() -> new AccountHideMessageException(message));
        
        if (!user.isEnabled()) {
            throw new UnauthorizedException("Account not verified. Please verify your email first.");
        }
        
        if(user.isLocked()) {
        	throw new UnauthorizedException("Account is locked. Contact administrator.");
        }
        
        String resetCode = CodeGenerator.code();
        
        user.setPasswordResetCode(resetCode);
        user.setPasswordResetCodeExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);
        
        // Send reset code via email
        emailService.sendPasswordResetEmail(user.getEmail(), resetCode);
        
        return AppResponse.success(message);
    }

	@Transactional
	public AppResponse<String> resetPassword(String code, String password, String confirmPassword) {
		if (!password.equals(confirmPassword)) {
			throw new InvalidCredentialsException("Passwords do not match");
		}

		User user = userRepository.findByPasswordResetCode(code)
				.orElseThrow(() -> new NotFoundException("Invalid or expired reset code"));

		if (user.getPasswordResetCodeExpiry().isBefore(LocalDateTime.now())) {
			throw new IllegalArgumentException("Reset code has expired");
		}

		// Update password
		user.setPassword(passwordEncoder.encode(password));
		user.setPasswordResetCode(null);
		user.setPasswordResetCodeExpiry(null);

		refreshTokenRepository.revokeAllByUserId(user.getId());
		userRepository.save(user);
		return AppResponse.success("Password has been reset");
	}
	
	@Transactional
	public AppResponse<String> resendActivationCode(String email){
		User user = userService.findByEmail(email);
		
		String activationCode = CodeGenerator.code();
		
		user.setEmailVerificationCode(activationCode);
		user.setEmailVerificationCodeExpiry(LocalDateTime.now().plusHours(1));
		
		User savedUser = userRepository.save(user);
		
		emailService.resendActivationEmail(savedUser.getEmail(), savedUser.getUname(), savedUser.getEmailVerificationCode());
		
		return AppResponse.success("Please check your email INBOX/SPAM folder for verification code. Remember code expires after 1 hour.");
	}
	
	@Transactional
	public AppResponse<String> resendPasswordResetCode(String email){
		User user = userService.findByEmail(email);
		
		String activationCode = CodeGenerator.code();
		
		user.setPasswordResetCode(activationCode);
		user.setPasswordResetCodeExpiry(LocalDateTime.now().plusHours(1));
		
		User savedUser = userRepository.save(user);
		
		emailService.resendPasswordResetEmail(savedUser.getEmail(), savedUser.getEmailVerificationCode());
		
		return AppResponse.success("Please check your email INBOX/SPAM folder for password reset code. Remember code expires after 1 hour.");
	}


	@Transactional
	public AppResponse<String> activateAccount(String code) {
		User user = userRepository.findByEmailVerificationCode(code)
				.orElseThrow(() -> new InvalidTokenException("Invalid or expired verification code"));

		if (user.getEmailVerificationCodeExpiry() == null || user.getEmailVerificationCodeExpiry().isBefore(LocalDateTime.now())) {
			throw new InvalidTokenException("Verification code has expired. Please request a new one.");
		}

		if (user.isEnabled()) {
			return AppResponse.success("Account already activated. You can now log in.");
		}

		user.setEnabled(true);
		user.setEmailVerificationCode(null); 
		user.setEmailVerificationCodeExpiry(null);

		userRepository.save(user);
		log.info("Account activated for user: {}", user.getEmail());
		return AppResponse.success("Account activated successfully. You can now log in.");
	}
}
