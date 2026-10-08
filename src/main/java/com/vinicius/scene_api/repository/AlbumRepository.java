package com.vinicius.scene_api.repository;

import com.vinicius.scene_api.model.Album;
import com.vinicius.scene_api.model.AlbumType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlbumRepository extends JpaRepository<Album, Long> {

    Page<Album> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    Page<Album> findByArtist_Id(Long artistId, Pageable pageable);

    Page<Album> findByType(AlbumType type, Pageable pageable);

    boolean existsByArtist_IdAndTitleIgnoreCase(Long artistId, String title);

    boolean existsByArtist_IdAndTitleIgnoreCaseAndIdNot(Long artistId, String title, Long id);
}
