package live.streamcraft.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import live.streamcraft.entity.Stream;

@Repository
public interface StreamRepository extends JpaRepository<Stream, String> {
	@Query("SELECT s FROM Stream s LEFT JOIN FETCH s.streamer LEFT JOIN FETCH s.category WHERE s.id = :id")
    Optional<Stream> findByIdWithDetails(@Param("id") String id);
    
    @Query("SELECT s FROM Stream s WHERE s.isLive = true AND s.status = 'LIVE' ORDER BY s.viewerCount DESC")
    Page<Stream> findLiveStreams(Pageable pageable);
    
    @Modifying
    @Transactional
    @Query("UPDATE Stream s SET s.viewerCount = s.viewerCount + 1 WHERE s.id = :streamId")
    void incrementViewerCount(@Param("streamId") String streamId);
    
    @Modifying
    @Transactional
    @Query("UPDATE Stream s SET s.viewerCount = s.viewerCount - 1 WHERE s.id = :streamId AND s.viewerCount > 0")
    void decrementViewerCount(@Param("streamId") String streamId);
    
    @Query("SELECT s FROM Stream s WHERE s.actualStartTime < :timeout AND s.isLive = true AND s.status = 'LIVE'")
    List<Stream> findPotentiallyDisconnectedStreams(@Param("timeout") LocalDateTime timeout);
    
    @Modifying
    @Transactional
    @Query("UPDATE Stream s SET s.status = 'ENDED', s.isLive = false, s.actualEndTime = :endTime WHERE s.id = :streamId")
    void endStream(@Param("streamId") String streamId, @Param("endTime") LocalDateTime endTime);
    
    @Query("SELECT s FROM Stream s WHERE s.status = 'SCHEDULED' AND s.scheduledStartTime BETWEEN :start AND :end AND s.deleted = false")
    List<Stream> findScheduledStreamsInRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    @Query("SELECT s FROM Stream s WHERE s.status = 'SCHEDULED' AND s.scheduledStartTime <= :now AND s.isLive = false AND s.deleted = false")
    List<Stream> findStreamsReadyToStart(@Param("now") LocalDateTime now);
    
    @Query("SELECT s FROM Stream s WHERE s.isLive = true AND s.status = 'LIVE' AND s.deleted = false ORDER BY s.viewerCount DESC")
    Page<Stream> findLiveStreamsOrderByViewerCount(Pageable pageable);
    
    @Query("SELECT s FROM Stream s WHERE s.isLive = true AND s.status = 'LIVE' AND s.deleted = false ORDER BY s.viewerCount DESC")
    Optional<Stream> findFeaturedStream();
    
    @Query("SELECT s FROM Stream s WHERE s.isLive = true AND s.status = 'LIVE' AND s.deleted = false ORDER BY s.viewerCount DESC")
    List<Stream> findTopFeaturedStreams(Pageable pageable);
    
    @Modifying
    @Transactional
    @Query("UPDATE Stream s SET s.isFeatured = false WHERE s.isFeatured = true")
    void resetFeaturedStatus();
    
    @Modifying
    @Transactional
    @Query("UPDATE Stream s SET s.isFeatured = true WHERE s.id IN " +
           "(SELECT id FROM Stream WHERE isLive = true AND status = 'LIVE' AND deleted = false " +
           "ORDER BY viewerCount DESC LIMIT 1)")
    void setTopStreamAsFeatured();
    
    @Query("SELECT s FROM Stream s WHERE s.status = 'SCHEDULED' AND s.scheduledStartTime BETWEEN :now AND :soon AND s.deleted = false")
    List<Stream> findStreamsStartingSoon(@Param("now") LocalDateTime now, @Param("soon") LocalDateTime soon);
    
    // Cancel scheduled stream
    @Modifying
    @Transactional
    @Query("UPDATE Stream s SET s.status = 'CANCELLED', s.isLive = false WHERE s.id = :streamId AND s.status = 'SCHEDULED'")
    int cancelScheduledStream(@Param("streamId") String streamId);
    
    @Modifying
    @Transactional
    @Query("UPDATE Stream s SET s.deleted = true, s.deletedAt = :now WHERE s.status = 'ENDED' AND s.actualEndTime < :cutoffDate " +
           "AND s.deleted = false")
    int softDeleteOldStreams(@Param("cutoffDate") LocalDateTime cutoffDate,@Param("now") LocalDateTime now);
    
    @Query("SELECT s.streamer.email, s.streamer.uname, s.title, s.scheduledStartTime " +
    	       "FROM Stream s WHERE s.status = 'SCHEDULED' AND s.scheduledStartTime > :now " +
    	       "AND s.scheduledStartTime <= :notifyTime AND s.deleted = false")
    List<Object[]> findStreamsToNotify(@Param("now") LocalDateTime now, @Param("notifyTime") LocalDateTime notifyTime);

    @Query("SELECT s FROM Stream s WHERE s.streamKey = :streamKey")
    Optional<Stream> findByStreamKey(@Param("streamKey") String streamKey);

    @Query("SELECT s FROM Stream s WHERE s.streamer.id = :userId AND s.deleted = false")
	Page<Stream> findByStreamerId(@Param("userId") String userId, Pageable pageable);
    
    @Query("""
    		SELECT s FROM Stream s WHERE s.deleted = false AND s.status = 'SCHEDULED' 
    		AND s.scheduledStartTime IS NOT NULL AND s.scheduledStartTime > :now
    	""")
    Page<Stream> findUpcomingStreams(@Param("now") LocalDateTime now, Pageable pageable);
    
    @Query("""
    	    SELECT s FROM Stream s WHERE s.deleted = false AND s.category.id = :categoryId
    		AND (s.status = 'LIVE' OR s.status = 'SCHEDULED')
    		ORDER BY  CASE WHEN s.status = 'LIVE' THEN 0 ELSE 1 END, s.scheduledStartTime ASC
    		""")
    Page<Stream> findActiveStreamsByCategory(@Param("categoryId") String categoryId,Pageable pageable);
    
    @Query("SELECT s FROM Stream s WHERE s.isLive = true AND s.status = 'LIVE' AND s.deleted = false")
    List<Stream> findLiveStreamsForTrending();
    
    @Query("""
    	    SELECT s FROM Stream s WHERE s.streamer.id = :streamerId  AND s.deleted = false
    	    AND s.status IN ('LIVE', 'SCHEDULED') ORDER BY s.createdAt DESC
    	""")
    List<Stream> findActiveStreamsByStreamer(@Param("streamerId") String streamerId);
    
    @Query("""
    	    SELECT s FROM Stream s WHERE s.streamer.id = :streamerId AND s.deleted = false
    	    AND s.status = 'ENDED' ORDER BY s.actualEndTime DESC
    	""")
    Page<Stream> findEndedStreamsByStreamer(@Param("streamerId") String streamerId,Pageable pageable);
    
}
