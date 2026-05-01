package live.streamcraft.initializer;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import live.streamcraft.entity.Role;
import live.streamcraft.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoleInitializer {
	private final RoleRepository roleRepository;
	
	@PostConstruct
    @Transactional
    public void initData() {
        log.info("Starting role initialization...");
        initRoles();
        log.info("Role initialization completed");
    }

	private void initRoles() {
		if(roleRepository.count() == 0) {
			log.info("Seeding initial roles...");
            
            Role admin = Role.builder()
                .name("ADMIN")
                .description("Full system access")
                .build();
            
            Role moderator = Role.builder()
                .name("MODERATOR")
                .description("Can moderate streams and users")
                .build();
            
            Role user = Role.builder()
                .name("USER")
                .description("Regular user can create streams and chat")
                .build();
            
            roleRepository.saveAll(List.of(admin, moderator, user));
            log.info("Added 3 roles: ADMIN, MODERATOR, USER");
		} else {
            log.info("Roles already exist ({} found), skipping initialization", roleRepository.count());
        }
	}
}
