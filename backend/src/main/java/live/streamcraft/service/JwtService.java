package live.streamcraft.service;



import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import live.streamcraft.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class JwtService {
	@Value("${jwt.secret}")
    private String secretKey;
    
    @Value("${jwt.access-token-expiration}")
    private long accessExpiration;
    
    @Value("${jwt.refresh-token-expiration}")
    private long refreshExpiration;
    
    @Value("${jwt.issuer}")
    private String issuer;
    
    public String generateAccessToken(User user) {
        return Jwts.builder()
                .subject(user.getId())
                .claim("email", user.getEmail())
                .claim("username", user.getUname())
                .claim("role", user.getRole().getName())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpiration))
                .issuer(issuer)
                .signWith(getSignKey())
                .compact();
    }
    
    public String generateRefreshToken(User user) {
        return Jwts.builder()
                .subject(user.getId())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .issuer(issuer)
                .signWith(getSignKey())
                .compact();
    }
    
    private SecretKey getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    
    public String extractUserId(String token) {
        return extractAllClaims(token).getSubject();
    }
    
    public String extractEmail(String token) {
        return extractAllClaims(token).get("email", String.class);
    }
    
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }
    
    public boolean isTokenValid(String token, User user) {
        final String userId = extractUserId(token);
        return (userId.equals(user.getId())) && !isTokenExpired(token);
    }
    
    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }
}
