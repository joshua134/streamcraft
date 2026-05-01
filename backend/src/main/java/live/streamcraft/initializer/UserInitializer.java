package live.streamcraft.initializer;

import org.springframework.context.annotation.DependsOn;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import live.streamcraft.entity.Role;
import live.streamcraft.entity.User;
import live.streamcraft.exception.NotFoundException;
import live.streamcraft.repository.RoleRepository;
import live.streamcraft.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@DependsOn("roleInitializer")
public class UserInitializer {
	private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    
    @PostConstruct
    @Transactional
    public void initAdminUser() {
    	log.info("Checking for admin user...");
    	
    	Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new NotFoundException("ADMIN role not found! Run DataInitializer first."));
    	
    	boolean adminExists = userRepository.findAll().stream()
                .anyMatch(user -> user.getRole() != null && adminRole.getName().equals(user.getRole().getName()));
    	
    	if (!adminExists) {
            log.warn("No admin user found. Creating default admin...");
            
            User adminUser = User.builder()
                .uname("admin")
                .email("admin@livestream.com")
                .password(passwordEncoder.encode("Admin@123"))
                .enabled(true)
                .locked(false)
                .role(adminRole)
                .streamsCreatedCount(0)
                .build();
            
            userRepository.save(adminUser);
            
            log.warn("========================================");
            log.warn("DEFAULT ADMIN CREATED - PLEASE CHANGE PASSWORD");
            log.warn("Username: admin");
            log.warn("Email: admin@livestream.com");
            log.warn("Temporary Password: Admin@123");
            log.warn("========================================");
        } else {
            log.info("Admin user already exists, skipping creation");
        }
    	
    }
}
