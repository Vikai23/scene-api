package com.vinicius.scene_api.repository;

import com.vinicius.scene_api.model.Playlist;
import com.vinicius.scene_api.model.Visibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {

    Page<Playlist> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Playlist> findByVisibility(Visibility visibility, Pageable pageable);

    Page<Playlist> findByTracks_Id(Long trackId, Pageable pageable);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
