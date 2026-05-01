package live.streamcraft.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import live.streamcraft.entity.RefreshToken;
import live.streamcraft.entity.User;
import live.streamcraft.exception.InvalidTokenException;
import live.streamcraft.exception.NotFoundException;
import live.streamcraft.exception.UnauthorizedException;
import live.streamcraft.model.response.AppResponse;
import live.streamcraft.model.response.LoginToken;
import live.streamcraft.repository.RefreshTokenRepository;
import live.streamcraft.repository.UserRepository;
import live.streamcraft.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshService {
	private final RefreshTokenRepository refreshTokenRepository;
	private final JwtService jwtService;
	private final UserRepository userRepository;
	
	@Value("${user.new-days:7}")
    private int NEW_USER_DAYS;
	
	@Transactional
	public AppResponse<LoginToken> refresh(String refreshTokenRaw){
		String hash = CodeGenerator.hashToken(refreshTokenRaw);
		RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
				.orElseThrow(() -> new NotFoundException("Invalid refresh token"));
		
		if(token.isRevoked() || token.getExpiryDate().isBefore(LocalDateTime.now())) {
			throw new UnauthorizedException("Expired or revoked refresh token.");
		}
		
		token.setRevoked(true);
		RefreshToken savedRefreshToken = refreshTokenRepository.save(token);
		
		User user = userRepository.findById(savedRefreshToken.getUser().getId())
				.orElseThrow(() -> new NotFoundException("Account not found."));
		
		String newAccessToken = jwtService.generateAccessToken(user);
		String newRefreshToken = UUID.randomUUID().toString();
		
		RefreshToken refreshEntity = RefreshToken.builder()
				.tokenHash(CodeGenerator.hashToken(newRefreshToken))
				.user(user)
				.expiryDate(LocalDateTime.now().plusDays(NEW_USER_DAYS))
				.revoked(false)
				.lastUsedAt(LocalDateTime.now())
				.build();
		
		refreshTokenRepository.save(refreshEntity);
		return AppResponse.success("Token accessed again.", new LoginToken(newAccessToken, newRefreshToken));
	}

	@Transactional
	public void logout(String refreshToken) {
		String hash = CodeGenerator.hashToken(refreshToken);
		RefreshToken token = findByHash(hash);
		token.setRevoked(true);
		refreshTokenRepository.save(token);
	}
	
	public RefreshToken findByHash(String hash) {
		return refreshTokenRepository.findByTokenHash(hash).orElseThrow(() -> new InvalidTokenException("Invalid Token."));
	}

	public void logoutAll(String id) {
		refreshTokenRepository.revokeAllByUserId(id);
	}
}
