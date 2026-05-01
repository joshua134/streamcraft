package live.streamcraft.service;

import live.streamcraft.entity.Role;
import live.streamcraft.entity.User;
import live.streamcraft.exception.DuplicateException;
import live.streamcraft.exception.InvalidCredentialsException;
import live.streamcraft.exception.NotFoundException;
import live.streamcraft.exception.UnauthorizedException;
import live.streamcraft.model.enums.NotificationPriority;
import live.streamcraft.model.enums.NotificationType;
import live.streamcraft.model.request.ChangePasswordRequest;
import live.streamcraft.model.request.NotificationCreateRequest;
import live.streamcraft.model.request.UpdateProfileRequest;
import live.streamcraft.model.response.PageDto;
import live.streamcraft.model.response.UserProfileDto;
import live.streamcraft.model.response.UserStatisticsDto;
import live.streamcraft.repository.RoleRepository;
import live.streamcraft.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
	private final PasswordEncoder passwordEncoder;
	private final NotificationService notificationService;
	private final UserRepository userRepository;
	private final RoleRepository roleRepository;

	public User findByEmail(String email) {
		return userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("Account not found."));
	}

	public User findByUname(String uname) {
		return userRepository.findByUname(uname).orElseThrow(() -> new NotFoundException("Account not found."));
	}

	public User findById(String id) {
		return userRepository.findById(id).orElseThrow(() -> new NotFoundException("Account not found."));
	}

	@Transactional(readOnly = true)
	public PageDto<UserProfileDto> getAllUsers(Pageable pageable) {
		Page<UserProfileDto> page = userRepository.findAll(pageable).map(this::toUserProfileDto);
		return PageDto.from(page);
	}

	@Transactional(readOnly = true)
	public List<UserProfileDto> searchUsers(String searchTerm) {
		List<User> users = userRepository.findByEmailContainingOrUnameContaining(searchTerm, searchTerm);
		return users.stream().map(this::toUserProfileDto).collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public PageDto<UserProfileDto> getUsersByEnabledStatus(boolean enabled, Pageable pageable) {
		Page<UserProfileDto> page;
		if (enabled) {
			page = userRepository.findByEnabledTrue(pageable)
					.map(this::toUserProfileDto);
		} else {
			page = userRepository.findByEnabledFalse(pageable)
					.map(this::toUserProfileDto);
		}
		return PageDto.from(page);
	}

	@Transactional(readOnly = true)
	public PageDto<UserProfileDto> getUsersByLockedStatus(boolean locked, Pageable pageable) {
		Page<UserProfileDto> page;
		if (locked) {
			page = userRepository.findByLockedTrue(pageable).map(this::toUserProfileDto);
		} else {
			page = userRepository.findByLockedFalse(pageable).map(this::toUserProfileDto);
		}
		return PageDto.from(page);
	}

	@Transactional(readOnly = true)
	public PageDto<UserProfileDto> getSubscribedUsers(Pageable pageable) {
		LocalDateTime now = LocalDateTime.now();
		Page<UserProfileDto> page = userRepository.findBySubscriptionEndDateAfter(now, pageable).map(this::toUserProfileDto);
		return PageDto.from(page);
	}

	@Transactional(readOnly = true)
	public PageDto<UserProfileDto> getUnverifiedUsers(Pageable pageable) {
		Page<UserProfileDto> page = userRepository.findByEnabledFalseAndEmailVerificationCodeIsNotNull(pageable)
				.map(this::toUserProfileDto);
		return PageDto.from(page);
	}

	@Transactional(readOnly = true)
	public PageDto<UserProfileDto> getRecentUsers(Pageable pageable) {
		LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
		Page<UserProfileDto> page = userRepository.findByCreatedAtAfter(thirtyDaysAgo, pageable)
				.map(this::toUserProfileDto);
		return PageDto.from(page);
	}

	@Transactional(readOnly = true)
	public PageDto<UserProfileDto> getUsersByRole(String roleName, Pageable pageable) {
		Page<UserProfileDto> page = userRepository.findByRoleName(roleName, pageable)
				.map(this::toUserProfileDto);
		return PageDto.from(page);
	}


	public UserProfileDto updateProfile(String userId, UpdateProfileRequest request) {
		User user = findById(userId);

		if (request.getUname() != null && !request.getUname().equals(user.getUname())) {
			if (userRepository.existsByUname(request.getUname())) {
				throw new DuplicateException("Username already taken: " + request.getUname());
			}
			user.setUname(request.getUname());
		}

		if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
			if (userRepository.existsByEmail(request.getEmail())) {
				throw new DuplicateException("Email already registered: " + request.getEmail());
			}
			user.setEmail(request.getEmail());
		}

		//        if (request.getAvatarUrl() != null) {
		//            user.setAvatarUrl(request.getAvatarUrl());
		//        }

		User updatedUser = userRepository.save(user);
		log.info("User profile updated: {}", userId);

		return toUserProfileDto(updatedUser);
	}

	@Transactional
	public void changePassword(String userId, ChangePasswordRequest request) {
		User user = findById(userId);

		if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
			throw new UnauthorizedException("Current password is incorrect");
		}

		if (!request.getNewPassword().equals(request.getConfirmPassword())) {
			throw new InvalidCredentialsException("New password and confirmation do not match");
		}

		if(passwordEncoder.matches(request.getNewPassword(), user.getPassword()) || 
				passwordEncoder.matches(request.getConfirmPassword(), user.getPassword())) {
			throw new UnauthorizedException("New password must different from current password");
		}

		user.setPassword(passwordEncoder.encode(request.getNewPassword()));
		userRepository.save(user);
		log.info("Password changed for user: {}", userId);
	}

	@Transactional
	public void deleteAccount(String userId) {
		User user = findById(userId);
		user.setDeleted(true);
		user.setDeletedAt(LocalDateTime.now());
		user.setEnabled(false);
		userRepository.save(user);
		log.info("Account deleted for user: {}", userId);
	}
	// admin functions
	@Transactional
	public UserProfileDto enableUser(String adminId, String userId) {
		// Call core service for actual operation
		UserProfileDto result = enableUser(userId);

		notificationService.createNotification(userId, NotificationCreateRequest.builder()
				.title("Account Enabled")
				.message("Your account has been enabled by an administrator.")
				.type(NotificationType.ACCOUNT_ENABLED)
				.priority(NotificationPriority.HIGH)
				.build());

		log.info("Admin {} enabled user {}", adminId, userId);
		return result;
	}

	@Transactional
	public UserProfileDto disableUser(String adminId, String userId, String reason) {
		UserProfileDto result = disableUser(userId);

		notificationService.createNotification(userId, NotificationCreateRequest.builder()
				.title("Account Disabled")
				.message("Your account has been disabled by an administrator. Reason: " + reason)
				.type(NotificationType.ACCOUNT_DISABLED)
				.priority(NotificationPriority.URGENT)
				.build());

		log.info("Admin {} disabled user {}: {}", adminId, userId, reason);
		return result;
	}

	@Transactional
	public UserProfileDto lockUser(String adminId, String userId, String reason) {
		UserProfileDto result = lockUser(userId);

		notificationService.createNotification(userId, NotificationCreateRequest.builder()
				.title("Account Locked")
				.message("Your account has been locked by an administrator. Reason: " + reason)
				.type(NotificationType.ACCOUNT_LOCKED)
				.priority(NotificationPriority.URGENT)
				.build());

		log.info("Admin {} locked user {}: {}", adminId, userId, reason);
		return result;
	}

	@Transactional
	public UserProfileDto unlockUser(String adminId, String userId) {
		UserProfileDto result = unlockUser(userId);

		notificationService.createNotification(userId, NotificationCreateRequest.builder()
				.title("Account Unlocked")
				.message("Your account has been unlocked by an administrator.")
				.type(NotificationType.ACCOUNT_UNLOCKED)
				.priority(NotificationPriority.HIGH)
				.build());

		log.info("Admin {} unlocked user {}", adminId, userId);
		return result;
	}

	@Transactional
	public UserProfileDto changeUserRole(String adminId, String userId, String roleName) {
		User user = findById(userId);

		Role newRole = roleRepository.findByName(roleName)
				.orElseThrow(() -> new NotFoundException("Role not found: " + roleName));

		String oldRole = user.getRole().getName();
		user.setRole(newRole);
		User updatedUser = userRepository.save(user);


		notificationService.createNotification(userId, NotificationCreateRequest.builder()
				.title("Role Changed")
				.message("Your role has been changed from " + oldRole + " to " + roleName)
				.type(NotificationType.ROLE_CHANGED)
				.priority(NotificationPriority.HIGH)
				.build());

		log.info("Admin {} changed role for user {} from {} to {}", adminId, userId, oldRole, roleName);

		return toUserProfileDto(updatedUser);
	}


	@Transactional
	public int bulkEnableUsers(String adminId, List<String> userIds) {
		List<User> users = userRepository.findAllById(userIds);
		users.forEach(user -> user.setEnabled(true));
		userRepository.saveAll(users);

		for (User user : users) {
			notificationService.createNotification(user.getId(), NotificationCreateRequest.builder()
					.title("Account Enabled")
					.message("Your account has been enabled by an administrator.")
					.type(NotificationType.ACCOUNT_ENABLED)
					.priority(NotificationPriority.MEDIUM)
					.build());
		}

		log.info("Admin {} bulk enabled {} users", adminId, users.size());
		return users.size();
	}

	@Transactional
	public int bulkDisableUsers(String adminId, List<String> userIds, String reason) {
		List<User> users = userRepository.findAllById(userIds);
		users.forEach(user -> user.setEnabled(false));
		userRepository.saveAll(users);

		
		for (User user : users) {
			notificationService.createNotification(user.getId(), NotificationCreateRequest.builder()
					.title("Account Disabled")
					.message("Your account has been disabled by an administrator. Reason: " + reason)
					.type(NotificationType.ACCOUNT_DISABLED)
					.priority(NotificationPriority.HIGH)
					.build());
		}

		log.info("Admin {} bulk disabled {} users", adminId, users.size());
		return users.size();
	}


	@Transactional(readOnly = true)
	public UserStatisticsDto getUserStatistics() {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
		LocalDateTime startOfWeek = now.minusDays(7);

		long totalUsers = userRepository.count();  
		long enabledUsers = userRepository.countByEnabledTrue();
		long disabledUsers = userRepository.countByEnabledFalse();
		long lockedUsers = userRepository.countByLockedTrue();
		long unverifiedUsers = userRepository.countByEnabledFalseAndEmailVerificationCodeIsNotNull();
		long subscribedUsers = userRepository.countBySubscriptionEndDateAfter(now);
		long adminUsers = userRepository.countByRoleName("ADMIN");
		long moderatorUsers = userRepository.countByRoleName("MODERATOR");
		long regularUsers = userRepository.countByRoleName("USER");
		long usersJoinedThisMonth = userRepository.countByCreatedAtAfter(startOfMonth);
		long usersJoinedLastWeek = userRepository.countByCreatedAtAfter(startOfWeek);

		return UserStatisticsDto.builder()
				.totalUsers(totalUsers)
				.enabledUsers(enabledUsers)
				.disabledUsers(disabledUsers)
				.lockedUsers(lockedUsers)
				.unverifiedUsers(unverifiedUsers)
				.subscribedUsers(subscribedUsers)
				.adminUsers(adminUsers)
				.moderatorUsers(moderatorUsers)
				.regularUsers(regularUsers)
				.usersJoinedThisMonth(usersJoinedThisMonth)
				.usersJoinedLastWeek(usersJoinedLastWeek)
				.build()
				.calculatePercentages();
	}

	public UserProfileDto enableUser(String userId) {
		User user = findById(userId);
		user.setEnabled(true);
		user.setEnabledAt(LocalDateTime.now());
		User updatedUser = userRepository.save(user);
		log.info("User enabled: {}", userId);
		return toUserProfileDto(updatedUser);
	}

	public UserProfileDto disableUser(String userId) {
		User user = findById(userId);
		user.setEnabled(false);
		User updatedUser = userRepository.save(user);
		log.info("User disabled: {}", userId);
		return toUserProfileDto(updatedUser);
	}

	public UserProfileDto lockUser(String userId) {
		User user = findById(userId);
		user.setLocked(true);
		user.setLockedAt(LocalDateTime.now());
		User updatedUser = userRepository.save(user);
		log.info("User locked: {}", userId);
		return toUserProfileDto(updatedUser);
	}

	public UserProfileDto unlockUser(String userId) {
		User user = findById(userId);
		user.setLocked(false);
		user.setLockedAt(null);
		User updatedUser = userRepository.save(user);
		log.info("User unlocked: {}", userId);
		return toUserProfileDto(updatedUser);
	}

	public UserProfileDto changeUserRole(String userId, String roleName) {
		User user = findById(userId);
		// Role validation happens in AdminUserService
		// This just updates the role
		return toUserProfileDto(user);
	}

	public UserProfileDto toUserProfileDto(User user) {
		return UserProfileDto.builder()
				.id(user.getId())
				.uname(user.getUname())
				.email(user.getEmail())
				.avatarUrl(user.getAvatarUrl())
				.enabled(user.isEnabled())
				.locked(user.isLocked())
				.subscribed(user.isSubscribed())
				.subscriptionEndDate(user.getSubscriptionEndDate())
				.streamsCreatedCount(user.getStreamsCreatedCount())
				.remainingFreeStreams(user.getRemainingFreeStreams())
				.canCreateStream(user.canCreateStream())
				.role(user.getRole() != null ? user.getRole().getName() : "USER")
				.createdAt(user.getCreatedAt())
				.build();
	}
}