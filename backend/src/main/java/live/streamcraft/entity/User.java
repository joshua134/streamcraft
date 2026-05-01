package live.streamcraft.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.annotations.NaturalId;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name="tbl_user", indexes= {
		@Index(name="idx_user_email", columnList="email"),
		@Index(name = "idx_user_uname", columnList = "uname"),
		@Index(name = "idx_user_enabled", columnList = "enabled"),
		@Index(name = "idx_user_created_at", columnList = "created_at"),
		@Index(name = "idx_user_locked", columnList = "locked"),
		@Index(name = "idx_user_subscription_end", columnList="subscriptionEndDate")
})
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@EqualsAndHashCode(callSuper = true, exclude = {"role", "chatMessages", "ownedStreams","refreshTokens"})
@ToString(exclude = {"role", "chatMessages", "ownedStreams","refreshTokens"})
public class User extends BaseEntity {

	@NaturalId
	@NotBlank(message = "Username is required")
	@Size(min = 4, max = 50, message = "Username must be between 4 and 50 characters")
	@Column(unique = true, nullable = false, length = 50)
	private String uname;

	@NaturalId
	@NotBlank(message = "Email is required")
	@Email(message = "Invalid email address")
	@Column(unique = true, nullable = false, length = 100)
	private String email;

	@NotBlank(message = "Password is required")
	@Size(min = 8, message = "Password must be at least 8 characters")
	@Column(nullable = false)
	private String password;

	@Column(length = 500)
	private String avatarUrl;

	@Builder.Default
	private boolean locked = false;

	@Builder.Default
	private boolean enabled = false;

	private LocalDateTime lockedAt;
	private LocalDateTime enabledAt;

	@Builder.Default
	private int streamsCreatedCount = 0;

	private LocalDateTime firstStreamDate;

	@Builder.Default
	private LocalDateTime subscriptionEndDate = null;

	@Builder.Default
	private String emailVerificationCode = null;

	private LocalDateTime emailVerificationCodeExpiry;

	@Builder.Default
	private String passwordResetCode = null;

	private LocalDateTime passwordResetCodeExpiry;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "role_id", nullable = false)
	private Role role;

	@OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@Builder.Default
	private List<Chat> chatMessages = new ArrayList<>();

	@OneToMany(mappedBy = "streamer", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@Builder.Default
	private List<Stream> ownedStreams = new ArrayList<>();
	
	@OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@Builder.Default
	private List<RefreshToken> refreshTokens = new ArrayList<>();

	@Transient
	public boolean isNewUser() {
		return createdAt != null && createdAt.plusDays(7).isAfter(LocalDateTime.now());
	}

	@Transient
	public boolean isSubscribed() {
		return subscriptionEndDate != null && subscriptionEndDate.isAfter(LocalDateTime.now());
	}

	@Transient
	public boolean canCreateStream() {
		if (isSubscribed()) {
			return true;
		}

		if (isNewUser()) {
			return streamsCreatedCount < 4;
		}

		return false;
	}

	@Transient
	public int getRemainingFreeStreams() {
		if (!isNewUser()) return 0;
		return Math.max(0, 4 - streamsCreatedCount);
	}

	@PrePersist
	@PreUpdate
	public void validateActivationCode() {
		if (emailVerificationCode != null && emailVerificationCodeExpiry == null) {
			emailVerificationCodeExpiry = LocalDateTime.now().plusHours(24);
		}
	}

	@Transient
	public boolean isActivationCodeValid() {
		return emailVerificationCode != null && 
				emailVerificationCodeExpiry != null && 
				emailVerificationCodeExpiry.isAfter(LocalDateTime.now());
	}
	
	@Transient
	public String getAvatarUrl() {
		if (this.avatarUrl != null && !this.avatarUrl.isEmpty()) {
			return this.avatarUrl;
		}
		return generateAvatarFromUsername(this.uname);
	}

	@Transient
	private String generateAvatarFromUsername(String username) {
		if (username == null || username.isEmpty()) {
			username = "User";
		}

		String firstChar = username.substring(0, 1).toUpperCase();
		String backgroundColor = getRandomColor(username);

		return String.format("https://api.dicebear.com/7.x/initials/svg?seed=%s&backgroundColor=%s&fontSize=48&radius=50&size=128",
				firstChar,backgroundColor);
	}

	@Transient
	private String getRandomColor(String seed) {
		List<String> colors = Arrays.asList("FF6B6B", "4ECDC4", "45B7D1", "96CEB4", "FFEAA7", "DDA0DD", "98D8C8", "F7B787", "5E5B71", "FF9F4A",
				"6C5CE7", "A8E6CF", "FFD3B6", "FF8B94", "A8E6D8","FDCB6E", "E76F51", "F4A261", "E9C46A", "2A9D8F",
				"1A535C", "4ECDC4", "FF6B6B", "FFE66D", "292F36");
		int index = Math.abs(seed.hashCode()) % colors.size();
		return colors.get(index);
	}
}
