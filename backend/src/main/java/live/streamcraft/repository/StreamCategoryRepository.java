package live.streamcraft.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import live.streamcraft.entity.StreamCategory;

@Repository
public interface StreamCategoryRepository extends JpaRepository<StreamCategory, String> {
	Optional<StreamCategory> findByNameIgnoreCase(String name);
    List<StreamCategory> findByActiveTrueOrderByDisplayOrderAsc();
    
    @Query("SELECT c FROM StreamCategory c WHERE c.active = false AND c.deleted = false ORDER BY c.displayOrder ASC")
    List<StreamCategory> findByActiveFalseOrderByDisplayOrderAsc();
    
    boolean existsByNameIgnoreCase(String name);
	
}
