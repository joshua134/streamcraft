package live.streamcraft.initializer;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import live.streamcraft.entity.StreamCategory;
import live.streamcraft.repository.StreamCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class StreamCategoryInitializer {
	private final StreamCategoryRepository streamCategoryRepository;
	
	@PostConstruct
    @Transactional
    public void initData() {
        log.info("Starting stream category initialization...");
        initStreamCategory();
        log.info("Stream Category initialization completed");
    }

	private void initStreamCategory() {
		if (streamCategoryRepository.count() == 0) {
			log.info("Seeding initial stream categories...");

			List<StreamCategory> categories = List.of(
					createCategory("Gaming", "Video game live streams", 1),
					createCategory("Music", "Live music performances, concerts, DJ sets", 2),
					createCategory("Talk Shows", "Discussions, interviews, podcasts", 3),
					createCategory("Education", "Tutorials, workshops, online classes", 4),
					createCategory("Sports", "Live sports events, commentary, analysis", 5),
					createCategory("Technology", "Tech reviews, coding, gadget unboxings", 6),
					createCategory("Art & Design", "Drawing, painting, digital art streams", 7),
					createCategory("Cooking", "Live cooking shows, recipes, food challenges", 8),
					createCategory("Travel", "Live travel vlogs, destination tours", 9),
					createCategory("Just Chatting", "Casual conversations with audience", 10)
					);

			streamCategoryRepository.saveAll(categories);
			log.info("Added {} stream categories", categories.size());
		} else {
			log.info("Categories already exist ({} found), skipping initialization", streamCategoryRepository.count());
		}
	}

	private StreamCategory createCategory(String name, String description, int displayOrder) {
		return StreamCategory.builder()
				.name(name)
				.description(description)
				.active(true)
				.displayOrder(displayOrder)
				.build();
	}
}
