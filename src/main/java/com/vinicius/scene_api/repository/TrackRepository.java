package com.vinicius.scene_api.repository;

import com.vinicius.scene_api.model.Track;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackRepository extends JpaRepository<Track, Long> {

    Page<Track> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    Page<Track> findByAlbum_Id(Long albumId, Pageable pageable);

    Page<Track> findByPlaylists_Id(Long playlistId, Pageable pageable);

    boolean existsByAlbum_IdAndTrackNumber(Long albumId, Integer trackNumber);

    boolean existsByAlbum_IdAndTrackNumberAndIdNot(Long albumId, Integer trackNumber, Long id);
}
