/*
 * Página de demonstração da Scene API.
 *
 * A página fica em uma origem (por exemplo http://localhost:3000) e a API em outra (http://localhost:8080).
 * Origem é o conjunto esquema + host + porta. Quando a origem da página é diferente da origem da API,
 * o navegador só entrega a resposta ao JavaScript se a API autorizar, e essa autorização é o CORS.
 * A lista de origens autorizadas fica em scene.cors.allowed-origins, no application.properties da API.
 */

// Endereço da API. Dá para trocar pela URL da página: index.html?api=http://localhost:9090
const API_URL = new URLSearchParams(location.search).get('api') || 'http://localhost:8080';

// Nome oficial (em inglês) de cada status HTTP que a API devolve
const STATUS_NAMES = {
    200: 'OK', 201: 'Created', 204: 'No Content',
    400: 'Bad Request', 403: 'Forbidden', 404: 'Not Found', 405: 'Method Not Allowed', 406: 'Not Acceptable',
    409: 'Conflict', 415: 'Unsupported Media Type', 422: 'Unprocessable Content', 429: 'Too Many Requests',
    500: 'Internal Server Error'
};

// Cabeçalhos de resposta que valem a pena mostrar. O JavaScript só consegue ler estes cabeçalhos
// porque a API os lista em Access-Control-Expose-Headers (ver CorsConfig.java).
const RESPONSE_HEADERS = ['X-API-Version', 'X-RateLimit-Limit', 'X-RateLimit-Remaining', 'X-RateLimit-Reset',
    'X-Idempotent-Replayed', 'Retry-After', 'Location'];

const BURST_SIZE = 120;

const $ = (id) => document.getElementById(id);

let selectedArtistId = null;  // artista mostrado no painel de versionamento
let selectedVersion = '1';    // valor enviado em X-API-Version
let createdPlaylistId = null; // última playlist criada pela página


// ---------------------------------------------------------------------------------------------
// Chamadas à API
// ---------------------------------------------------------------------------------------------

/**
 * Faz uma chamada à API e devolve { status, headers, data }.
 * "path" pode ser um caminho ("/artists") ou um endereço completo (os links HATEOAS já vêm completos).
 */
async function callApi(method, path, { headers = {}, body } = {}) {
    const url = path.startsWith('http') ? path : API_URL + path;
    const options = { method, headers: { Accept: 'application/json', ...headers } };
    if (body !== undefined) {
        options.headers['Content-Type'] = 'application/json';
        options.body = JSON.stringify(body);
    }

    let response;
    try {
        response = await fetch(url, options);
    } catch (error) {
        // O fetch só lança erro quando a resposta não chega ao JavaScript: API desligada ou bloqueio de CORS
        await explainFailure();
        logFailure(method, url);
        throw error;
    }

    showCorsState('allowed');
    const data = parseBody(await response.text());
    updateMeter(response.headers);
    logExchange(method, url, options.headers, response, data);
    return { status: response.status, headers: response.headers, data };
}

/**
 * Descobre por que a chamada falhou. Uma requisição "no-cors" não entrega a resposta ao JavaScript,
 * mas mostra se a API está no ar: se ela responde, o problema era o CORS; se não, a API está desligada.
 */
async function explainFailure() {
    try {
        await fetch(API_URL + '/artists?page=0&size=1', { mode: 'no-cors' });
        showCorsState('blocked');
    } catch (error) {
        showCorsState('offline');
    }
}

/** A API sempre responde JSON. Se vier outra coisa (ou nada, como no 204), a página segue funcionando. */
function parseBody(text) {
    if (text === '') {
        return null;
    }
    try {
        return JSON.parse(text);
    } catch (error) {
        return { message: 'Resposta que não é JSON', details: [] };
    }
}

function statusText(status) {
    return status + ' ' + (STATUS_NAMES[status] || '');
}


// ---------------------------------------------------------------------------------------------
// CORS: as duas origens no topo da página
// ---------------------------------------------------------------------------------------------

function pageOrigin() {
    // Arquivo aberto direto do disco (file://) não tem origem: o navegador manda "Origin: null"
    return location.protocol === 'file:' ? 'null (arquivo aberto do disco)' : location.origin;
}

function showCorsState(state) {
    const messages = {
        allowed: ['CORS liberado', ''],
        blocked: ['Bloqueado pelo CORS',
            'A API está no ar, mas não libera a origem desta página, então o navegador barra a resposta. ' +
            'Abra a página por http://localhost:3000 (ou outra origem listada em scene.cors.allowed-origins). ' +
            'O erro completo aparece no Console do navegador (F12).'],
        offline: ['API fora do ar',
            'Nenhuma resposta em ' + API_URL + '. Suba a aplicação (SceneApiApplication) e recarregue a página.']
    };
    const [status, help] = messages[state];
    $('link').dataset.state = state;
    $('cors-status').textContent = status;
    $('cors-help').textContent = help;
    $('cors-help').hidden = help === '';
}


// ---------------------------------------------------------------------------------------------
// Artistas: lista paginada, busca e links HATEOAS
// ---------------------------------------------------------------------------------------------

async function loadArtists(path) {
    const list = $('artist-list');
    let status, data;
    try {
        ({ status, data } = await callApi('GET', path));
    } catch (error) {
        list.replaceChildren(listMessage('Sem dados: a chamada à API não foi concluída.'));
        setPager(null, null, '');
        return;
    }
    list.replaceChildren();

    if (status !== 200) {
        list.append(listMessage(statusText(status) + ': ' + data.message));
        setPager(null, null, '');
        return;
    }

    // Uma página vazia vem sem o bloco _embedded
    const artists = data._embedded ? data._embedded.artists : [];
    if (artists.length === 0) {
        list.append(listMessage('Nenhum artista encontrado.'));
    }
    for (const artist of artists) {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'artist';
        button.append(element('span', 'artist-name', artist.name), element('span', 'artist-genre', artist.genre));
        button.addEventListener('click', () => run(() => selectArtist(artist.id, button)));
        const item = document.createElement('li');
        item.append(button);
        list.append(item);
    }

    // HATEOAS: a resposta já traz o endereço da página anterior e da próxima, quando existem
    const links = data._links || {};
    const page = data.page;
    setPager(links.prev, links.next, 'Página ' + (page.number + 1) + ' de ' + Math.max(page.totalPages, 1)
        + ' (' + page.totalElements + ' artistas)');
}

function setPager(prev, next, text) {
    $('page-prev').disabled = !prev;
    $('page-next').disabled = !next;
    $('page-prev').dataset.href = prev ? prev.href : '';
    $('page-next').dataset.href = next ? next.href : '';
    $('page-info').textContent = text;
}

function listMessage(text) {
    return element('li', 'list-message', text);
}

function firstPage() {
    return '/artists?page=0&size=5&sort=name,asc';
}


// ---------------------------------------------------------------------------------------------
// Versionamento: o mesmo artista nas versões 1 e 2
// ---------------------------------------------------------------------------------------------

function selectArtist(id, button) {
    selectedArtistId = id;
    for (const other of document.querySelectorAll('.artist')) {
        other.setAttribute('aria-current', other === button ? 'true' : 'false');
    }
    return loadArtistVersion();
}

async function loadArtistVersion() {
    if (selectedArtistId === null) {
        return;
    }
    // A versão não vai na URL: vai no cabeçalho X-API-Version
    const { status, data } = await callApi('GET', '/artists/' + selectedArtistId,
        { headers: { 'X-API-Version': selectedVersion } });
    $('version-hint').textContent = 'GET /artists/' + selectedArtistId + ' com X-API-Version: ' + selectedVersion
        + ' respondeu ' + statusText(status);
    $('artist-json').textContent = JSON.stringify(data, null, 2);
}

function chooseVersion(version) {
    selectedVersion = version;
    for (const option of document.querySelectorAll('.switch-option')) {
        option.setAttribute('aria-pressed', option.dataset.version === version ? 'true' : 'false');
    }
    return loadArtistVersion();
}


// ---------------------------------------------------------------------------------------------
// Idempotência: POST /playlists com X-Idempotency-Key
// ---------------------------------------------------------------------------------------------

function newKey() {
    // randomUUID só existe em contexto seguro (https ou localhost); fora disso, monta uma chave parecida
    $('idempotency-key').value = crypto.randomUUID
        ? crypto.randomUUID()
        : 'chave-' + Date.now().toString(36) + '-' + Math.random().toString(36).slice(2, 10);
}

async function createPlaylist() {
    const headers = {};
    if ($('send-key').checked) {
        headers['X-Idempotency-Key'] = $('idempotency-key').value;
    }
    const body = { name: $('playlist-name').value, visibility: $('playlist-visibility').value };

    const { status, headers: responseHeaders, data } = await callApi('POST', '/playlists', { headers, body });
    const result = $('playlist-result');
    result.replaceChildren();

    if (status === 201) {
        createdPlaylistId = data.id;
        $('delete-playlist').hidden = false;
        const replayed = responseHeaders.get('X-Idempotent-Replayed') === 'true';
        result.dataset.kind = 'ok';
        result.append(element('strong', '', statusText(status)), element('p', '', replayed
            ? 'A API reconheceu a chave e devolveu a resposta original (X-Idempotent-Replayed: true). '
              + 'Nenhuma playlist nova foi criada: continua sendo a de id ' + data.id + '.'
            : 'Playlist criada com id ' + data.id + '. Clique em "Criar playlist" de novo sem mudar nada para ver a repetição.'));
        return;
    }

    // Erros vêm sempre no mesmo formato: status, error, message e details (um item por campo com problema)
    result.dataset.kind = 'error';
    result.append(element('strong', '', statusText(status)), element('p', '', data.message));
    for (const detail of data.details) {
        result.append(element('p', 'detail', detail.field + ': ' + detail.message));
    }
}

async function deletePlaylist() {
    const { status, data } = await callApi('DELETE', '/playlists/' + createdPlaylistId);
    const result = $('playlist-result');
    result.replaceChildren();
    result.dataset.kind = status === 204 ? 'ok' : 'error';
    result.append(element('strong', '', statusText(status)), element('p', '', status === 204
        ? 'Playlist ' + createdPlaylistId + ' excluída. O DELETE também só passa porque o CORS libera esse método.'
        : data.message));
    if (status === 204 || status === 404) {
        createdPlaylistId = null;
        $('delete-playlist').hidden = true;
    }
}


// ---------------------------------------------------------------------------------------------
// Rate limiting: medidor de fichas e rajada
// ---------------------------------------------------------------------------------------------

function updateMeter(headers) {
    // Respostas que não passam pelo rate limiting (por exemplo, versão inválida) não trazem os cabeçalhos
    if (!headers.has('X-RateLimit-Limit')) {
        return;
    }
    const limit = Number(headers.get('X-RateLimit-Limit'));
    const remaining = Number(headers.get('X-RateLimit-Remaining'));
    const reset = Number(headers.get('X-RateLimit-Reset'));
    $('meter-fill').style.width = (100 * remaining / limit) + '%';
    $('meter').dataset.empty = remaining === 0 ? 'true' : 'false';
    $('meter-text').textContent = remaining + ' de ' + limit + ' fichas. Balde cheio de novo em ' + reset + ' s.';
}

async function burst() {
    const button = $('burst');
    const result = $('burst-result');
    button.disabled = true;
    result.replaceChildren();
    result.dataset.kind = '';

    try {
        // Dispara tudo de uma vez, sem esperar uma resposta para mandar a próxima
        const calls = [];
        for (let i = 0; i < BURST_SIZE; i++) {
            calls.push(fetch(API_URL + '/artists?page=0&size=1', { headers: { Accept: 'application/json' } }));
        }
        const responses = await Promise.all(calls);

        const refused = responses.filter((response) => response.status === 429);
        const accepted = responses.length - refused.length;
        const emptiest = responses.reduce((a, b) =>
            Number(a.headers.get('X-RateLimit-Remaining')) <= Number(b.headers.get('X-RateLimit-Remaining')) ? a : b);
        updateMeter(emptiest.headers);

        result.dataset.kind = refused.length > 0 ? 'error' : 'ok';
        result.append(element('strong', '', accepted + ' aceitas (200 OK), ' + refused.length + ' recusadas (429 Too Many Requests)'));
        if (refused.length > 0) {
            result.append(element('p', '', 'Retry-After: ' + refused[0].headers.get('Retry-After')
                + ' s. O balde repõe as fichas aos poucos; use "Atualizar as fichas" para acompanhar.'));
        } else {
            result.append(element('p', '', 'O balde tinha fichas para todas. Dispare de novo para esvaziar.'));
        }
        logSummary(BURST_SIZE + ' x GET /artists?page=0&size=1', accepted + ' x 200 OK, ' + refused.length + ' x 429 Too Many Requests',
            refused.length > 0 ? 'error' : 'ok');
    } catch (error) {
        await explainFailure();
        logFailure('GET', API_URL + '/artists?page=0&size=1');
    } finally {
        button.disabled = false;
    }
}


// ---------------------------------------------------------------------------------------------
// Tráfego: registro das chamadas
// ---------------------------------------------------------------------------------------------

function logExchange(method, url, requestHeaders, response, data) {
    const entry = logEntry(response.ok ? 'ok' : 'error');
    entry.append(element('p', 'log-request', method + ' ' + url.replace(API_URL, '')));

    // Só os cabeçalhos da Parte 2 enviados nesta chamada
    for (const name of ['X-API-Version', 'X-Idempotency-Key']) {
        if (requestHeaders[name] !== undefined) {
            entry.append(element('p', 'log-header sent', name + ': ' + requestHeaders[name]));
        }
    }
    entry.append(element('p', 'log-status', statusText(response.status)));
    for (const name of RESPONSE_HEADERS) {
        if (response.headers.has(name)) {
            entry.append(element('p', 'log-header', name + ': ' + response.headers.get(name)));
        }
    }
    if (!response.ok && data && data.message) {
        entry.append(element('p', 'log-message', data.message));
    }
}

function logFailure(method, url) {
    const entry = logEntry('error');
    entry.append(element('p', 'log-request', method + ' ' + url.replace(API_URL, '')),
        element('p', 'log-status', 'Sem resposta'),
        element('p', 'log-message', $('cors-status').textContent + '. ' + $('cors-help').textContent));
}

function logSummary(request, outcome, kind) {
    const entry = logEntry(kind);
    entry.append(element('p', 'log-request', request), element('p', 'log-status', outcome));
}

function logEntry(kind) {
    const entry = document.createElement('li');
    entry.className = 'log-entry';
    entry.dataset.kind = kind;
    entry.append(element('time', 'log-time', new Date().toLocaleTimeString('pt-BR')));
    $('log-list').prepend(entry);
    return entry;
}


// ---------------------------------------------------------------------------------------------
// Utilitário e eventos
// ---------------------------------------------------------------------------------------------

/** Cria um elemento com classe e texto. O texto entra por textContent, então nada vindo da API vira HTML. */
function element(tag, className, text) {
    const node = document.createElement(tag);
    if (className) {
        node.className = className;
    }
    node.textContent = text;
    return node;
}

/** Roda uma ação que chama a API. O erro de rede não precisa de tratamento aqui: callApi já explicou na tela. */
function run(action) {
    Promise.resolve(action()).catch(() => {});
}

$('page-origin').textContent = pageOrigin();
$('api-origin').textContent = API_URL;
$('docs-link').href = API_URL + '/swagger-ui/index.html';

$('search-form').addEventListener('submit', (event) => {
    event.preventDefault();
    const name = $('search-name').value.trim();
    run(() => loadArtists(name === '' ? firstPage() : '/artists/search?name=' + encodeURIComponent(name) + '&page=0&size=5&sort=name,asc'));
});
$('search-clear').addEventListener('click', () => {
    $('search-name').value = '';
    run(() => loadArtists(firstPage()));
});
$('page-prev').addEventListener('click', (event) => run(() => loadArtists(event.target.dataset.href)));
$('page-next').addEventListener('click', (event) => run(() => loadArtists(event.target.dataset.href)));

for (const option of document.querySelectorAll('.switch-option')) {
    option.addEventListener('click', () => run(() => chooseVersion(option.dataset.version)));
}

$('playlist-form').addEventListener('submit', (event) => {
    event.preventDefault();
    run(createPlaylist);
});
$('new-key').addEventListener('click', newKey);
$('delete-playlist').addEventListener('click', () => run(deletePlaylist));

$('burst').addEventListener('click', () => run(burst));
$('refresh-meter').addEventListener('click', () => run(() => callApi('GET', '/artists?page=0&size=1')));
$('log-clear').addEventListener('click', () => $('log-list').replaceChildren());

newKey();
run(() => loadArtists(firstPage()));
