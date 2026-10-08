package com.vinicius.scene_api.model;

import com.vinicius.scene_api.validation.ValidationRules;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Faixa de um álbum. Pode estar em várias playlists (N:N, lado inverso: quem manda é {@link Playlist}).
 */
@Entity
@Table(name = "tracks",
        uniqueConstraints = @UniqueConstraint(name = "uk_track_album_number", columnNames = {"album_id", "track_number"}))
public class Track {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 1, max = 150)
    @Pattern(regexp = ValidationRules.TITLE, message = ValidationRules.TITLE_MESSAGE)
    @Column(nullable = false, length = 150)
    private String title;

    @NotNull
    @Min(1)
    @Max(99)
    @Column(name = "track_number", nullable = false)
    private Integer trackNumber;

    @Min(1)
    @Max(7200)
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "album_id", nullable = false)
    private Album album;

    @ManyToMany(mappedBy = "tracks")
    private Set<Playlist> playlists = new LinkedHashSet<>();

    protected Track() {
    }

    public Track(Album album, String title, Integer trackNumber, Integer durationSeconds) {
        this.album = album;
        this.title = title;
        this.trackNumber = trackNumber;
        this.durationSeconds = durationSeconds;
    }

    /**
     * Tira a faixa de todas as playlists. Deve ser chamado antes de excluir a faixa,
     * senão o banco recusa a exclusão por causa da tabela de ligação.
     */
    public void detachFromPlaylists() {
        for (Playlist playlist : new ArrayList<>(playlists)) {
            playlist.removeTrack(this);
        }
        playlists.clear();
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getTrackNumber() {
        return trackNumber;
    }

    public void setTrackNumber(Integer trackNumber) {
        this.trackNumber = trackNumber;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Album getAlbum() {
        return album;
    }

    public void setAlbum(Album album) {
        this.album = album;
    }

    public Set<Playlist> getPlaylists() {
        return playlists;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Track other)) return false;
        return id != null && Objects.equals(id, other.getId());
    }

    @Override
    public int hashCode() {
        return Track.class.hashCode();
    }

    @Override
    public String toString() {
        return "Track{id=" + id + ", title='" + title + "', trackNumber=" + trackNumber + '}';
    }
}
