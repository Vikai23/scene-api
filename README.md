# Scene API

API REST de catálogo musical em **Java 21** e **Spring Boot**: artistas, perfis, álbuns, faixas e playlists, com os três tipos de relacionamento (1:1, 1:N e N:N), paginação, validação, links HATEOAS e documentação Swagger. Na Parte 2 entraram versionamento por header, idempotência, autenticação por chave de API, rate limiting e CORS, com um front-end em Angular, em outro repositório, que consome a API a partir de outra origem.

> Projeto final da disciplina de **Web Services** (Senac TSI) — **Parte 1** (out/2026) e **Parte 2** (nov/2026).

[![CI](https://github.com/Vikai23/scene-api/actions/workflows/ci.yml/badge.svg)](https://github.com/Vikai23/scene-api/actions/workflows/ci.yml)

**43 endpoints** · **129 requisições no Postman** (62 delas de erro) · **47 testes automatizados** · **105 registros de exemplo** carregados ao subir

![Swagger UI da Scene API, com tema customizado e o seletor Versão da API](docs/swagger-hero.png)

---

## Índice

- [Destaques](#destaques)
- [Como executar](#como-executar)
- [Dados iniciais](#dados-iniciais)
- [Modelo de dados](#modelo-de-dados)
- [Convenções da API](#convenções-da-api)
- [Parte 2: recursos avançados](#parte-2-recursos-avançados)
- [Regras de validação](#regras-de-validação)
- [API Reference](#api-reference)
- [Consultas no Console do H2](#consultas-no-console-do-h2)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Licença](#licença)
- [Autor](#autor)

## Destaques

- **Arquitetura em camadas:** `controller` → `service` → `repository`, com DTOs separados para entrada (`*Request`) e saída (`*Response`). As entidades JPA não vazam para fora da API.
- **Os três relacionamentos do enunciado:** 1:1 (artista ↔ perfil), 1:N (artista → álbuns → faixas) e N:N (playlists ↔ faixas), cada um com endpoints próprios de consulta.
- **Erros padronizados:** um `GlobalExceptionHandler` devolve sempre o mesmo corpo, com a lista campo a campo nos erros de validação. Treze status diferentes, cada um com o seu caso, e cada rota lista no Swagger todos os erros que podem acontecer nela. ([detalhes](#erros))
- **Validação em dois níveis:** as mesmas regras nos DTOs de entrada e nas entidades (Bean Validation), com expressões regulares compartilhadas em `validation/`.
- **HATEOAS (HAL):** cada recurso traz `_links` para si mesmo, para a coleção e para os recursos relacionados; as listagens vêm em `PagedModel`.
- **Paginação e ordenação** em todas as listagens, inclusive nas buscas e nas consultas por relação.
- **Parte 2, recursos avançados:** versionamento por header (`X-API-Version`), idempotência nos `POST` (`X-Idempotency-Key`), autenticação por chave de API nas rotas que alteram dados (`X-API-Key`, `401` sem chave válida), rate limiting por token bucket (`429` com `Retry-After`) e CORS para origens específicas. ([detalhes](#parte-2-recursos-avançados))
- **Front-end em Angular:** o [scene-web](https://github.com/Vikai23/scene-web) é um repositório separado que roda em outra origem e usa as 43 rotas pelo navegador. Ele tem o CRUD das 5 entidades, a chave de API, a comparação das duas versões, idempotência, o medidor do rate limit e uma aba que provoca cada erro. O CORS é o que libera a ligação entre os dois. ([detalhes](#front-end-em-angular))
- **Testado:** 47 testes automatizados (JUnit), a maioria subindo a aplicação inteira e fazendo requisições HTTP de verdade, e coleção do Postman com 309 testes de status, de corpo, de headers e de links — incluindo 62 casos de erro.

### Tecnologias

| Camada | O que foi usado |
| :-- | :-- |
| Linguagem e build | Java 21 · Maven (wrapper `mvnw`) |
| Framework | Spring Boot 4.1.1 · Spring Web MVC · Spring Data JPA · Hibernate |
| Validação | Bean Validation (Jakarta Validation) |
| Hipermídia | Spring HATEOAS (formato HAL) |
| Banco | H2 (em memória) |
| Documentação | Springdoc OpenAPI / Swagger UI 3.1.1, com tema customizado |
| Parte 2 | Versionamento de API nativo do Spring Framework 7 · filtro de CORS do Spring · token bucket e idempotência escritos no próprio projeto, sem biblioteca extra |
| Front-end | Angular 21, em repositório separado ([scene-web](https://github.com/Vikai23/scene-web)) |

## Como executar

Requisito: **JDK 21 ou superior**. O Maven já vem junto no projeto (`mvnw`), então não precisa instalar nada além do JDK.

```bash
# Linux / macOS
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

A API sobe em `http://localhost:8080`; abrir a raiz já redireciona para o Swagger.

| Recurso | URL |
| :-- | :-- |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| OpenAPI (JSON) | http://localhost:8080/v3/api-docs |
| Console do H2 | http://localhost:8080/h2-console |
| Front-end em Angular | http://localhost:4200, depois de `npm start` no [scene-web](https://github.com/Vikai23/scene-web) ([como usar](#front-end-em-angular)) |

No Console do H2: JDBC URL `jdbc:h2:mem:scenedb`, usuário `sa`, senha em branco.

O Swagger UI tem tema próprio e traz, além dos 43 endpoints, o modelo de dados, as convenções de paginação e a tabela de status:

<!-- Captura dos endpoints: abra o grupo ARTISTAS no Swagger (deixando os endpoints recolhidos),
     tire o print e salve como docs/swagger-endpoints.png; depois descomente a linha abaixo.
     ![Os 43 endpoints, agrupados por recurso](docs/swagger-endpoints.png) -->

![Tabela de status HTTP documentada no Swagger UI](docs/swagger-erros.png)

Para rodar os 47 testes automatizados (os de integração sobem a aplicação inteira em uma porta livre, com o H2 e a carga inicial, e fazem requisições HTTP de verdade):

```bash
./mvnw test
```

### Coleção do Postman

A coleção está em [`postman/scene-api.postman_collection.json`](postman/scene-api.postman_collection.json): **129 requisições** e **309 testes**, em 12 pastas — os 43 endpoints, os passos extras de fluxo, 62 casos de erro (`400`, `401`, `403`, `404`, `405`, `406`, `409`, `415`, `422` e `429`) e as demonstrações da Parte 2.

1. No Postman: **Import** → selecione o arquivo `.json`.
2. Suba a aplicação (`http://localhost:8080`). A variável `baseUrl` já vem configurada.
3. Rode as pastas **0 a 11**, de cima para baixo (ou use o **Collection Runner**).

Os ids criados são guardados automaticamente em variáveis (`artistId`, `profileId`, `albumId`, `trackId`, `track2Id` e `playlistId`), e cada requisição tem testes de status, de corpo, de headers e de links HATEOAS. A pasta 0 gera a chave de API e guarda em `apiKey`, que a aba **Authorization** da coleção envia no header `X-API-Key` de todas as requisições.

| Pasta | O que faz |
| :-- | :-- |
| 0. Chave de API | Gera a chave que a coleção usa (`POST /api-keys`), consulta e revoga (`/api-keys/me`) e os `401`: sem chave, chave inexistente e chave revogada |
| 1. Artists | CRUD de artistas, buscas e erros de validação |
| 2. Artist Profiles | Perfil do artista criado na pasta 1 (relação 1:1) |
| 3. Albums | Álbuns do artista (relação 1:N) |
| 4. Tracks | Faixas do álbum (relação 1:N) |
| 5. Playlists | Playlists e faixas dentro delas (relação N:N) |
| 6. Erros gerais | Rota inexistente, método, Content-Type e Accept errados, JSON ruim ou com valor de tipo errado, ordenação inválida |
| 7. Limpeza | Remove o que as pastas 1 a 5 criaram e confere as cascatas |
| 8. Versionamento | `GET /artists/{id}` nas versões 1 e 2, e o `400` de versão inexistente |
| 9. Idempotência | `POST` repetido com a mesma chave, chave reaproveitada (`422`) e chave fora do formato (`400`) |
| 10. CORS | Origem permitida, origem bloqueada (`403`) e o preflight (`OPTIONS`) |
| 11. Rate limiting | Headers `X-RateLimit-*`, o burst que esvazia o bucket (`429`) e a volta depois da espera |

> **A coleção respeita o rate limit.** A API aceita 30 requisições por minuto por IP e a coleção inteira passa disso. Por isso ela tem um script próprio (aba **Scripts** da coleção) que lê os headers `X-RateLimit-*` de cada resposta e espera a reposição dos tokens quando eles estão acabando. A execução inteira leva cerca de 3 minutos. Reiniciar a aplicação enche o bucket de novo.

## Dados iniciais

O H2 é **em memória**: os dados somem ao encerrar a aplicação. Por isso, ao subir — e somente se ainda não houver nenhum artista —, a classe `DataLoader` cadastra automaticamente:

| Quantidade | O quê | Observação |
| --: | :-- | :-- |
| 6 | artistas | Bring Me the Horizon, Title Fight, Asking Alexandria, blessthefall, Alesana e I Promised the World |
| 5 | perfis | o de *I Promised the World* fica de fora de propósito, para dar para testar o `POST /artist-profiles` e o `404` por artista |
| 8 | álbuns | inclui um EP sem faixas, que serve para testar a consulta por formato |
| 82 | faixas | com número e duração |
| 4 | playlists | *Heavy Rotation* (PUBLIC), *Treino Pesado* (PRIVATE), *Curtas e Grossas* (PUBLIC) e *Madrugada* (PRIVATE), ligando faixas de álbuns diferentes |

Os artistas saem do perfil do autor no Last.fm. Gravadoras, sites e Instagram foram conferidos em fontes públicas (Wikipedia e sites oficiais); o que não foi confirmado fica `null`, porque `label`, `website` e `instagram` são campos opcionais.

## Modelo de dados

```mermaid
erDiagram
    ARTIST ||--o| ARTIST_PROFILE : "tem (1:1)"
    ARTIST ||--o{ ALBUM : "lança (1:N)"
    ALBUM ||--o{ TRACK : "contém (1:N)"
    PLAYLIST }o--o{ TRACK : "reúne (N:N)"

    ARTIST {
        Long id PK
        String name UK
        Genre genre
        String country
        Integer formedYear
    }
    ARTIST_PROFILE {
        Long id PK
        Long artist_id FK
        String biography
        String hometown
        String website
        String instagram
    }
    ALBUM {
        Long id PK
        Long artist_id FK
        String title
        Integer releaseYear
        AlbumType type
        String label
    }
    TRACK {
        Long id PK
        Long album_id FK
        String title
        Integer trackNumber
        Integer durationSeconds
    }
    PLAYLIST {
        Long id PK
        String name UK
        String description
        Visibility visibility
    }
```

| Entidade | Descrição | Relacionamentos |
| :-- | :-- | :-- |
| `Artist` | Artista ou banda | 1:1 com `ArtistProfile`; 1:N com `Album` |
| `ArtistProfile` | Biografia, cidade natal, site e Instagram do artista | 1:1 com `Artist` |
| `Album` | Álbum, EP, single, coletânea ou show ao vivo | N:1 com `Artist`; 1:N com `Track` |
| `Track` | Faixa de um álbum | N:1 com `Album`; N:N com `Playlist` |
| `Playlist` | Lista de faixas de qualquer artista ou álbum | N:N com `Track` (tabela `playlist_tracks`) |

**Enums:** `Genre` (`ROCK`, `ALTERNATIVE`, `PUNK`, `EMO`, `POST_HARDCORE`, `METALCORE`, `METAL`, `POP`, `HIP_HOP`, `ELECTRONIC`, `JAZZ`, `FOLK`, `CLASSICAL`, `OTHER`) · `AlbumType` (`ALBUM`, `EP`, `SINGLE`, `COMPILATION`, `LIVE`) · `Visibility` (`PUBLIC`, `PRIVATE`).

### Decisões e limitações do modelo

O modelo é uma simplificação de propósito: cobre os três tipos de relacionamento do enunciado sem complicar a API. O que ele **não** representa:

- **Álbum com vários artistas.** Cada álbum pertence a um artista. Colaborações, splits e coletâneas de "vários artistas" exigiriam um N:N entre `Album` e `Artist`, com uma tabela de ligação.
- **Participações especiais (feat.).** A faixa não tem artistas próprios; ela pertence ao álbum e, por ele, a um artista. Representar participações exigiria um N:N entre `Track` e `Artist`.
- **A mesma música em mais de um lançamento.** `Track` é a faixa dentro de um álbum, EP ou single, como nos serviços de streaming, em que cada lançamento lista as suas próprias faixas. Uma música lançada como single e depois no álbum vira duas faixas, uma em cada lançamento. Um modelo mais completo separaria a música (a gravação) da faixa no lançamento.
- **Artistas históricos.** `formedYear` aceita de 1000 a 2100, para caberem, por exemplo, compositores clássicos; em artista solo, vale o ano de início da carreira.

## Convenções da API

### Paginação e ordenação

Todas as listagens — inclusive as buscas e as consultas por relação — são paginadas e aceitam os mesmos parâmetros de query:

| Parâmetro | Tipo | Descrição |
| :-- | :-- | :-- |
| `page` | `integer` | Número da página, começando em 0 (padrão `0`). Valor negativo ou que não é número vale o padrão |
| `size` | `integer` | Itens por página (padrão `10`, máximo `50`: valores maiores viram `50`). Zero, negativo ou valor que não é número vale o padrão |
| `sort` | `string` | Ordenação no formato `campo,asc` ou `campo,desc`, ex.: `sort=name,asc` |

Os campos aceitos em `sort` são os do recurso **devolvido** pelo endpoint:

| Recurso devolvido | Campos de ordenação |
| :-- | :-- |
| `Artist` | `id`, `name`, `genre`, `country`, `formedYear` |
| `ArtistProfile` | `id`, `hometown`, `website`, `instagram` |
| `Album` | `id`, `title`, `releaseYear`, `type`, `label` |
| `Track` | `id`, `title`, `trackNumber`, `durationSeconds` |
| `Playlist` | `id`, `name`, `description`, `visibility` |

Um campo de ordenação que não existe devolve `400`.

> Nos dois endpoints que atravessam o N:N, valem os campos do recurso devolvido, e não os do recurso do caminho: `GET /playlists/{id}/tracks` devolve faixas, então ordena por campos de `Track`; `GET /tracks/{id}/playlists` devolve playlists, então ordena por campos de `Playlist`.

### Listas vazias, remoções e valores inválidos

- **Lista vazia não é erro.** Quando a consulta não encontra nada (nenhum artista de um gênero, uma faixa que não está em nenhuma playlist, uma busca sem resultado), a resposta é `200 OK` com a página vazia: `_embedded` ausente e `page.totalElements = 0`. O `404` fica para quando o **recurso pedido pelo id** não existe; por exemplo, `GET /tracks/999/playlists` devolve `404` se a faixa 999 não existir, mas `200` com página vazia se ela existir e não estiver em nenhuma playlist.
- **Remoção devolve `204 No Content`, sem corpo.** Pelo HTTP, o `204` significa "deu certo e não há nada a devolver"; o próprio status é a confirmação. Se o id não existir ou for inválido, a resposta é `404` ou `400`, com o corpo de erro padrão.
- **Enum inválido só aparece fora do Swagger.** No Swagger UI o gênero (e o tipo de álbum e a visibilidade) é uma lista de opções, então não dá para enviar um valor que não existe. O `400` acontece quando a requisição é feita por outro meio (Postman, navegador, curl), por exemplo `GET /artists/by-genre/SAMBA`.
- **Cidade natal e país são validados só no formato.** `hometown` e `country` aceitam apenas letras (com acentos), espaço, ponto, vírgula, apóstrofo e hífen, e têm tamanho limitado. A API não consulta nenhuma base de lugares, então ela não sabe se a cidade existe de verdade.

### Links HATEOAS

Cada recurso traz `_links` com `self`, `update` e `delete`, a coleção e os recursos relacionados. As listagens vêm em `PagedModel` (itens em `_embedded`, mais `_links` de navegação e o bloco `page`).

```json
{
  "id": 1,
  "name": "Bring Me the Horizon",
  "genre": "METALCORE",
  "country": "United Kingdom",
  "formedYear": 2004,
  "_links": {
    "self":    { "href": "http://localhost:8080/artists/1" },
    "update":  { "href": "http://localhost:8080/artists/1" },
    "delete":  { "href": "http://localhost:8080/artists/1" },
    "artists": { "href": "http://localhost:8080/artists" },
    "albums":  { "href": "http://localhost:8080/albums/by-artist/1" },
    "profile": { "href": "http://localhost:8080/artist-profiles/by-artist/1" }
  }
}
```

### Erros

Todo erro tem o mesmo corpo; `details` só vem preenchida nos erros de validação:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Artista não encontrado(a) com id: 99",
  "path": "/artists/99",
  "timestamp": "2026-10-09T10:15:30",
  "details": []
}
```

Erro de validação, campo a campo:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Dados inválidos: 2 campo(s) com problema",
  "path": "/artists",
  "timestamp": "2026-10-09T10:15:30",
  "details": [
    { "field": "genre", "message": "não deve ser nulo" },
    { "field": "name", "message": "tamanho deve ser entre 2 e 100" }
  ]
}
```

| Status | Quando |
| :-- | :-- |
| `200 OK` | Consulta ou atualização bem-sucedida |
| `201 Created` | Recurso criado (o header `Location` aponta para ele) |
| `204 No Content` | Remoção bem-sucedida |
| `400 Bad Request` | Validação falhou, JSON malformado ou com valor de tipo errado, id ou enum inválido, parâmetro ausente ou em branco, ordenação inválida, versão da API inexistente, chave de idempotência fora do formato |
| `401 Unauthorized` | Rota que altera dados chamada sem o header `X-API-Key`, com uma chave que não existe ou com uma chave revogada |
| `403 Forbidden` | Requisição de uma origem que o CORS não libera |
| `404 Not Found` | Recurso (ou artista/álbum/playlist/faixa referenciado) não existe, ou rota inexistente |
| `405 Method Not Allowed` | Verbo HTTP não suportado pela rota |
| `406 Not Acceptable` | Header `Accept` que só aceita um formato diferente de JSON (a requisição nem é executada) |
| `409 Conflict` | Valor único duplicado (nome de artista ou playlist, título de álbum no mesmo artista, número de faixa no mesmo álbum, perfil repetido, faixa já na playlist), ou duas requisições simultâneas com a mesma chave de idempotência |
| `415 Unsupported Media Type` | Corpo enviado com um Content-Type diferente de `application/json` (form-data, texto, XML) |
| `422 Unprocessable Content` | Chave de idempotência reaproveitada em uma requisição diferente |
| `429 Too Many Requests` | Limite de requisições por IP excedido; o header `Retry-After` diz quantos segundos esperar |
| `500 Internal Server Error` | Só para falhas inesperadas do servidor, com mensagem genérica (nenhum dos casos acima chega aqui) |

O JSON de entrada é **estrito**: cada campo só aceita o tipo dele. Texto em campo numérico (`"formedYear": "2004"`), número em campo de texto (`"name": 123`), número com casas decimais em campo inteiro e número no lugar de um enum (`"genre": 1`) devolvem `400`, com o campo apontado em `details`.

## Parte 2: recursos avançados

| Recurso | Como funciona | Onde está no código |
| :-- | :-- | :-- |
| [Versionamento](#versionamento-por-header) | Header `X-API-Version`: `1` ou `2` (sem ele vale a `1`) | `config/ApiVersionConfig`, `ArtistController` |
| [Idempotência](#idempotência) | Header opcional `X-Idempotency-Key` nos 7 `POST` | pacote `idempotency/` |
| [Chave de API](#chave-de-api-x-api-key) | Header `X-API-Key` nas rotas que alteram dados; a chave é gerada em `POST /api-keys` | pacote `apikey/`, `ApiKeyController` |
| [Rate limiting](#rate-limiting-token-bucket) | Token bucket por IP: bucket de 30 tokens, 30 novos por minuto | pacote `ratelimit/` |
| [CORS](#cors) | Só as origens listadas em `scene.cors.allowed-origins` | `config/CorsConfig`, `config/JsonCorsProcessor` |

CORS, rate limiting e idempotência são configurados no `application.properties` (`scene.cors.*`, `scene.rate-limit.*` e `scene.idempotency.*`). Os cinco estão documentados no Swagger: cada rota lista os headers novos e os erros `401`, `403`, `406`, `422` e `429` onde eles podem acontecer.

### Versionamento por header

A versão não vai na URL: vai no header `X-API-Version`. A resposta devolve o mesmo header com a versão que atendeu, e um valor diferente de `1` e `2` devolve `400`.

Hoje só `GET /artists/{id}` muda de uma versão para a outra. As demais rotas respondem igual nas duas.

```http
GET /artists/1
X-API-Version: 1
```
```json
{ "id": 1, "name": "Bring Me the Horizon", "genre": "METALCORE", "country": "United Kingdom", "formedYear": 2004 }
```

```http
GET /artists/1
X-API-Version: 2
```
```json
{
  "id": 1, "name": "Bring Me the Horizon", "genre": "METALCORE",
  "origin": { "country": "United Kingdom", "formedYear": 2004 },
  "albumCount": 1,
  "hasProfile": true
}
```

Na versão 2, `country` e `formedYear` passam para o objeto `origin`, e entram `albumCount` e `hasProfile`. Quem já usa a versão 1 não percebe a mudança, porque continua recebendo o formato antigo. (Os `_links` vêm nas duas versões; ficaram de fora aqui para encurtar.)

No Swagger UI, o seletor **Versão da API**, no topo da página, troca a documentação entre a `v1` e a `v2`, e o selo ao lado do título passa a mostrar `1.0` ou `2.0`. Com a `v2` escolhida, o campo `X-API-Version` de todas as rotas já vem com `2` no *Try it out*, e `GET /artists/{id}` mostra o formato da versão 2. O seletor só preenche esse campo: quem escolhe a versão continua sendo o header HTTP da requisição.

### Idempotência

Uma operação é idempotente quando repetir a mesma requisição não muda o resultado. `GET`, `PUT` e `DELETE` já são assim por natureza; o `POST` não: enviado duas vezes, ele cria dois registros. Isso acontece, por exemplo, quando a resposta se perde no caminho e o cliente tenta de novo.

Para resolver, o cliente gera um valor único (o ideal é um UUID) e manda no header `X-Idempotency-Key`. A API guarda a chave junto com a resposta.

| Situação | Resposta |
| :-- | :-- |
| Primeira requisição com a chave | `201 Created`, com `X-Idempotent-Replayed: false` |
| Mesma chave, mesma requisição | A resposta original de novo, com `X-Idempotent-Replayed: true`; nada é criado |
| Mesma chave, requisição diferente (outro corpo ou outra rota) | `422 Unprocessable Content` |
| Chave fora do formato (1 a 100 letras, números e `. _ : -`) | `400 Bad Request` |
| Duas requisições iguais ao mesmo tempo | Uma é processada; a outra recebe `409 Conflict` e pode tentar de novo |
| A operação falhou (`400`, `404`, `409`) | Nada é guardado: dá para corrigir e reenviar com a mesma chave |
| Sem o header | O `POST` funciona como sempre |

A chave vale por 24 horas (`scene.idempotency.ttl`). A chave e o registro criado são gravados na mesma transação, então não existe o caso de a chave ficar guardada sem o registro, nem o contrário.

### Chave de API (`X-API-Key`)

As rotas que alteram dados exigem uma chave de API no header `X-API-Key`: os `POST`, `PUT` e `DELETE` de artistas, perfis, álbuns, faixas e playlists. As consultas (`GET`) continuam abertas, e gerar uma chave também.

| Rota | O que faz | Exige chave? |
| :-- | :-- | :-- |
| `POST /api-keys` | Gera uma chave para o usuário informado em `owner`. A chave completa vem só nesta resposta | Não |
| `GET /api-keys/me` | Mostra a chave enviada no header: dono, prefixo, se está ativa e quando foi gerada | Sim |
| `DELETE /api-keys/me` | Revoga a chave enviada no header | Sim |

Resposta do `POST /api-keys`:

```json
{
  "id": 1,
  "owner": "Vinícius",
  "key": "scn_q8Xv2LmA9fKd0pRt7wYz3bNc5hJe1uSg",
  "prefix": "scn_q8Xv2LmA",
  "active": true,
  "createdAt": "2026-11-13T00:50:12",
  "_links": {
    "self": { "href": "http://localhost:8080/api-keys/me" },
    "delete": { "href": "http://localhost:8080/api-keys/me" }
  }
}
```

| Situação | Resposta |
| :-- | :-- |
| Rota protegida sem o header `X-API-Key` | `401 Unauthorized` |
| Chave que não existe | `401 Unauthorized` |
| Chave revogada | `401 Unauthorized` |
| Chave válida | A rota funciona normalmente |

Cada `401` traz uma mensagem própria e o header `WWW-Authenticate: ApiKey realm="Scene API", header="X-API-Key"`.

A chave é `scn_` seguido de 32 caracteres aleatórios (`SecureRandom`). O banco guarda só o hash SHA-256 dela e o começo (`prefix`), que serve para reconhecer a chave sem mostrar o valor. Por isso a chave completa aparece uma única vez, na resposta do `POST`. Se esse `POST` vier com `X-Idempotency-Key`, a resposta guardada para o replay também traz a chave, durante as 24 horas da idempotência.

Quem confere a chave é o `ApiKeyInterceptor`. Ele roda antes do controller nas rotas marcadas com `@RequiresApiKey`, então uma requisição sem chave válida não chega a validar o corpo nem a gravar nada.

No Swagger UI, gere a chave em `POST /api-keys`, clique em **Authorize** e cole a chave. As rotas com cadeado passam a enviar o header.

### Rate limiting (token bucket)

Cada IP tem um bucket com 30 tokens. Cada chamada a uma rota da API consome um token, e o bucket recebe 30 tokens por minuto, aos poucos (um a cada 2 s), até voltar a ficar cheio. Com isso o cliente pode fazer um burst de até 30 requisições seguidas, mas na média não passa de 30 por minuto. Swagger UI, Console do H2 e arquivos estáticos ficam fora do limite.

Toda resposta informa o saldo:

| Header | Significado |
| :-- | :-- |
| `X-RateLimit-Limit` | Capacidade do bucket |
| `X-RateLimit-Remaining` | Tokens que sobraram depois desta requisição |
| `X-RateLimit-Reset` | Segundos até o bucket ficar cheio de novo |
| `Retry-After` | Só no `429`: segundos até a próxima requisição ser aceita |

Com o bucket vazio:

```json
{
  "status": 429,
  "error": "Too Many Requests",
  "message": "Limite de requisições excedido. Tente novamente em 1 segundo(s).",
  "path": "/artists",
  "timestamp": "2026-11-11T10:15:30",
  "details": []
}
```

Para ver o `429` à mão, sem precisar de 30 requisições, suba a API com um bucket pequeno: troque no `application.properties` para `scene.rate-limit.capacity=5` e `scene.rate-limit.refill-tokens=5`. No sexto clique seguido no Swagger vem o `429`.

### CORS

Navegadores só deixam o JavaScript de uma página ler a resposta de outra origem (outro esquema, host ou porta) se o servidor autorizar. A API autoriza apenas as origens de `scene.cors.allowed-origins`:

| Origem | Para quê |
| :-- | :-- |
| `http://localhost:4200` | O front-end em Angular ([scene-web](https://github.com/Vikai23/scene-web)), rodando com `npm start` |

- **Métodos liberados:** `GET`, `POST`, `PUT`, `DELETE` e `OPTIONS` (o *preflight* que o navegador faz antes de certas requisições).
- **Headers que a página pode enviar:** `Content-Type`, `Accept`, `X-API-Version`, `X-Idempotency-Key` e `X-API-Key`.
- **Headers que a página pode ler:** `Location`, `Retry-After`, `X-API-Version`, `X-Idempotent-Replayed` e os três `X-RateLimit-*`.
- **Origem fora da lista:** `403 Forbidden`, no mesmo corpo de erro das outras respostas.
- **Sem o header `Origin`** (Postman, curl e o próprio Swagger UI, que tem a mesma origem da API) a checagem não acontece.

### Front-end em Angular

O front fica em outro repositório, o [scene-web](https://github.com/Vikai23/scene-web), feito em **Angular 21**. Ele roda em outra origem (`http://localhost:4200`) e consome a API pelo navegador, então só funciona porque o CORS libera esse endereço. O front usa as 43 rotas: dá para fazer nele tudo o que a API oferece, no mesmo visual do Swagger UI.

![Front-end em Angular consumindo a API a partir de outra origem](docs/pagina-demo.png)

Com a API no ar, rode dentro da pasta do scene-web:

```bash
npm install
npm start
```

e abra `http://localhost:4200`.

O que dá para fazer nele:

| Parte | O que faz |
| :-- | :-- |
| Topo | Mostra os tokens que restam no bucket (headers `X-RateLimit-*`) e, depois de um `429`, a contagem do `Retry-After` |
| Origens | A origem da página (header `Origin`), a da API e se o CORS liberou a ligação entre elas |
| Chave de API | Gera a chave (`POST /api-keys`), confere e revoga (`GET` e `DELETE /api-keys/me`). Com uma chave ativa, o front manda o `X-API-Key` em todo `POST`, `PUT` e `DELETE`; sem ela, essas ações voltam `401` |
| Abas Artistas, Perfis, Álbuns, Faixas e Playlists | Listagem paginada com busca, filtro (gênero, formato ou visibilidade), `sort` e `size`; os botões de página seguem os links HATEOAS |
| Detalhes | Os campos do item, os links HATEOAS (cada um abre o recurso ou a lista para onde aponta) e as ações `PUT` e `DELETE`, pelos links `update` e `delete` |
| Cadastro e edição | Formulários que mandam o JSON sem conferir nada antes: quem valida é a API, e o `400` aponta o erro embaixo de cada campo |
| Idempotência | Todo `POST` leva um `X-Idempotency-Key`; o botão *Repetir o mesmo POST com a mesma key* mostra a resposta original voltando com `X-Idempotent-Replayed: true`, sem criar nada |
| Relacionamentos | Colocar e tirar faixas de uma playlist (N:N), cadastrar um álbum a partir do artista e uma faixa a partir do álbum (1:N), e criar o perfil quando o link `profile` devolve `404` (1:1) |
| Versões | Nos detalhes de um artista, o botão *Comparar v1 e v2* busca o mesmo artista com `X-API-Version: 1` e `2`, mostra numa tabela o que muda de uma versão para a outra e coloca os dois JSON lado a lado |
| Status codes | 19 botões que provocam de propósito cada erro da API: `400` (6 casos), `401` (sem chave, chave inexistente e chave revogada), `403` do CORS (header e método não liberados), `404` (recurso e rota), `405`, `406`, `409`, `415`, `422` e `429` |
| Tráfego | Cada chamada feita, com os headers enviados e recebidos e o status. A chave de API aparece só pelo começo |

O front usa a versão padrão em todas as chamadas, menos na comparação. No Swagger UI, o seletor do topo troca a documentação inteira entre a `v1` e a `v2`.

Para ver o **bloqueio**, rode `npm run start:bloqueado` no scene-web. Ele sobe o mesmo front na porta 4300, que não está na lista: a página avisa "Bloqueado pelo CORS", e o Console do navegador (F12) mostra o erro completo.

## Regras de validação

As mesmas regras valem na entrada (DTOs `*Request`) e nas entidades (Bean Validation). Textos são aparados (`trim`) antes de validar.

<details>
<summary><b>Tabela completa de validações</b></summary>

| Entidade | Campo | Regra |
| :-- | :-- | :-- |
| `Artist` | `name` | obrigatório; 2 a 100 caracteres; ao menos uma letra; único (sem diferenciar maiúsculas de minúsculas) |
| `Artist` | `genre` | obrigatório; enum `Genre` |
| `Artist` | `country` | opcional; 2 a 60 caracteres; somente letras, espaços e `. , ' -` |
| `Artist` | `formedYear` | opcional; de 1000 a 2100 (cabem artistas históricos, como compositores clássicos) |
| `ArtistProfile` | `artistId` | obrigatório; o artista precisa existir e ainda não ter perfil (1:1); não muda depois de criado |
| `ArtistProfile` | `biography` | obrigatório; 10 a 1000 caracteres |
| `ArtistProfile` | `hometown` | opcional; 2 a 100 caracteres; somente letras, espaços e `. , ' -` |
| `ArtistProfile` | `website` | opcional; até 200 caracteres; começa com `http://` ou `https://` |
| `ArtistProfile` | `instagram` | opcional; até 30 caracteres entre letras, números, `.` e `_` (sem o `@`) |
| `Album` | `artistId` | obrigatório; o artista precisa existir |
| `Album` | `title` | obrigatório; 1 a 150 caracteres; títulos numéricos como `1989` valem; único dentro do artista |
| `Album` | `releaseYear` | obrigatório; de 1900 a 2100 |
| `Album` | `type` | obrigatório; enum `AlbumType` |
| `Album` | `label` | opcional; 2 a 100 caracteres; ao menos uma letra |
| `Track` | `albumId` | obrigatório; o álbum precisa existir |
| `Track` | `title` | obrigatório; 1 a 150 caracteres; títulos numéricos como `27` valem |
| `Track` | `trackNumber` | obrigatório; de 1 a 99; único dentro do álbum |
| `Track` | `durationSeconds` | opcional; de 1 a 7200 |
| `Playlist` | `name` | obrigatório; 2 a 100 caracteres; ao menos uma letra; único (sem diferenciar maiúsculas de minúsculas) |
| `Playlist` | `description` | opcional; até 255 caracteres |
| `Playlist` | `visibility` | obrigatório; enum `Visibility` |

</details>

## API Reference

São **43 endpoints**, em 6 grupos. A descrição completa de cada um, com exemplos de resposta de cada status, está no **Swagger UI**. Todas as listagens aceitam `page`, `size` e `sort` ([convenções](#paginação-e-ordenação)). Todo `POST`, `PUT` e `DELETE` (menos `POST /api-keys`) exige o header `X-API-Key` e devolve `401` sem uma chave válida ([chave de API](#chave-de-api-x-api-key)).

<details>
<summary><b>API Keys</b> — 3 endpoints · geração e gerenciamento das chaves de API</summary>

A chave vai no header `X-API-Key` das rotas que alteram dados. Gerar a chave é aberto.

#### Corpo da requisição (usado no `POST`)

| Campo | Tipo | Descrição |
| :-- | :-- | :-- |
| `owner` | `string` | **Obrigatório.** Nome de quem vai usar a chave. De 2 a 100 caracteres, com ao menos uma letra; aceita números e pontuação comum. |

```json
{
  "owner": "Vinícius"
}
```

---

```http
POST /api-keys
```
Gera uma chave. A resposta traz a chave completa no campo `key`, uma única vez. · `201` · `400`

```http
GET /api-keys/me
```
Dados da chave enviada no header `X-API-Key`: dono, prefixo, se está ativa e quando foi gerada. · `200` · `401`

```http
DELETE /api-keys/me
```
Revoga a chave enviada no header `X-API-Key`. · `204` · `401`

</details>

<details>
<summary><b>Artists</b> — 7 endpoints · cadastro de artistas e bandas</summary>

Um artista tem vários álbuns (1:N) e no máximo um perfil (1:1).

Campos de ordenação: `id`, `name`, `genre`, `country`, `formedYear`.

#### Corpo da requisição (usado no `POST` e no `PUT`)

| Campo | Tipo | Descrição |
| :-- | :-- | :-- |
| `name` | `string` | **Obrigatório.** Nome do artista ou banda (único, sem diferenciar maiúsculas de minúsculas). De 2 a 100 caracteres, com ao menos uma letra; aceita números e pontuação comum. |
| `genre` | `string` | **Obrigatório.** Gênero principal. Valores: `ROCK`, `ALTERNATIVE`, `PUNK`, `EMO`, `POST_HARDCORE`, `METALCORE`, `METAL`, `POP`, `HIP_HOP`, `ELECTRONIC`, `JAZZ`, `FOLK`, `CLASSICAL`, `OTHER` |
| `country` | `string` | Opcional. De 2 a 60 caracteres, somente letras, espaços e `. , ' -` (números não são aceitos). |
| `formedYear` | `integer` | Opcional. Ano de formação da banda ou, em artista solo, de início da carreira. De 1000 a 2100. |

```json
{
  "name": "Sleeping with Sirens",
  "genre": "POST_HARDCORE",
  "country": "United States",
  "formedYear": 2009
}
```

---

```http
GET /artists
```
Retorna uma página com os artistas cadastrados. · `200` · `400`

```http
GET /artists/search?name=${name}
```
Consulta personalizada: artistas cujo nome contém o texto informado, sem diferenciar maiúsculas de minúsculas (ex.: `name=bring` encontra "Bring Me the Horizon"). O parâmetro `name` é **obrigatório**, de 1 a 100 caracteres. · `200` · `400`

```http
GET /artists/by-genre/${genre}
```
Consulta personalizada: artistas do gênero informado (valores do enum `Genre`). · `200` · `400`

```http
GET /artists/${id}
```
Dados de um único artista, com links para álbuns e perfil. · `200` · `400` · `404`

```http
POST /artists
```
Cria um artista. · `201` · `400` · `409`

```http
PUT /artists/${id}
```
Substitui todos os dados de um artista existente; mesmas regras do cadastro. · `200` · `400` · `404` · `409`

```http
DELETE /artists/${id}
```
Exclui o artista junto com o perfil, os álbuns e as faixas dele. As faixas também saem de todas as playlists. · `204` · `400` · `404`

</details>

<details>
<summary><b>Artist Profiles</b> — 7 endpoints · perfil detalhado do artista (1:1)</summary>

Biografia, cidade natal, site e Instagram. Cada artista tem no máximo um perfil.

Campos de ordenação: `id`, `hometown`, `website`, `instagram`.

#### Corpo da requisição (usado no `POST` e no `PUT`)

| Campo | Tipo | Descrição |
| :-- | :-- | :-- |
| `artistId` | `integer` | **Obrigatório.** Id do artista dono do perfil. O artista precisa existir e ainda não ter perfil; depois de criado, o perfil não muda de artista. |
| `biography` | `string` | **Obrigatório.** De 10 a 1000 caracteres. |
| `hometown` | `string` | Opcional. De 2 a 100 caracteres, somente letras, espaços e `. , ' -`. |
| `website` | `string` | Opcional. Precisa começar com `http://` ou `https://`; até 200 caracteres. |
| `instagram` | `string` | Opcional. Usuário sem o `@`, até 30 caracteres: letras, números, ponto e underline. |

```json
{
  "artistId": 6,
  "biography": "Banda de Denton, Texas, formada em 2020, que já se chamou Sw!tch e Sinema.",
  "hometown": "Denton, Texas",
  "website": "https://www.example.com",
  "instagram": "iptw.example"
}
```

---

```http
GET /artist-profiles
```
Retorna uma página com os perfis cadastrados. · `200` · `400`

```http
GET /artist-profiles/search?hometown=${hometown}
```
Consulta personalizada: perfis cuja cidade natal contém o texto informado (ex.: `hometown=england` encontra "Sheffield, England" e "York, England"). O parâmetro é **obrigatório**, de 1 a 100 caracteres. · `200` · `400`

```http
GET /artist-profiles/by-artist/${artistId}
```
Consulta personalizada: o perfil do artista informado (relação 1:1). · `200` · `400` · `404`

```http
GET /artist-profiles/${id}
```
Dados de um único perfil. · `200` · `400` · `404`

```http
POST /artist-profiles
```
Cria o perfil de um artista que ainda não tem um. · `201` · `400` · `404` · `409`

```http
PUT /artist-profiles/${id}
```
Substitui os dados de um perfil existente. O `artistId` enviado precisa ser o mesmo do perfil. · `200` · `400` · `404`

```http
DELETE /artist-profiles/${id}
```
Exclui o perfil. O artista continua cadastrado e pode receber um novo perfil depois. · `204` · `400` · `404`

</details>

<details>
<summary><b>Albums</b> — 8 endpoints · álbuns, EPs, singles, coletâneas e shows ao vivo</summary>

Cada álbum pertence a um artista (N:1) e tem várias faixas (1:N).

Campos de ordenação: `id`, `title`, `releaseYear`, `type`, `label`.

#### Corpo da requisição (usado no `POST` e no `PUT`)

| Campo | Tipo | Descrição |
| :-- | :-- | :-- |
| `artistId` | `integer` | **Obrigatório.** Id do artista dono do álbum (o artista precisa existir). |
| `title` | `string` | **Obrigatório.** De 1 a 150 caracteres: letras, números, espaços e pontuação comum (títulos numéricos como "1989" são válidos). Não pode repetir dentro do mesmo artista. |
| `releaseYear` | `integer` | **Obrigatório.** De 1900 a 2100. |
| `type` | `string` | **Obrigatório.** Valores: `ALBUM`, `EP`, `SINGLE`, `COMPILATION`, `LIVE`. |
| `label` | `string` | Gravadora (opcional). De 2 a 100 caracteres, com ao menos uma letra. |

```json
{
  "artistId": 1,
  "title": "That's the Spirit",
  "releaseYear": 2015,
  "type": "ALBUM",
  "label": "RCA Records"
}
```

---

```http
GET /albums
```
Retorna uma página com os álbuns cadastrados. · `200` · `400`

```http
GET /albums/search?title=${title}
```
Consulta personalizada: álbuns cujo título contém o texto informado (ex.: `title=scream` encontra "Stand Up and Scream"). O parâmetro é **obrigatório**, de 1 a 150 caracteres. · `200` · `400`

```http
GET /albums/by-artist/${artistId}
```
Consulta personalizada: os álbuns do artista informado (relação 1:N). · `200` · `400` · `404`

```http
GET /albums/by-type/${type}
```
Consulta personalizada: os álbuns do formato informado (enum `AlbumType`). · `200` · `400`

```http
GET /albums/${id}
```
Dados de um único álbum, com links para o artista e as faixas. · `200` · `400` · `404`

```http
POST /albums
```
Cria um álbum para um artista existente. · `201` · `400` · `404` · `409`

```http
PUT /albums/${id}
```
Substitui todos os dados de um álbum existente, inclusive o artista dono. · `200` · `400` · `404` · `409`

```http
DELETE /albums/${id}
```
Exclui o álbum junto com as faixas dele. As faixas também saem de todas as playlists. · `204` · `400` · `404`

</details>

<details>
<summary><b>Tracks</b> — 8 endpoints · faixas dos álbuns</summary>

Cada faixa pertence a um álbum (N:1) e pode estar em várias playlists (N:N).

Campos de ordenação: `id`, `title`, `trackNumber`, `durationSeconds`.

#### Corpo da requisição (usado no `POST` e no `PUT`)

| Campo | Tipo | Descrição |
| :-- | :-- | :-- |
| `albumId` | `integer` | **Obrigatório.** Id do álbum da faixa (o álbum precisa existir). |
| `title` | `string` | **Obrigatório.** De 1 a 150 caracteres: letras, números, espaços e pontuação comum (títulos numéricos como "27" são válidos). |
| `trackNumber` | `integer` | **Obrigatório.** De 1 a 99. Não pode repetir dentro do mesmo álbum. |
| `durationSeconds` | `integer` | Opcional. De 1 a 7200. |

```json
{
  "albumId": 8,
  "title": "Intro",
  "trackNumber": 1,
  "durationSeconds": 95
}
```

---

```http
GET /tracks
```
Retorna uma página com as faixas cadastradas. · `200` · `400`

```http
GET /tracks/search?title=${title}
```
Consulta personalizada: faixas cujo título contém o texto informado (ex.: `title=heart` encontra "Can You Feel My Heart"). O parâmetro é **obrigatório**, de 1 a 150 caracteres. · `200` · `400`

```http
GET /tracks/by-album/${albumId}
```
Consulta personalizada: as faixas do álbum informado (relação 1:N). Use `sort=trackNumber,asc` para a ordem do disco. · `200` · `400` · `404`

```http
GET /tracks/${id}/playlists
```
Consulta personalizada: as playlists em que a faixa aparece (relação N:N). Devolve playlists, então ordena por `id`, `name`, `description` ou `visibility`. · `200` · `400` · `404`

```http
GET /tracks/${id}
```
Dados de uma única faixa, com links para o álbum e as playlists. · `200` · `400` · `404`

```http
POST /tracks
```
Cria uma faixa em um álbum existente. · `201` · `400` · `404` · `409`

```http
PUT /tracks/${id}
```
Substitui todos os dados de uma faixa existente, inclusive o álbum. · `200` · `400` · `404` · `409`

```http
DELETE /tracks/${id}
```
Exclui a faixa e a tira de todas as playlists em que estava. · `204` · `400` · `404`

</details>

<details>
<summary><b>Playlists</b> — 10 endpoints · playlists e as faixas de cada uma (N:N)</summary>

Uma playlist tem várias faixas e uma faixa pode estar em várias playlists.

Campos de ordenação: `id`, `name`, `description`, `visibility`.

#### Corpo da requisição (usado no `POST` e no `PUT`)

| Campo | Tipo | Descrição |
| :-- | :-- | :-- |
| `name` | `string` | **Obrigatório.** Nome da playlist (único, sem diferenciar maiúsculas de minúsculas). De 2 a 100 caracteres, com ao menos uma letra. |
| `description` | `string` | Opcional. Até 255 caracteres. |
| `visibility` | `string` | **Obrigatório.** Valores: `PUBLIC`, `PRIVATE`. |

```json
{
  "name": "Road Trip Mix",
  "description": "Músicas para viagens longas",
  "visibility": "PUBLIC"
}
```

---

```http
GET /playlists
```
Retorna uma página com as playlists cadastradas. · `200` · `400`

```http
GET /playlists/search?name=${name}
```
Consulta personalizada: playlists cujo nome contém o texto informado (ex.: `name=rotation` encontra "Heavy Rotation"). O parâmetro é **obrigatório**, de 1 a 100 caracteres. · `200` · `400`

```http
GET /playlists/by-visibility/${visibility}
```
Consulta personalizada: as playlists `PUBLIC` ou `PRIVATE`. · `200` · `400`

```http
GET /playlists/${id}/tracks
```
Consulta personalizada: as faixas que fazem parte da playlist (relação N:N). Devolve faixas, então ordena por `id`, `title`, `trackNumber` ou `durationSeconds`. · `200` · `400` · `404`

```http
GET /playlists/${id}
```
Dados de uma única playlist, com a quantidade de faixas e o link para elas. · `200` · `400` · `404`

```http
POST /playlists
```
Cria uma playlist vazia. Para colocar faixas nela, use `POST /playlists/{playlistId}/tracks/{trackId}`. · `201` · `400` · `409`

```http
PUT /playlists/${id}
```
Substitui nome, descrição e visibilidade de uma playlist existente. As faixas dela não mudam. · `200` · `400` · `404` · `409`

```http
DELETE /playlists/${id}
```
Exclui a playlist. As faixas continuam cadastradas; só deixam de fazer parte dela. · `204` · `400` · `404`

```http
POST /playlists/${playlistId}/tracks/${trackId}
```
Coloca uma faixa existente na playlist (relação N:N). Devolve a playlist atualizada; o header `Location` aponta para a lista de faixas dela. · `201` · `400` · `404` · `409`

```http
DELETE /playlists/${playlistId}/tracks/${trackId}
```
Tira a faixa da playlist. A faixa continua cadastrada e em outras playlists. · `204` · `400` · `404`

</details>

## Consultas no Console do H2

As tabelas guardam apenas a **chave estrangeira** (por exemplo, `tracks.album_id`), nunca uma cópia do nome do outro lado: assim o título de um álbum existe em um único lugar e não fica desatualizado. A API já devolve os nomes nas respostas (`albumTitle`, `artistName`); no banco, os nomes aparecem com um `JOIN`. A tabela `playlist_tracks` é só a tabela de ligação do N:N, com `playlist_id` e `track_id`.

> No banco, a gravadora está na coluna `record_label` (`@Column(name = "record_label")`), enquanto no JSON o campo se chama `label`.

<details>
<summary><b>Quatro consultas prontas</b></summary>

Faixas com o álbum e o artista:

```sql
SELECT t.id, t.track_number, t.title AS faixa, a.title AS album, ar.name AS artista
FROM tracks t
         JOIN albums a ON a.id = t.album_id
         JOIN artists ar ON ar.id = a.artist_id
ORDER BY a.id, t.track_number;
```

Playlists com o nome das faixas:

```sql
SELECT p.id AS playlist_id, p.name AS playlist, t.title AS faixa, a.title AS album, ar.name AS artista
FROM playlist_tracks pt
         JOIN playlists p ON p.id = pt.playlist_id
         JOIN tracks t ON t.id = pt.track_id
         JOIN albums a ON a.id = t.album_id
         JOIN artists ar ON ar.id = a.artist_id
ORDER BY p.id, t.id;
```

Perfis com o nome do artista, e artistas que ainda não têm perfil (a relação 1:1 é opcional):

```sql
SELECT ar.name AS artista, pr.hometown, pr.website, pr.instagram
FROM artist_profiles pr
         JOIN artists ar ON ar.id = pr.artist_id;

SELECT ar.id, ar.name
FROM artists ar
         LEFT JOIN artist_profiles pr ON pr.artist_id = ar.id
WHERE pr.id IS NULL;
```

Álbuns com a gravadora e a quantidade de faixas de cada um:

```sql
SELECT a.id, a.title AS album, a.release_year AS ano, COALESCE(a.record_label, '(sem gravadora)') AS gravadora,
       COUNT(t.id) AS faixas
FROM albums a
         LEFT JOIN tracks t ON t.album_id = a.id
GROUP BY a.id, a.title, a.release_year, a.record_label
ORDER BY a.id;
```

</details>

## Estrutura do projeto

```text
src/main/java/com/vinicius/scene_api
├── SceneApiApplication.java
├── apikey/       chave de API: entidade (só o hash), geração, @RequiresApiKey e o interceptor que devolve 401
├── assembler/    monta os links HATEOAS de cada recurso
├── config/       carga inicial (DataLoader), OpenAPI/Swagger, versionamento, CORS, JSON estrito e interceptors
├── controller/   endpoints REST e a documentação de cada um (@Operation, @ApiResponse)
├── dto/          *Request (entrada validada), *Response (saída) e exemplos de erro
├── exception/    exceções de negócio, corpo padrão de erro (ApiError) e GlobalExceptionHandler
├── idempotency/  chave de idempotência dos POST (tabela idempotency_keys)
├── model/        entidades JPA e enums
├── ratelimit/    token bucket por IP e o interceptor que aplica o limite
├── repository/   Spring Data JPA (consultas derivadas e paginadas)
├── service/      regras de negócio e transações
└── validation/   expressões regulares, mensagens e campos de ordenação aceitos

src/main/resources
├── application.properties
└── static/       tema do Swagger UI (CSS, fontes e imagens) e a página de termos de uso

src/test/java     testes do token bucket e testes de integração (versionamento, idempotência, chave de API, CORS, rate limiting, erros e documentação)

postman/          coleção do Postman
```

## Licença

Distribuído sob a licença Apache 2.0 — veja o arquivo [LICENSE](LICENSE).

## Autor

**Vinícius Alves e Sousa** — Senac TSI, projeto final da disciplina de Web Services.

[GitHub](https://github.com/Vikai23) · [LinkedIn](https://www.linkedin.com/in/vinicius-alves-e-sousa/)

![xXx thanks for the add xXx — you are visitor #000666](docs/swagger-rodape.png)
