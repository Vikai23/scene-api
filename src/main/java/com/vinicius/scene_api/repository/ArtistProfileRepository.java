package com.vinicius.scene_api.repository;

import com.vinicius.scene_api.model.ArtistProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArtistProfileRepository extends JpaRepository<ArtistProfile, Long> {

    Optional<ArtistProfile> findByArtist_Id(Long artistId);

    Page<ArtistProfile> findByHometownContainingIgnoreCase(String hometown, Pageable pageable);

    boolean existsByArtist_Id(Long artistId);
}
