package com.vinicius.scene_api.service;

import com.vinicius.scene_api.dto.PlaylistRequest;
import com.vinicius.scene_api.dto.PlaylistResponse;
import com.vinicius.scene_api.dto.TrackResponse;
import com.vinicius.scene_api.exception.ConflictException;
import com.vinicius.scene_api.exception.ResourceNotFoundException;
import com.vinicius.scene_api.model.Playlist;
import com.vinicius.scene_api.model.Track;
import com.vinicius.scene_api.model.Visibility;
import com.vinicius.scene_api.repository.PlaylistRepository;
import com.vinicius.scene_api.repository.TrackRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final TrackRepository trackRepository;

    public PlaylistService(PlaylistRepository playlistRepository, TrackRepository trackRepository) {
        this.playlistRepository = playlistRepository;
        this.trackRepository = trackRepository;
    }

    public Page<PlaylistResponse> findAll(Pageable pageable) {
        return playlistRepository.findAll(pageable).map(PlaylistResponse::from);
    }

    public Page<PlaylistResponse> searchByName(String name, Pageable pageable) {
        return playlistRepository.findByNameContainingIgnoreCase(name, pageable).map(PlaylistResponse::from);
    }

    public Page<PlaylistResponse> findByVisibility(Visibility visibility, Pageable pageable) {
        return playlistRepository.findByVisibility(visibility, pageable).map(PlaylistResponse::from);
    }

    /** Faixas que fazem parte da playlist. */
    public Page<TrackResponse> findTracks(Long playlistId, Pageable pageable) {
        if (!playlistRepository.existsById(playlistId)) {
            throw new ResourceNotFoundException("Playlist", playlistId);
        }
        return trackRepository.findByPlaylists_Id(playlistId, pageable).map(TrackResponse::from);
    }

    public PlaylistResponse findById(Long id) {
        return PlaylistResponse.from(getPlaylist(id));
    }

    @Transactional
    public PlaylistResponse create(PlaylistRequest request) {
        if (playlistRepository.existsByNameIgnoreCase(request.name())) {
            throw new ConflictException("Já existe uma playlist com o nome '" + request.name() + "'");
        }
        Playlist playlist = new Playlist(request.name(), request.description(), request.visibility());
        return PlaylistResponse.from(playlistRepository.saveAndFlush(playlist));
    }

    @Transactional
    public PlaylistResponse update(Long id, PlaylistRequest request) {
        Playlist playlist = getPlaylist(id);
        if (playlistRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
            throw new ConflictException("Já existe uma playlist com o nome '" + request.name() + "'");
        }
        playlist.setName(request.name());
        playlist.setDescription(request.description());
        playlist.setVisibility(request.visibility());
        return PlaylistResponse.from(playlistRepository.saveAndFlush(playlist));
    }

    @Transactional
    public void delete(Long id) {
        playlistRepository.delete(getPlaylist(id));
    }

    /** Coloca uma faixa na playlist. Falha com 409 se ela já estiver lá. */
    @Transactional
    public PlaylistResponse addTrack(Long playlistId, Long trackId) {
        Playlist playlist = getPlaylist(playlistId);
        Track track = getTrack(trackId);
        if (!playlist.addTrack(track)) {
            throw new ConflictException("A faixa " + trackId + " já está na playlist " + playlistId);
        }
        return PlaylistResponse.from(playlistRepository.saveAndFlush(playlist));
    }

    /** Tira uma faixa da playlist. Falha com 404 se ela não estiver lá. */
    @Transactional
    public void removeTrack(Long playlistId, Long trackId) {
        Playlist playlist = getPlaylist(playlistId);
        Track track = getTrack(trackId);
        if (!playlist.removeTrack(track)) {
            throw new ResourceNotFoundException("A faixa " + trackId + " não está na playlist " + playlistId);
        }
        playlistRepository.saveAndFlush(playlist);
    }

    private Playlist getPlaylist(Long id) {
        return playlistRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Playlist", id));
    }

    private Track getTrack(Long id) {
        return trackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Faixa", id));
    }
}
