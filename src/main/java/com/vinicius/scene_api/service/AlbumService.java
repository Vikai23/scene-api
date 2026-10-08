package com.vinicius.scene_api.service;

import com.vinicius.scene_api.dto.AlbumRequest;
import com.vinicius.scene_api.dto.AlbumResponse;
import com.vinicius.scene_api.exception.ConflictException;
import com.vinicius.scene_api.exception.ResourceNotFoundException;
import com.vinicius.scene_api.model.Album;
import com.vinicius.scene_api.model.AlbumType;
import com.vinicius.scene_api.model.Artist;
import com.vinicius.scene_api.model.Track;
import com.vinicius.scene_api.repository.AlbumRepository;
import com.vinicius.scene_api.repository.ArtistRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AlbumService {

    private final AlbumRepository albumRepository;
    private final ArtistRepository artistRepository;

    public AlbumService(AlbumRepository albumRepository, ArtistRepository artistRepository) {
        this.albumRepository = albumRepository;
        this.artistRepository = artistRepository;
    }

    public Page<AlbumResponse> findAll(Pageable pageable) {
        return albumRepository.findAll(pageable).map(AlbumResponse::from);
    }

    public Page<AlbumResponse> searchByTitle(String title, Pageable pageable) {
        return albumRepository.findByTitleContainingIgnoreCase(title, pageable).map(AlbumResponse::from);
    }

    public Page<AlbumResponse> findByType(AlbumType type, Pageable pageable) {
        return albumRepository.findByType(type, pageable).map(AlbumResponse::from);
    }

    public Page<AlbumResponse> findByArtist(Long artistId, Pageable pageable) {
        if (!artistRepository.existsById(artistId)) {
            throw new ResourceNotFoundException("Artista", artistId);
        }
        return albumRepository.findByArtist_Id(artistId, pageable).map(AlbumResponse::from);
    }

    public AlbumResponse findById(Long id) {
        return AlbumResponse.from(getAlbum(id));
    }

    @Transactional
    public AlbumResponse create(AlbumRequest request) {
        Artist artist = getArtist(request.artistId());
        if (albumRepository.existsByArtist_IdAndTitleIgnoreCase(artist.getId(), request.title())) {
            throw new ConflictException("Esse artista já tem um álbum chamado '" + request.title() + "'");
        }
        Album album = new Album(artist, request.title(), request.releaseYear(), request.type(), request.label());
        return AlbumResponse.from(albumRepository.saveAndFlush(album));
    }

    @Transactional
    public AlbumResponse update(Long id, AlbumRequest request) {
        Album album = getAlbum(id);
        Artist artist = getArtist(request.artistId());
        if (albumRepository.existsByArtist_IdAndTitleIgnoreCaseAndIdNot(artist.getId(), request.title(), id)) {
            throw new ConflictException("Esse artista já tem um álbum chamado '" + request.title() + "'");
        }
        album.setArtist(artist);
        album.setTitle(request.title());
        album.setReleaseYear(request.releaseYear());
        album.setType(request.type());
        album.setLabel(request.label());
        return AlbumResponse.from(albumRepository.saveAndFlush(album));
    }

    /**
     * Remove o álbum junto com suas faixas. As faixas saem antes de todas as playlists.
     */
    @Transactional
    public void delete(Long id) {
        Album album = getAlbum(id);
        album.getTracks().forEach(Track::detachFromPlaylists);
        albumRepository.delete(album);
    }

    private Album getAlbum(Long id) {
        return albumRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Álbum", id));
    }

    private Artist getArtist(Long id) {
        return artistRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Artista", id));
    }
}
