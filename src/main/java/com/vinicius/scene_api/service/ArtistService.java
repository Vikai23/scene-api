package com.vinicius.scene_api.service;

import com.vinicius.scene_api.dto.ArtistRequest;
import com.vinicius.scene_api.dto.ArtistResponse;
import com.vinicius.scene_api.exception.ConflictException;
import com.vinicius.scene_api.exception.ResourceNotFoundException;
import com.vinicius.scene_api.model.Artist;
import com.vinicius.scene_api.model.Genre;
import com.vinicius.scene_api.model.Track;
import com.vinicius.scene_api.repository.ArtistProfileRepository;
import com.vinicius.scene_api.repository.ArtistRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ArtistService {

    private final ArtistRepository artistRepository;
    private final ArtistProfileRepository profileRepository;

    public ArtistService(ArtistRepository artistRepository, ArtistProfileRepository profileRepository) {
        this.artistRepository = artistRepository;
        this.profileRepository = profileRepository;
    }

    public Page<ArtistResponse> findAll(Pageable pageable) {
        return artistRepository.findAll(pageable).map(ArtistResponse::from);
    }

    public Page<ArtistResponse> searchByName(String name, Pageable pageable) {
        return artistRepository.findByNameContainingIgnoreCase(name, pageable).map(ArtistResponse::from);
    }

    public Page<ArtistResponse> findByGenre(Genre genre, Pageable pageable) {
        return artistRepository.findByGenre(genre, pageable).map(ArtistResponse::from);
    }

    public ArtistResponse findById(Long id) {
        return ArtistResponse.from(getArtist(id));
    }

    @Transactional
    public ArtistResponse create(ArtistRequest request) {
        if (artistRepository.existsByNameIgnoreCase(request.name())) {
            throw new ConflictException("Já existe um artista com o nome '" + request.name() + "'");
        }
        Artist artist = new Artist(request.name(), request.genre(), request.country(), request.formedYear());
        return ArtistResponse.from(artistRepository.saveAndFlush(artist));
    }

    @Transactional
    public ArtistResponse update(Long id, ArtistRequest request) {
        Artist artist = getArtist(id);
        if (artistRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
            throw new ConflictException("Já existe um artista com o nome '" + request.name() + "'");
        }
        artist.setName(request.name());
        artist.setGenre(request.genre());
        artist.setCountry(request.country());
        artist.setFormedYear(request.formedYear());
        return ArtistResponse.from(artistRepository.saveAndFlush(artist));
    }

    /**
     * Remove o artista junto com seu perfil, álbuns e faixas. As faixas saem antes de todas as playlists.
     */
    @Transactional
    public void delete(Long id) {
        Artist artist = getArtist(id);
        artist.getAlbums().forEach(album -> album.getTracks().forEach(Track::detachFromPlaylists));
        profileRepository.findByArtist_Id(id).ifPresent(profileRepository::delete);
        artistRepository.delete(artist);
    }

    private Artist getArtist(Long id) {
        return artistRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Artista", id));
    }
}
