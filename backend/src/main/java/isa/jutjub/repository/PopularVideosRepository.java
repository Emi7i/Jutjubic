package isa.jutjub.repository;

import isa.jutjub.model.PopularVideos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PopularVideosRepository extends JpaRepository<PopularVideos, Long> {

    // Get the latest popular videos run
    PopularVideos findTopByOrderByRunTimeDesc();
}
