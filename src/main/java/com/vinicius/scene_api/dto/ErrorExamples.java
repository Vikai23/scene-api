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
            {"status": 400, "error": "Bad Request", "message": "JSON malformado: confira se as aspas, vírgulas e chaves estão no lugar", "path": "/artists", "timestamp": "2026-10-09T10:15:30", "details": []}""";

    public static final String BAD_REQUEST_WRONG_TYPE = """
            {"status": 400, "error": "Bad Request", "message": "JSON com valor de tipo inválido: 1 campo(s) com problema", "path": "/artists", "timestamp": "2026-10-09T10:15:30", "details": [{"field": "formedYear", "message": "deve ser um número inteiro, sem aspas e sem casas decimais"}]}""";

    public static final String BAD_REQUEST_BODY_MISSING = """
            {"status": 400, "error": "Bad Request", "message": "Corpo da requisição ausente: envie os dados em JSON", "path": "/artists", "timestamp": "2026-10-09T10:15:30", "details": []}""";

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

    public static final String BAD_REQUEST_API_VERSION = """
            {"status": 400, "error": "Bad Request", "message": "Versão da API não suportada: '9'. Use o cabeçalho X-API-Version com 1 ou 2 (sem o cabeçalho vale a versão 1).", "path": "/artists/1", "timestamp": "2026-10-09T10:15:30", "details": []}""";

    public static final String BAD_REQUEST_IDEMPOTENCY_KEY = """
            {"status": 400, "error": "Bad Request", "message": "Cabeçalho X-Idempotency-Key inválido: use de 1 a 100 caracteres entre letras, números, ponto, hífen, dois-pontos e underline (ex.: um UUID)", "path": "/artists", "timestamp": "2026-10-09T10:15:30", "details": []}""";

    public static final String BAD_REQUEST_PAGE_TOO_LARGE = """
            {"status": 400, "error": "Bad Request", "message": "Número de página muito grande para o tamanho informado (página x tamanho passa de 2147483647)", "path": "/artists", "timestamp": "2026-10-09T10:15:30", "details": []}""";

    // ---------- 403 Forbidden ----------

    public static final String FORBIDDEN_CORS = """
            {"status": 403, "error": "Forbidden", "message": "Requisição bloqueada pela política de CORS: a origem 'http://site-nao-autorizado.com' não está na lista de origens permitidas", "path": "/artists", "timestamp": "2026-10-09T10:15:30", "details": []}""";

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

    // ---------- 406 Not Acceptable ----------

    public static final String NOT_ACCEPTABLE = """
            {"status": 406, "error": "Not Acceptable", "message": "Formato de resposta não suportado: use Accept: application/json", "path": "/artists", "timestamp": "2026-10-09T10:15:30", "details": []}""";

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

    public static final String CONFLICT_IDEMPOTENCY_IN_PROGRESS = """
            {"status": 409, "error": "Conflict", "message": "Já existe uma requisição em andamento com a chave de idempotência 'pedido-123'. Aguarde e tente de novo.", "path": "/artists", "timestamp": "2026-10-09T10:15:30", "details": []}""";

    // ---------- 415 Unsupported Media Type ----------

    public static final String UNSUPPORTED_MEDIA_TYPE = """
            {"status": 415, "error": "Unsupported Media Type", "message": "Content-Type não suportado: envie o corpo como application/json", "path": "/artists", "timestamp": "2026-10-09T10:15:30", "details": []}""";

    // ---------- 422 Unprocessable Content ----------

    public static final String UNPROCESSABLE_IDEMPOTENCY_KEY = """
            {"status": 422, "error": "Unprocessable Content", "message": "A chave de idempotência 'pedido-123' já foi usada em outra requisição (POST /artists). Use uma chave nova para cada operação diferente.", "path": "/artists", "timestamp": "2026-10-09T10:15:30", "details": []}""";

    // ---------- 429 Too Many Requests ----------

    public static final String TOO_MANY_REQUESTS = """
            {"status": 429, "error": "Too Many Requests", "message": "Limite de requisições excedido. Tente novamente em 1 segundo(s).", "path": "/artists", "timestamp": "2026-10-09T10:15:30", "details": []}""";
}
