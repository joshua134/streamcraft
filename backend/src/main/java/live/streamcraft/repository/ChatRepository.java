package live.streamcraft.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import live.streamcraft.entity.Chat;

@Repository
public interface ChatRepository extends JpaRepository<Chat, String> {
	 @Query("SELECT c FROM Chat c WHERE c.stream.id = :streamId AND c.deleted = false ORDER BY c.createdAt ASC")
	 Page<Chat> findByStreamIdOrderByCreatedAtAsc(@Param("streamId") String streamId, Pageable pageable);

	 @Query("SELECT c FROM Chat c WHERE c.stream.id = :streamId AND c.deleted = false ORDER BY c.createdAt DESC")
	 Page<Chat> findByStreamIdOrderByCreatedAtDesc(@Param("streamId") String streamId, Pageable pageable);

	 @Query("SELECT c FROM Chat c WHERE c.stream.id = :streamId AND c.deleted = false")
	 List<Chat> findByStreamId(@Param("streamId") String streamId);

	 @Query("SELECT COUNT(c) FROM Chat c WHERE c.stream.id = :streamId AND c.deleted = false")
	 long countByStreamId(@Param("streamId") String streamId);

	 @Modifying
	 @Transactional
	 @Query("UPDATE Chat c SET c.deleted = true, c.deletedAt = :now WHERE c.stream.id = :streamId")
	 void softDeleteByStreamId(@Param("streamId") String streamId, @Param("now") LocalDateTime now);
}
