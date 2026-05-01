package live.streamcraft.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import live.streamcraft.entity.StreamCategory;
import live.streamcraft.exception.DuplicateException;
import live.streamcraft.exception.NotFoundException;
import live.streamcraft.model.request.StreamCategoryImportDto;
import live.streamcraft.model.response.AppResponse;
import live.streamcraft.model.response.BatchImportResult;
import live.streamcraft.repository.StreamCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class StreamCategoryService {
	private final StreamCategoryRepository streamCategoryRepository;

	public StreamCategory findById(String id) {
		return streamCategoryRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Stream category not found."));
	}

	public StreamCategory findByName(String name) {
		return streamCategoryRepository.findByNameIgnoreCase(name)
				.orElseThrow(() -> new NotFoundException("Stream category not found."));
	}

	public List<StreamCategory> findAll() {
		return streamCategoryRepository.findAll();
	}

	@Transactional
	public AppResponse<StreamCategory> createStreamCategory(String name, String description, int displayOrder, String iconUrl) {
		if (streamCategoryRepository.existsByNameIgnoreCase(name)) {
			throw new DuplicateException("Stream category already exists.");
		}

		StreamCategory category = StreamCategory.builder()
				.name(name)
				.description(description)
				.displayOrder(displayOrder)
				.iconUrl(iconUrl)
				.active(true)
				.build();

		StreamCategory saved = streamCategoryRepository.save(category);
		log.info("Created stream category: {}", name);

		return AppResponse.success("Category created successfully", saved);
	}

	public AppResponse<StreamCategory> updateCategory(String id, String name, String description,
			Integer displayOrder, String iconUrl, Boolean active) {
		StreamCategory category = findById(id);

		if (name != null && !name.equals(category.getName())) {
			if (streamCategoryRepository.existsByNameIgnoreCase(name)) {
				throw new DuplicateException("Category name already exists: " + name);
			}
			category.setName(name);
		}

		if (description != null) category.setDescription(description);
		if (displayOrder != null) category.setDisplayOrder(displayOrder);
		if (iconUrl != null) category.setIconUrl(iconUrl);
		if (active != null) category.setActive(active);

		StreamCategory updated = streamCategoryRepository.save(category);
		log.info("Updated stream category: {}", updated.getName());

		return AppResponse.success("Category updated successfully", updated);
	}

	public AppResponse<Void> deleteCategory(String id) {
		StreamCategory category = findById(id);
		category.setDeleted(true);
		category.setActive(false);
		category.setDeletedAt(LocalDateTime.now());

		streamCategoryRepository.save(category);
		log.info("Soft deleted stream category: {}", category.getName());

		return AppResponse.success("Category deleted successfully");
	}

	public StreamCategory getCategoryById(String id) {
		return streamCategoryRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Category not found with id: " + id));
	}

	@Transactional(readOnly = true)
	public List<StreamCategory> getAllCategories(boolean includeDeleted, boolean activeOnly, String sortBy) {
		Sort sort = Sort.by("displayOrder").ascending();

		if ("name".equalsIgnoreCase(sortBy)) {
			sort = Sort.by("name").ascending();
		} else if ("createdAt".equalsIgnoreCase(sortBy)) {
			sort = Sort.by("createdAt").descending();
		}

		if (activeOnly) {
			return streamCategoryRepository.findByActiveTrueOrderByDisplayOrderAsc();
		}

		if (includeDeleted) {
			return streamCategoryRepository.findAll(sort);
		}

		return streamCategoryRepository.findAll(sort).stream()
				.filter(c -> !c.isDeleted())
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<StreamCategory> getCategoriesByActiveStatus(boolean active) {
		if (active) {
			return streamCategoryRepository.findByActiveTrueOrderByDisplayOrderAsc();
		}
		
		return streamCategoryRepository.findAll().stream()
				.filter(c -> !c.isActive())
				.collect(Collectors.toList());
	}

	public AppResponse<StreamCategory> toggleCategoryStatus(String id) {
		StreamCategory category = findById(id);
		category.setActive(!category.isActive());
		StreamCategory saved = streamCategoryRepository.save(category);

		String status = saved.isActive() ? "activated" : "deactivated";
		log.info("Category {} {}", saved.getName(), status);

		return AppResponse.success("Category " + status + " successfully", saved);
	}

	public AppResponse<BatchImportResult> batchCreateCategories(List<StreamCategoryImportDto> categories) {
		int created = 0;
		int skipped = 0;
		List<String> skippedNames = new ArrayList<>();

		for (StreamCategoryImportDto dto : categories) {
			if (streamCategoryRepository.existsByNameIgnoreCase(dto.getName())) {
				skipped++;
				skippedNames.add(dto.getName());
				log.warn("Category already exists, skipping: {}", dto.getName());
				continue;
			}

			StreamCategory category = StreamCategory.builder()
					.name(dto.getName())
					.description(dto.getDescription())
					.displayOrder(dto.getDisplayOrder())
					.iconUrl(dto.getIconUrl())
					.active(true)
					.build();

			streamCategoryRepository.save(category);
			created++;
		}

		BatchImportResult result = new BatchImportResult(created, skipped, skippedNames);
		log.info("Batch import completed: {} created, {} skipped", created, skipped);

		return AppResponse.success("Import completed", result);
	}

	public AppResponse<BatchImportResult> importFromFile(MultipartFile file) throws IOException {
		List<StreamCategoryImportDto> categories = new ArrayList<>();

		String filename = file.getOriginalFilename();
		if (filename != null && filename.endsWith(".csv")) {
			// Parse CSV
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
				String line = reader.readLine(); // skip header
				while ((line = reader.readLine()) != null) {
					String[] parts = line.split(",");
					if (parts.length >= 3) {
						categories.add(StreamCategoryImportDto.builder()
								.name(parts[0].trim())
								.description(parts[1].trim())
								.displayOrder(Integer.parseInt(parts[2].trim()))
								.iconUrl(parts.length > 3 ? parts[3].trim() : null)
								.build());
					}
				}
			}
		} else if (filename != null && filename.endsWith(".json")) {
			// Parse JSON
			ObjectMapper mapper = new ObjectMapper();
			categories = Arrays.asList(mapper.readValue(file.getInputStream(), StreamCategoryImportDto[].class));
		} else {
			throw new IllegalArgumentException("Unsupported file format. Use CSV or JSON.");
		}

		return batchCreateCategories(categories);
	}

	@Transactional(readOnly = true)
	public byte[] exportCategoriesToCSV() {
		List<StreamCategory> categories = streamCategoryRepository.findAll(Sort.by("displayOrder").ascending());

		StringBuilder sb = new StringBuilder();
		sb.append("id,name,description,displayOrder,active,createdAt\n");

		for (StreamCategory cat : categories) {
			sb.append(String.format("%s,%s,%s,%d,%b,%s\n",
					cat.getId(),
					escapeCsv(cat.getName()),
					escapeCsv(cat.getDescription()),
					cat.getDisplayOrder(),
					cat.isActive(),
					cat.getCreatedAt()));
		}

		return sb.toString().getBytes(StandardCharsets.UTF_8);
	}

	@Transactional(readOnly = true)
	public List<StreamCategory> getActiveCategoriesSortedByDisplayOrder() {
		return streamCategoryRepository.findByActiveTrueOrderByDisplayOrderAsc();
	}

	private String escapeCsv(String value) {
		if (value == null) return "";
		if (value.contains(",") || value.contains("\"")) {
			return "\"" + value.replace("\"", "\"\"") + "\"";
		}
		return value;
	}
}
