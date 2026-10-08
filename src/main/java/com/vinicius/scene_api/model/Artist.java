package com.vinicius.scene_api.model;

import com.vinicius.scene_api.validation.ValidationRules;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Artista ou banda. Tem vários álbuns (1:N) e, opcionalmente, um perfil (1:1, lado dono em {@link ArtistProfile}).
 */
@Entity
@Table(name = "artists")
public class Artist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 2, max = 100)
    @Pattern(regexp = ValidationRules.NAME, message = ValidationRules.NAME_MESSAGE)
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Genre genre;

    @Size(min = 2, max = 60)
    @Pattern(regexp = ValidationRules.LETTERS_ONLY, message = ValidationRules.LETTERS_ONLY_MESSAGE)
    @Column(length = 60)
    private String country;

    @Min(1900)
    @Max(2100)
    private Integer formedYear;

    @OneToMany(mappedBy = "artist", cascade = CascadeType.REMOVE)
    private Set<Album> albums = new LinkedHashSet<>();

    protected Artist() {
    }

    public Artist(String name, Genre genre, String country, Integer formedYear) {
        this.name = name;
        this.genre = genre;
        this.country = country;
        this.formedYear = formedYear;
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

    public Genre getGenre() {
        return genre;
    }

    public void setGenre(Genre genre) {
        this.genre = genre;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Integer getFormedYear() {
        return formedYear;
    }

    public void setFormedYear(Integer formedYear) {
        this.formedYear = formedYear;
    }

    public Set<Album> getAlbums() {
        return albums;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Artist other)) return false;
        return id != null && Objects.equals(id, other.getId());
    }

    @Override
    public int hashCode() {
        return Artist.class.hashCode();
    }

    @Override
    public String toString() {
        return "Artist{id=" + id + ", name='" + name + "', genre=" + genre + '}';
    }
}
