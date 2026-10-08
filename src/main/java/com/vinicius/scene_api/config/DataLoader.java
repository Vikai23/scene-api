package com.vinicius.scene_api.config;

import com.vinicius.scene_api.model.Album;
import com.vinicius.scene_api.model.AlbumType;
import com.vinicius.scene_api.model.Artist;
import com.vinicius.scene_api.model.ArtistProfile;
import com.vinicius.scene_api.model.Genre;
import com.vinicius.scene_api.model.Playlist;
import com.vinicius.scene_api.model.Track;
import com.vinicius.scene_api.model.Visibility;
import com.vinicius.scene_api.repository.AlbumRepository;
import com.vinicius.scene_api.repository.ArtistProfileRepository;
import com.vinicius.scene_api.repository.ArtistRepository;
import com.vinicius.scene_api.repository.PlaylistRepository;
import com.vinicius.scene_api.repository.TrackRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Popula o banco H2 (em memória) ao subir a aplicação, com artistas e álbuns de uma biblioteca real do Last.fm.
 * As faixas são passadas como "título|m:ss"; sem duração (ainda não divulgada) basta deixar depois da barra vazio.
 */
@Component
public class DataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    private final ArtistRepository artistRepository;
    private final ArtistProfileRepository profileRepository;
    private final AlbumRepository albumRepository;
    private final TrackRepository trackRepository;
    private final PlaylistRepository playlistRepository;

    public DataLoader(ArtistRepository artistRepository,
                      ArtistProfileRepository profileRepository,
                      AlbumRepository albumRepository,
                      TrackRepository trackRepository,
                      PlaylistRepository playlistRepository) {
        this.artistRepository = artistRepository;
        this.profileRepository = profileRepository;
        this.albumRepository = albumRepository;
        this.trackRepository = trackRepository;
        this.playlistRepository = playlistRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (artistRepository.count() > 0) {
            return;
        }

        // ---------- Artistas e perfis (1:1) ----------
        Artist bmth = artist("Bring Me the Horizon", Genre.METALCORE, "United Kingdom", 2004);
        profile(bmth,
                "Banda britânica de Sheffield formada em 2004. Começou no metalcore e foi migrando para um som mais "
                        + "alternativo e eletrônico ao longo dos anos. Sempiternal (2013) é o álbum usado aqui como exemplo.",
                "Sheffield, England", "https://www.bmthofficial.com", "bmthofficial");

        Artist titleFight = artist("Title Fight", Genre.PUNK, "United States", 2003);
        profile(titleFight,
                "Banda de Kingston, Pensilvânia, formada em 2003. Está em hiato por tempo indeterminado desde janeiro de 2018. "
                        + "Shed (2011) saiu pela SideOneDummy Records.",
                "Kingston, Pennsylvania", null, null);

        Artist askingAlexandria = artist("Asking Alexandria", Genre.METALCORE, "United Kingdom", 2008);
        profile(askingAlexandria,
                "Banda britânica de York formada em 2008. Stand Up and Scream (2009) é o álbum de estreia do grupo.",
                "York, England", null, null);

        Artist blessthefall = artist("blessthefall", Genre.METALCORE, "United States", 2003);
        profile(blessthefall,
                "Banda de Phoenix, Arizona, formada em 2003. His Last Walk (2007) é o álbum de estreia, seguido por Witness (2009).",
                "Phoenix, Arizona", null, null);

        Artist alesana = artist("Alesana", Genre.POST_HARDCORE, "United States", 2004);
        profile(alesana,
                "Banda de Raleigh, Carolina do Norte, formada em 2004. On Frail Wings of Vanity and Wax (2006) traz a faixa Apology.",
                "Raleigh, North Carolina", null, null);

        // I Promised The World fica sem perfil de propósito, para dar para testar o POST /artist-profiles e o 404 por artista.
        Artist iPromisedTheWorld = artist("I Promised The World", Genre.POST_HARDCORE, "United States", 2020);

        // ---------- Álbuns (1:N) e faixas (1:N) ----------
        album(bmth, "Sempiternal", 2013, AlbumType.ALBUM, null,
                "Can You Feel My Heart|3:48", "The House of Wolves|3:25", "Empire (Let Them Sing)|3:45",
                "Sleepwalking|3:50", "Go to Hell, for Heaven's Sake|4:02", "Shadow Moses|4:03",
                "And the Snakes Start to Sing|5:01", "Seen It All Before|4:07", "Antivist|3:13",
                "Crooked Young|3:34", "Hospital for Souls|6:44");

        album(titleFight, "Shed", 2011, AlbumType.ALBUM, "SideOneDummy Records",
                "Coxton Yard|1:29", "Shed|2:16", "Flood Of '72|1:56", "Society|1:23",
                "You Can't Say Kingston Doesn't Love You|1:40", "Crescent-Shaped Depression|2:39",
                "Safe In Your Skin|2:11", "Where Am I?|3:06", "Your Screen Door|2:08", "27|2:27",
                "Stab|2:00", "GMT (Greenwich Mean Time)|3:37");

        album(askingAlexandria, "Stand Up and Scream", 2009, AlbumType.ALBUM, null,
                "Alerion|2:15", "Final Episode (Let's Change the Channel)|4:02", "A Candlelit Dinner With Inamorta|4:04",
                "Nobody Don't Dance No More|4:00", "Hey There Mr. Brooks|4:09", "Hiatus|1:45",
                "If You Can't Ride Two Horses At Once...You Should Get Out Of The Circus|3:46",
                "A Single Moment of Sincerity|3:50", "Not the American Average|4:39",
                "I Used to Have a Best Friend (But Then He Gave Me an STD)|4:06", "A Prophecy|3:33",
                "I Was Once, Possibly, Maybe, Perhaps a Cowboy King|3:41", "When Everyday's the Weekend|4:23");

        album(blessthefall, "His Last Walk", 2007, AlbumType.ALBUM, null,
                "A Message to the Unknown|3:32", "Guys Like You Make Us Look Bad|3:55", "Higinia|2:46",
                "Could Tell a Love|2:50", "Rise Up|3:42", "Times Like These|4:07", "Pray|4:18",
                "With Eyes Wide Shut|2:54", "Wait for Tomorrow|3:25", "Black Rose Dying|4:03", "His Last Walk|10:31");

        album(blessthefall, "Witness", 2009, AlbumType.ALBUM, null,
                "2.0|1:02", "What's Left of Me|3:33", "To Hell and Back|3:21", "God Wears Gucci|4:09",
                "Hey Baby, Here's That Song You Wanted|3:14", "Witness|3:13", "Last Ones Left|2:54",
                "Five Ninety|3:52", "We'll Sleep When We're Dead|4:14", "Skinwalkers|4:01",
                "You Deserve Nothing and I Hope You Get Less|3:39", "Stay Still|4:12");

        album(alesana, "On Frail Wings of Vanity and Wax", 2006, AlbumType.ALBUM, null,
                "Icarus|1:00", "Ambrosia|3:05", "Pathetic, Ordinary|4:00", "Alchemy Sounded Good At the Time|4:13",
                "Daggers Speak Louder Than Words|3:27", "The Last Three Letters|3:32", "Apology|3:59",
                "Tilting The Hourglass|3:48", "This Conversation is Over|3:23", "Congratulations, I Hate You|4:02",
                "The Third Temptation of Paris|3:42", "A Siren's Soliloquy|4:00", "Nero's Decay|4:26",
                "Early Mourning|3:46", "Apology (acoustic)|3:59");

        album(iPromisedTheWorld, "Fear of the Fall", 2025, AlbumType.ALBUM, null,
                "01|", "On a Broken Edge|", "Tears on the Moon|", "02|", "Weather to Believe|",
                "Don't Just Stand There|", "I Hold On Twice|", "At Least, Be Human|");

        // EP sem faixas cadastradas ainda: serve para testar a consulta por tipo de álbum.
        album(iPromisedTheWorld, "I Promised the World", 2026, AlbumType.EP, null);

        // ---------- Playlists (N:N com as faixas) ----------
        Map<String, Track> catalog = new HashMap<>();
        for (Track track : trackRepository.findAll()) {
            catalog.putIfAbsent(track.getTitle(), track);
        }

        playlist(catalog, "Heavy Rotation", "As faixas que mais rodaram no Last.fm.", Visibility.PUBLIC,
                "Apology", "Shadow Moses", "Sleepwalking", "Final Episode (Let's Change the Channel)",
                "A Message to the Unknown", "Coxton Yard");

        playlist(catalog, "Treino Pesado", "Para levantar peso sem pensar em nada.", Visibility.PRIVATE,
                "Go to Hell, for Heaven's Sake", "Not the American Average", "God Wears Gucci",
                "Skinwalkers", "Antivist", "Hey There Mr. Brooks");

        playlist(catalog, "Curtas e Grossas", "Faixas de menos de dois minutos e meio.", Visibility.PUBLIC,
                "Icarus", "2.0", "Society", "Coxton Yard", "Hiatus", "Flood Of '72",
                "You Can't Say Kingston Doesn't Love You", "Stab");

        playlist(catalog, "Madrugada", "Para ouvir de fone, com a casa em silêncio.", Visibility.PRIVATE,
                "Apology", "Tilting The Hourglass", "Early Mourning", "Hospital for Souls",
                "Tears on the Moon", "The Last Three Letters");

        log.info("Carga inicial concluída: {} artistas, {} álbuns, {} faixas e {} playlists",
                artistRepository.count(), albumRepository.count(), trackRepository.count(), playlistRepository.count());
    }

    private Artist artist(String name, Genre genre, String country, int formedYear) {
        return artistRepository.save(new Artist(name, genre, country, formedYear));
    }

    private void profile(Artist artist, String biography, String hometown, String website, String instagram) {
        profileRepository.save(new ArtistProfile(artist, biography, hometown, website, instagram));
    }

    private void album(Artist artist, String title, int releaseYear, AlbumType type, String label, String... tracks) {
        Album album = albumRepository.save(new Album(artist, title, releaseYear, type, label));
        int number = 1;
        for (String entry : tracks) {
            int separator = entry.lastIndexOf('|');
            String trackTitle = separator < 0 ? entry : entry.substring(0, separator);
            Integer seconds = separator < 0 ? null : toSeconds(entry.substring(separator + 1));
            trackRepository.save(new Track(album, trackTitle, number++, seconds));
        }
    }

    private void playlist(Map<String, Track> catalog, String name, String description, Visibility visibility,
                          String... trackTitles) {
        Playlist playlist = new Playlist(name, description, visibility);
        for (String title : trackTitles) {
            Track track = catalog.get(title);
            if (track == null) {
                log.warn("Faixa '{}' não encontrada; ignorada na playlist '{}'", title, name);
                continue;
            }
            playlist.addTrack(track);
        }
        playlistRepository.save(playlist);
    }

    /** Converte "m:ss" em segundos; devolve null quando a duração não foi informada. */
    private static Integer toSeconds(String duration) {
        if (duration == null || duration.isBlank()) {
            return null;
        }
        String[] parts = duration.trim().split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }
}
