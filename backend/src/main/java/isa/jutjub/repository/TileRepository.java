package isa.jutjub.repository;

import isa.jutjub.model.Tile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TileRepository extends JpaRepository<Tile, Long> {

    /**
     * Find tile by exact longitude and latitude coordinates
     */
    Optional<Tile> findByLongitudeAndLatitude(Integer longitude, Integer latitude);

    /**
     * Find tiles with at least one video
     */
    @Query("SELECT t FROM Tile t WHERE t.videoCount > 0")
    Page<Tile> findTilesWithVideos(Pageable pageable);

    /**
     * Count tiles with at least one video
     */
    @Query("SELECT COUNT(t) FROM Tile t WHERE t.videoCount > 0")
    Long countTilesWithVideos();

    /**
     * Check if a tile exists at the given coordinates
     */
    boolean existsByLongitudeAndLatitude(Integer longitude, Integer latitude);
}