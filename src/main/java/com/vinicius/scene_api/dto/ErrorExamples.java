package com.vinicius.scene_api.dto;

/**
 * Exemplos de corpo de erro usados na documentação do Swagger.
 */
public final class ErrorExamples {

    private ErrorExamples() {
    }

    // ---------- 400 Bad Request ----------

    public static final String BAD_REQUEST_VALIDATION = """
            {"status": 400, "error": "Bad Request", "message": "Dados inválidos: 2 campo(s) com problema", "path": "/artists", "timestamp": "2026-10-09T10:15:30", "details": [{"field": "genre", "message": "não deve ser nulo"}, {"field": "name", "message": "tamanho deve ser entre 2 e 100"}]}""";

    public static final String BAD_REQUEST_MALFORMED = """
            {"status": 400, "error": "Bad Request", "message": "JSON malformado ou com valor inválido (ex.: texto onde se espera número, ou valor de enum inexistente)", "path": "/artists", "timestamp": "2026-10-09T10:15:30", "details": []}""";

    public static final String BAD_REQUEST_ID = """
            {"status": 400, "error": "Bad Request", "message": "Parâmetro 'id' com valor inválido: abc.", "path": "/artists/abc", "timestamp": "2026-10-09T10:15:30", "details": []}""";

    public static final String BAD_REQUEST_SORT = """
            {"status": 400, "error": "Bad Request", "message": "Campo de ordenação inexistente: foo", "path": "/artists", "timestamp": "2026-10-09T10:15:30", "details": []}""";

    public static final String BAD_REQUEST_PARAM_MISSING = """
            {"status": 400, "error": "Bad Request", "message": "Parâmetro obrigatório ausente: name", "path": "/artists/search", "timestamp": "2026-10-09T10:15:30", "details": []}""";

    public static final String BAD_REQUEST_PARAM_BLANK = """
            {"status": 400, "error": "Bad Request", "message": "Parâmetro inválido", "path": "/artists/search", "timestamp": "2026-10-09T10:15:30", "details": [{"field": "name", "message": "não deve estar em branco"}]}""";

    public static final String BAD_REQUEST_ENUM_GENRE = """
            {"status": 400, "error": "Bad Request", "message": "Parâmetro 'genre' com valor inválido: FUNK. Valores aceitos: [ROCK, ALTERNATIVE, PUNK, EMO, POST_HARDCORE, METALCORE, METAL, POP, HIP_HOP, ELECTRONIC, JAZZ, FOLK, CLASSICAL, OTHER].", "path": "/artists/by-genre/FUNK", "timestamp": "2026-10-09T10:15:30", "details": []}""";

    public static final String BAD_REQUEST_ENUM_TYPE = """
            {"status": 400, "error": "Bad Request", "message": "Parâmetro 'type' com valor inválido: BOX. Valores aceitos: [ALBUM, EP, SINGLE, COMPILATION, LIVE].", "path": "/albums/by-type/BOX", "timestamp": "2026-10-09T10:15:30", "details": []}""";

    public static final String BAD_REQUEST_ENUM_VISIBILITY = """
            {"status": 400, "error": "Bad Request", "message": "Parâmetro 'visibility' com valor inválido: HIDDEN. Valores aceitos: [PUBLIC, PRIVATE].", "path": "/playlists/by-visibility/HIDDEN", "timestamp": "2026-10-09T10:15:30", "details": []}""";

    public static final String BAD_REQUEST_PROFILE_ARTIST = """
            {"status": 400, "error": "Bad Request", "message": "O artista de um perfil existente não pode ser alterado", "path": "/artist-profiles/1", "timestamp": "2026-10-09T10:15:30", "details": []}""";

    // ---------- 404 Not Found ----------

    public static final String NOT_FOUND_ARTIST = """
            {"status": 404, "error": "Not Found", "message": "Artista não encontrado(a) com id: 99", "path": "/artists/99", "timestamp": "2026-10-09T10:15:30", "details": []}""";

    public static final String NOT_FOUND_PROFILE = """
            {"status": 404, "error": "Not Found", "message": "Perfil não encontrado(a) com id: 99", "path": "/artist-profiles/99", "timestamp": "2026-10-09T10:15:30", "details": []}""";

    public static final String NOT_FOUND_PROFILE_OF_ARTIST = """
            {"status": 404, "error": "Not Found", "message": "Perfil do artista não encontrado(a) com id: 6", "path": "/artist-profiles/by-artist/6", "timestamp": "2026-10-09T10:15:30", "details": []}""";

    public static final String NOT_FOUND_ALBUM = """
            {"status": 404, "error": "Not Found", "message": "Álbum não encontrado(a) com id: 99", "path": "/albums/99", "timestamp": "2026-10-09T10:15:30", "details": []}""";

    public static final String NOT_FOUND_TRACK = """
            {"status": 404, "error": "Not Found", "message": "Faixa não encontrado(a) com id: 99", "path": "/tracks/99", "timestamp": "2026-10-09T10:15:30", "details": []}""";

    public static final String NOT_FOUND_PLAYLIST = """
            {"status": 404, "error": "Not Found", "message": "Playlist não encontrado(a) com id: 99", "path": "/playlists/99", "timestamp": "2026-10-09T10:15:30", "details": []}""";

    public static final String NOT_FOUND_TRACK_IN_PLAYLIST = """
            {"status": 404, "error": "Not Found", "message": "A faixa 5 não está na playlist 1", "path": "/playlists/1/tracks/5", "timestamp": "2026-10-09T10:15:30", "details": []}""";

    // ---------- 409 Conflict ----------

    public static final String CONFLICT_ARTIST = """
            {"status": 409, "error": "Conflict", "message": "Já existe um artista com o nome 'Alesana'", "path": "/artists", "timestamp": "2026-10-09T10:15:30", "details": []}""";

    public static final String CONFLICT_PROFILE = """
            {"status": 409, "error": "Conflict", "message": "O artista de id 1 já possui um perfil", "path": "/artist-profiles", "timestamp": "2026-10-09T10:15:30", "details": []}""";

    public static final String CONFLICT_ALBUM = """
            {"status": 409, "error": "Conflict", "message": "Esse artista já tem um álbum chamado 'Sempiternal'", "path": "/albums", "timestamp": "2026-10-09T10:15:30", "details": []}""";

    public static final String CONFLICT_TRACK = """
            {"status": 409, "error": "Conflict", "message": "Esse álbum já tem uma faixa de número 1", "path": "/tracks", "timestamp": "2026-10-09T10:15:30", "details": []}""";

    public static final String CONFLICT_PLAYLIST = """
            {"status": 409, "error": "Conflict", "message": "Já existe uma playlist com o nome 'Heavy Rotation'", "path": "/playlists", "timestamp": "2026-10-09T10:15:30", "details": []}""";

    public static final String CONFLICT_TRACK_IN_PLAYLIST = """
            {"status": 409, "error": "Conflict", "message": "A faixa 5 já está na playlist 1", "path": "/playlists/1/tracks/5", "timestamp": "2026-10-09T10:15:30", "details": []}""";
}
