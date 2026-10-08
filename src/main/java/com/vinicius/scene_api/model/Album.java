package com.vinicius.scene_api.model;

import com.vinicius.scene_api.validation.ValidationRules;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Álbum, EP, single, coletânea ou show ao vivo de um artista. Tem várias faixas (1:N).
 */
@Entity
@Table(name = "albums",
        uniqueConstraints = @UniqueConstraint(name = "uk_album_artist_title", columnNames = {"artist_id", "title"}))
public class Album {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 1, max = 150)
    @Pattern(regexp = ValidationRules.TITLE, message = ValidationRules.TITLE_MESSAGE)
    @Column(nullable = false, length = 150)
    private String title;

    @NotNull
    @Min(1900)
    @Max(2100)
    @Column(nullable = false)
    private Integer releaseYear;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "album_type", nullable = false, length = 20)
    private AlbumType type;

    @Size(min = 2, max = 100)
    @Pattern(regexp = ValidationRules.NAME, message = ValidationRules.NAME_MESSAGE)
    @Column(name = "record_label", length = 100)
    private String label;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "artist_id", nullable = false)
    private Artist artist;

    @OneToMany(mappedBy = "album", cascade = CascadeType.REMOVE)
    private Set<Track> tracks = new LinkedHashSet<>();

    protected Album() {
    }

    public Album(Artist artist, String title, Integer releaseYear, AlbumType type, String label) {
        this.artist = artist;
        this.title = title;
        this.releaseYear = releaseYear;
        this.type = type;
        this.label = label;
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

    public Integer getReleaseYear() {
        return releaseYear;
    }

    public void setReleaseYear(Integer releaseYear) {
        this.releaseYear = releaseYear;
    }

    public AlbumType getType() {
        return type;
    }

    public void setType(AlbumType type) {
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Artist getArtist() {
        return artist;
    }

    public void setArtist(Artist artist) {
        this.artist = artist;
    }

    public Set<Track> getTracks() {
        return tracks;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Album other)) return false;
        return id != null && Objects.equals(id, other.getId());
    }

    @Override
    public int hashCode() {
        return Album.class.hashCode();
    }

    @Override
    public String toString() {
        return "Album{id=" + id + ", title='" + title + "', releaseYear=" + releaseYear + ", type=" + type + '}';
    }
}
