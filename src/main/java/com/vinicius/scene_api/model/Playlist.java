package com.vinicius.scene_api.model;

import com.vinicius.scene_api.validation.ValidationRules;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Playlist com várias faixas (N:N). É o lado dono da relação, dono da tabela {@code playlist_tracks}.
 */
@Entity
@Table(name = "playlists")
public class Playlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 2, max = 100)
    @Pattern(regexp = ValidationRules.NAME, message = ValidationRules.NAME_MESSAGE)
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Size(max = 255)
    @Column(length = 255)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Visibility visibility;

    @ManyToMany
    @JoinTable(
            name = "playlist_tracks",
            joinColumns = @JoinColumn(name = "playlist_id"),
            inverseJoinColumns = @JoinColumn(name = "track_id"))
    private Set<Track> tracks = new LinkedHashSet<>();

    protected Playlist() {
    }

    public Playlist(String name, String description, Visibility visibility) {
        this.name = name;
        this.description = description;
        this.visibility = visibility;
    }

    /** @return {@code true} se a faixa foi adicionada, {@code false} se já estava na playlist. */
    public boolean addTrack(Track track) {
        return tracks.add(track);
    }

    /** @return {@code true} se a faixa foi removida, {@code false} se não estava na playlist. */
    public boolean removeTrack(Track track) {
        return tracks.remove(track);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public Set<Track> getTracks() {
        return tracks;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Playlist other)) return false;
        return id != null && Objects.equals(id, other.getId());
    }

    @Override
    public int hashCode() {
        return Playlist.class.hashCode();
    }

    @Override
    public String toString() {
        return "Playlist{id=" + id + ", name='" + name + "', visibility=" + visibility + '}';
    }
}
