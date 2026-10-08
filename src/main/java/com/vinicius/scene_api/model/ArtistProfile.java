package com.vinicius.scene_api.model;

import com.vinicius.scene_api.validation.ValidationRules;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.util.Objects;

/**
 * Perfil detalhado de um artista (relação 1:1). Cada artista tem no máximo um perfil.
 */
@Entity
@Table(name = "artist_profiles")
public class ArtistProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "artist_id", nullable = false, unique = true)
    private Artist artist;

    @NotBlank
    @Size(min = 10, max = 1000)
    @Column(nullable = false, length = 1000)
    private String biography;

    @Size(min = 2, max = 100)
    @Pattern(regexp = ValidationRules.LETTERS_ONLY, message = ValidationRules.LETTERS_ONLY_MESSAGE)
    @Column(length = 100)
    private String hometown;

    @Size(max = 200)
    @Pattern(regexp = ValidationRules.URL, message = ValidationRules.URL_MESSAGE)
    @Column(length = 200)
    private String website;

    @Pattern(regexp = ValidationRules.INSTAGRAM, message = ValidationRules.INSTAGRAM_MESSAGE)
    @Column(length = 30)
    private String instagram;

    protected ArtistProfile() {
    }

    public ArtistProfile(Artist artist, String biography, String hometown, String website, String instagram) {
        this.artist = artist;
        this.biography = biography;
        this.hometown = hometown;
        this.website = website;
        this.instagram = instagram;
    }

    public Long getId() {
        return id;
    }

    public Artist getArtist() {
        return artist;
    }

    public void setArtist(Artist artist) {
        this.artist = artist;
    }

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

    public String getHometown() {
        return hometown;
    }

    public void setHometown(String hometown) {
        this.hometown = hometown;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getInstagram() {
        return instagram;
    }

    public void setInstagram(String instagram) {
        this.instagram = instagram;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ArtistProfile other)) return false;
        return id != null && Objects.equals(id, other.getId());
    }

    @Override
    public int hashCode() {
        return ArtistProfile.class.hashCode();
    }

    @Override
    public String toString() {
        return "ArtistProfile{id=" + id + ", hometown='" + hometown + "'}";
    }
}
