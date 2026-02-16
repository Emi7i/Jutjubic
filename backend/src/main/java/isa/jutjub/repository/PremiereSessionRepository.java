package isa.jutjub.repository;

import isa.jutjub.model.PremiereSession;
import isa.jutjub.model.PremiereStatus;
import isa.jutjub.model.Videos;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PremiereSessionRepository extends JpaRepository<PremiereSession, Long> {

    /**
     * Find all premiere sessions for a specific video
     */
    List<PremiereSession> findByVideo(Videos video);

    /**
     * Find premiere session by video ID
     */
    List<PremiereSession> findByVideo_Id(Long videoId);

    /**
     * Find premiere sessions by status
     */
    Page<PremiereSession> findByStatus(PremiereStatus status, Pageable pageable);

    /**
     * Find scheduled premieres that should start
     */
    List<PremiereSession> findByStatusAndScheduledStartTimeBefore(
            PremiereStatus status,
            LocalDateTime now
    );

    /**
     * Find active live premieres
     */
    @Query("SELECT p FROM PremiereSession p WHERE p.status = isa.jutjub.model.PremiereStatus.LIVE")
    List<PremiereSession> findAllLivePremieres();

    /**
     * Find upcoming premieres ordered by scheduled time
     */
    @Query("SELECT p FROM PremiereSession p WHERE p.status = isa.jutjub.model.PremiereStatus.SCHEDULED ORDER BY p.scheduledStartTime ASC")
    Page<PremiereSession> findUpcomingPremieres(Pageable pageable);

    // Instead of @Query with 'LIVE'
    List<PremiereSession> findByStatus(PremiereStatus status);

    // Instead of @Query with 'SCHEDULED' and ORDER BY
    Page<PremiereSession> findByStatusOrderByScheduledStartTimeAsc(PremiereStatus status, Pageable pageable);

    /**
     * Find premieres that have ended before a given time
     */
    List<PremiereSession> findByStatusAndEndedAtBefore(
            PremiereStatus status,
            LocalDateTime time
    );

    /**
     * Find premiere by video and status (useful to prevent duplicate active premieres)
     */
    Optional<PremiereSession> findByVideo_IdAndStatus(Long videoId, PremiereStatus status);

    /**
     * Atomically update playback state
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE PremiereSession p 
        SET p.currentPositionSeconds = :position,
            p.playing = :playing,
            p.lastStateChangeEpoch = :epoch
        WHERE p.id = :id
    """)
    int updatePlaybackState(@Param("id") Long id,
                            @Param("position") Double position,
                            @Param("playing") boolean playing,
                            @Param("epoch") Long epoch);

    /**
     * Atomically increment viewer count
     */
    @Modifying
    @Transactional
    @Query("UPDATE PremiereSession p SET p.viewerCount = p.viewerCount + 1 WHERE p.id = :id")
    int incrementViewerCount(@Param("id") Long id);

    /**
     * Atomically decrement viewer count
     */
    @Modifying
    @Transactional
    @Query("UPDATE PremiereSession p SET p.viewerCount = p.viewerCount - 1 WHERE p.id = :id AND p.viewerCount > 0")
    int decrementViewerCount(@Param("id") Long id);

    /**
     * Count active live premieres
     */
    Long countByStatus(PremiereStatus status);

    /**
     * Search premieres by video title
     */
    @Query("""
        SELECT p FROM PremiereSession p
        WHERE LOWER(p.video.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
    """)
    Page<PremiereSession> searchByVideoTitle(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Find premieres created within a date range
     */
    @Query("""
        SELECT p FROM PremiereSession p
        WHERE p.createdAt BETWEEN :startDate AND :endDate
    """)
    Page<PremiereSession> findByCreatedAtBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
}
