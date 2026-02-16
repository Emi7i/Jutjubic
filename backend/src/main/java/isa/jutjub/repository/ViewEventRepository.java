package isa.jutjub.repository;

import isa.jutjub.model.ViewEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ViewEventRepository extends JpaRepository<ViewEvent, Long> {
    /**
     * Get daily view counts for all videos in the last 7 days
     */
    @Query("SELECT ve.videoId, DATE(ve.viewedAt), COUNT(ve) FROM ViewEvent ve WHERE ve.viewedAt >= :startDate GROUP BY ve.videoId, DATE(ve.viewedAt)")
    List<Object[]> getDailyViewCounts(@Param("startDate") LocalDateTime startDate);
}
