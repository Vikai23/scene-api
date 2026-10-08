package com.vinicius.scene_api.service;

import com.vinicius.scene_api.dto.PlaylistResponse;
import com.vinicius.scene_api.dto.TrackRequest;
import com.vinicius.scene_api.dto.TrackResponse;
import com.vinicius.scene_api.exception.ConflictException;
import com.vinicius.scene_api.exception.ResourceNotFoundException;
import com.vinicius.scene_api.model.Album;
import com.vinicius.scene_api.model.Track;
import com.vinicius.scene_api.repository.AlbumRepository;
import com.vinicius.scene_api.repository.PlaylistRepository;
import com.vinicius.scene_api.repository.TrackRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TrackService {

    private final TrackRepository trackRepository;
    private final AlbumRepository albumRepository;
    private final PlaylistRepository playlistRepository;

    public TrackService(TrackRepository trackRepository, AlbumRepository albumRepository,
                        PlaylistRepository playlistRepository) {
        this.trackRepository = trackRepository;
        this.albumRepository = albumRepository;
        this.playlistRepository = playlistRepository;
    }

    public Page<TrackResponse> findAll(Pageable pageable) {
        return trackRepository.findAll(pageable).map(TrackResponse::from);
    }

    public Page<TrackResponse> searchByTitle(String title, Pageable pageable) {
        return trackRepository.findByTitleContainingIgnoreCase(title, pageable).map(TrackResponse::from);
    }

    public Page<TrackResponse> findByAlbum(Long albumId, Pageable pageable) {
        if (!albumRepository.existsById(albumId)) {
            throw new ResourceNotFoundException("Álbum", albumId);
        }
        return trackRepository.findByAlbum_Id(albumId, pageable).map(TrackResponse::from);
    }

    /** Playlists que contêm a faixa. */
    public Page<PlaylistResponse> findPlaylists(Long trackId, Pageable pageable) {
        if (!trackRepository.existsById(trackId)) {
            throw new ResourceNotFoundException("Faixa", trackId);
        }
        return playlistRepository.findByTracks_Id(trackId, pageable).map(PlaylistResponse::from);
    }

    public TrackResponse findById(Long id) {
        return TrackResponse.from(getTrack(id));
    }

    @Transactional
    public TrackResponse create(TrackRequest request) {
        Album album = getAlbum(request.albumId());
        if (trackRepository.existsByAlbum_IdAndTrackNumber(album.getId(), request.trackNumber())) {
            throw new ConflictException("Esse álbum já tem uma faixa de número " + request.trackNumber());
        }
        Track track = new Track(album, request.title(), request.trackNumber(), request.durationSeconds());
        return TrackResponse.from(trackRepository.saveAndFlush(track));
    }

    @Transactional
    public TrackResponse update(Long id, TrackRequest request) {
        Track track = getTrack(id);
        Album album = getAlbum(request.albumId());
        if (trackRepository.existsByAlbum_IdAndTrackNumberAndIdNot(album.getId(), request.trackNumber(), id)) {
            throw new ConflictException("Esse álbum já tem uma faixa de número " + request.trackNumber());
        }
        track.setAlbum(album);
        track.setTitle(request.title());
        track.setTrackNumber(request.trackNumber());
        track.setDurationSeconds(request.durationSeconds());
        return TrackResponse.from(trackRepository.saveAndFlush(track));
    }

    /**
     * Remove a faixa, tirando-a antes de todas as playlists em que estava.
     */
    @Transactional
    public void delete(Long id) {
        Track track = getTrack(id);
        track.detachFromPlaylists();
        trackRepository.delete(track);
    }

    private Track getTrack(Long id) {
        return trackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Faixa", id));
    }

    private Album getAlbum(Long id) {
        return albumRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Álbum", id));
    }
}
