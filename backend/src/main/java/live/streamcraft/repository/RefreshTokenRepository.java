package live.streamcraft.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import live.streamcraft.entity.RefreshToken;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

	@Query("SELECT t FROM RefreshToken t WHERE expiryDate > :now")
	void deleteExpiredTokens(@Param("now") LocalDateTime now);

	Optional<RefreshToken> findByTokenHash(String hash);

	@Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user.id = :id AND rt.revoked = false")
	void revokeAllByUserId(@Param("id") String id);
	
	List<RefreshToken> findTop5ByUserIdOrderByCreatedAtDesc(String userId);
	
}
