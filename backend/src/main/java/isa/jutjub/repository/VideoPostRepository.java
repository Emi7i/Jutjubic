package isa.jutjub.repository;

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

@Repository
public interface VideoPostRepository extends JpaRepository<Videos, Long> {

    /**
     * Find video posts by tags
     * @param tags list of tags to search for
     * @return list of video posts containing any of the specified tags
     */
    @Query(value = "SELECT DISTINCT vp.* FROM videos vp WHERE vp.tags && :tags", nativeQuery = true)
    List<Videos> findByTags(@Param("tags") String[] tags);

    /**
     * Find video posts by title containing keyword
     * @param title keyword to search in title
     * @param pageable pagination information
     * @return page of video posts with title containing keyword
     */
    Page<Videos> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    /**
     * Find video posts by location
     * @param location location to search for
     * @param pageable pagination information
     * @return page of video posts from specified location
     */
    Page<Videos> findByLocationContainingIgnoreCase(String location, Pageable pageable);

    /**
     * Find video posts created after specified date
     * @param date date to search from
     * @param pageable pagination information
     * @return page of video posts created after specified date
     */
    Page<Videos> findByCreatedAtAfter(LocalDateTime date, Pageable pageable);

    /**
     * Find video posts created before specified date
     * @param date date to search until
     * @param pageable pagination information
     * @return page of video posts created before specified date
     */
    Page<Videos> findByCreatedAtBefore(LocalDateTime date, Pageable pageable);

    /**
     * Find video posts created between two dates
     * @param startDate start date
     * @param endDate end date
     * @param pageable pagination information
     * @return page of video posts created within date range
     */
    @Query("SELECT vp FROM Videos vp WHERE vp.createdAt BETWEEN :startDate AND :endDate")
    Page<Videos> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate,
                                        Pageable pageable);


    /**
     * Find most recent video posts ordered by creation date
     * @param pageable pagination information
     * @return page of most recent video posts
     */
    @Query("SELECT vp FROM Videos vp ORDER BY vp.createdAt DESC")
    Page<Videos> findMostRecent(Pageable pageable);

    /**
     * Find video posts by a specific tag
     * @param tag tag to search for
     * @param pageable pagination information
     * @return page of video posts with specified tag
     */
    @Query(value = "SELECT * FROM videos vp WHERE :tag = ANY(vp.tags)", nativeQuery = true)
    Page<Videos> findByTag(@Param("tag") String tag, Pageable pageable);

    /**
     * Search video posts by multiple criteria (title, description, tags, location)
     * @param keyword search keyword
     * @param pageable pagination information
     * @return page of video posts matching search criteria
     */
    @Query(value = "SELECT * FROM videos vp WHERE " +
           "LOWER(vp.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(vp.video_description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(vp.location) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           ":keyword = ANY(vp.tags)", 
           nativeQuery = true)
    Page<Videos> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Count video posts by user (assuming we add user relationship later)
     * @param userId user ID
     * @return count of video posts by user
     */
    // This will be implemented when we add user relationship
    // Long countByUserId(Long userId);

    /**
     * Find video posts with video file size within range
     * @param minSize minimum file size
     * @param maxSize maximum file size
     * @param pageable pagination information
     * @return page of video posts within file size range
     */
    Page<Videos> findByVideoFileSizeBetween(Long minSize, Long maxSize, Pageable pageable);


    /**
     * Atomically increments the view count for a video post
     * This method is thread-safe and handles concurrent access correctly
     * @param id the video post ID
     * @return number of rows updated (should be 1 if successful)
     */
    @Modifying
    @Transactional
    @Query("UPDATE Videos vp SET vp.viewsCount = vp.viewsCount + 1 WHERE vp.id = :id")
    int incrementViewsCount(@Param("id") Long id);

}
