package com.vinicius.scene_api.service;

import com.vinicius.scene_api.dto.ArtistProfileRequest;
import com.vinicius.scene_api.dto.ArtistProfileResponse;
import com.vinicius.scene_api.exception.BadRequestException;
import com.vinicius.scene_api.exception.ConflictException;
import com.vinicius.scene_api.exception.ResourceNotFoundException;
import com.vinicius.scene_api.model.Artist;
import com.vinicius.scene_api.model.ArtistProfile;
import com.vinicius.scene_api.repository.ArtistProfileRepository;
import com.vinicius.scene_api.repository.ArtistRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ArtistProfileService {

    private final ArtistProfileRepository profileRepository;
    private final ArtistRepository artistRepository;

    public ArtistProfileService(ArtistProfileRepository profileRepository, ArtistRepository artistRepository) {
        this.profileRepository = profileRepository;
        this.artistRepository = artistRepository;
    }

    public Page<ArtistProfileResponse> findAll(Pageable pageable) {
        return profileRepository.findAll(pageable).map(ArtistProfileResponse::from);
    }

    public Page<ArtistProfileResponse> searchByHometown(String hometown, Pageable pageable) {
        return profileRepository.findByHometownContainingIgnoreCase(hometown, pageable).map(ArtistProfileResponse::from);
    }

    public ArtistProfileResponse findById(Long id) {
        return ArtistProfileResponse.from(getProfile(id));
    }

    public ArtistProfileResponse findByArtist(Long artistId) {
        if (!artistRepository.existsById(artistId)) {
            throw new ResourceNotFoundException("Artista", artistId);
        }
        return profileRepository.findByArtist_Id(artistId)
                .map(ArtistProfileResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil do artista", artistId));
    }

    @Transactional
    public ArtistProfileResponse create(ArtistProfileRequest request) {
        Artist artist = getArtist(request.artistId());
        if (profileRepository.existsByArtist_Id(artist.getId())) {
            throw new ConflictException("O artista de id " + artist.getId() + " já possui um perfil");
        }
        ArtistProfile profile = new ArtistProfile(artist, request.biography(), request.hometown(),
                request.website(), request.instagram());
        return ArtistProfileResponse.from(profileRepository.saveAndFlush(profile));
    }

    @Transactional
    public ArtistProfileResponse update(Long id, ArtistProfileRequest request) {
        ArtistProfile profile = getProfile(id);
        if (!profile.getArtist().getId().equals(request.artistId())) {
            throw new BadRequestException("O artista de um perfil existente não pode ser alterado");
        }
        profile.setBiography(request.biography());
        profile.setHometown(request.hometown());
        profile.setWebsite(request.website());
        profile.setInstagram(request.instagram());
        return ArtistProfileResponse.from(profileRepository.saveAndFlush(profile));
    }

    @Transactional
    public void delete(Long id) {
        profileRepository.delete(getProfile(id));
    }

    private ArtistProfile getProfile(Long id) {
        return profileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil", id));
    }

    private Artist getArtist(Long id) {
        return artistRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Artista", id));
    }
}
