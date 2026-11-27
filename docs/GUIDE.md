# Gossips — как работает система

Каркас мессенджера: микросервисы на Java 21 + Spring Boot 4.1, PostgreSQL (jOOQ, Flyway/Liquibase), API-first через OpenAPI-спеки. Этот документ — карта всего: архитектура, потоки данных, соглашения, запуск.

## 1. Модули

```
gossips/                        (root pom, общие настройки сборки)
├── src/main/resources/openapi/ # ЕДИНЫЕ спеки: app-api.yml, gateway-api.yml
├── common/                     # то, что разделяют сервисы
│   └── dto/                    #   сгенерированные модели (только сюда!)
│   └── security/             #   JwtService, JwtProperties (валидация JWT локально)
│   └── exception/            #   ErrorCode, ErrorResponse
│   └── ApiPaths.java         #   все пути API — константы вместо строк
├── gossips-security/   :8081   # аутентификация: регистрация, логин, токены
├── gossips-app/        :8082   # вся бизнес-логика мессенджера + WebSocket /ws
└── gossips-gateway/    :8080   # единая точка входа для клиентов, прокси + проверка JWT
```

Три Spring-приложения + один jar common. Клиент ходит **только на gateway (8080)**; единственное исключение — WebSocket (сейчас напрямую на app:8082, см. §5).

## 2. Ключевые принципы

### 2.1 API-first: спека → код
- Все эндпоинты описаны в `src/main/resources/openapi/*.yml`.
- Плагин openapi-generator на этапе сборки генерирует **интерфейсы** (`MessagesApi`, `ConversationsApi`, `GatewayAuthApi`, ...) в пакет `*.api` модуля.
- Контроллеры только `implements СгенерированныйApi` + `@Override` — руками маршруты и валидацию не пишем, валидация (`@Min/@Max/minLength`) приезжает из спеки.
- DTO-модели генерируются **только в common** (`org.your.gossips.common.dto`) — один источник правды на все сервисы. Дубли схем в gateway-спеке игнорируются генератором.

### 2.2 Доверие между сервисами: X-Username
- Gateway проверяет JWT **локально** (тот же секрет, что в security; `JwtService` из common) и пересылает downstream заголовком `X-Username: <владелец токена>`.
- Внутренние сервисы (app, security) **доверяют** `X-Username` — на этот заголовок опираются все проверки доступа.
- Клиентский `username` в телах запросов игнорируется/перезаписывается — автор всегда владелец токена.

### 2.3 Слои внутри сервиса (на примере gossips-app)
```
adapter/in/            контроллеры — тонкие: вызов сервиса + ResponseEntity
service/               интерфейсы бизнес-логики (+ javadoc)
service/impl/          @Service: проверки, нормализация, маппинг, push-уведомления
adapter/out/
├── model/             record-зеркала таблиц (StoredMessage и т.п.)
├── page/              пагинация (MessagePage)
├── persistence/       порты (MessagePersistence, ...) + impl/ (репозитории jOOQ)
└── realtime/          WebSocket: хендлер, реестр сессий, push-уведомления
mapper/                MapStruct-модели common.dto <-> record'ы (на уровне модуля)
config/                @Configuration: WebSocket, хендлеры ошибок, @ConfigurationProperties
exception/             доменные исключения (AccessDeniedException, KeyNotFoundException)
```
Контроллер не знает ни о БД, ни о записях — только DTO из common и сервисные интерфейсы.

### 2.4 Ошибки
- Канонические коды — `common.exception.ErrorCode` (UNAUTHORIZED, FORBIDDEN, NOT_FOUND, VALIDATION_FAILED, USER_ALREADY_EXISTS, INVALID_CREDENTIALS, INVALID_REFRESH_TOKEN, SERVICE_UNAVAILABLE, UPSTREAM_ERROR).
- Тело всегда `{"code": "...", "message": "..."}`.
- app: `GlobalExceptionHandler` маппит доменные исключения (AccessDenied→403, KeyNotFound→404, IllegalArgument/ConstraintViolation→400).
- gateway прокидывает 4xx downstream телом как есть; только сбой сети → 503 `SERVICE_UNAVAILABLE`.

### 2.5 Пагинация
- Курсорная: `beforeId` + `limit` (default/max — из yml, `gossips.pagination`).
- Нормализация — статический `PaginationUtils.normalizeLimit(limit, default, max)`.
- Курсор — позиция `(created_at, id)` с двойным ключом (снимает коллизии timestamp'ов); в ответе `nextBeforeId`, null = история кончилась.

### 2.6 Сквозной идентификатор запроса (X-Request-Id)

`RequestIdFilter` (common, `logging/`) читает заголовок `X-Request-Id` или генерирует UUID,
кладёт в MDC (`requestId`) и возвращает в ответе; паттерн логов всех сервисов
(`logging.pattern.level: "%5p [requestId=%X{requestId}]"`) печатает его в каждой строке.
Gateway дополнительно прокидывает заголовок в security/app через интерсептор
`RestClientConfig` — цепочка «gateway → внутренний сервис» связывается одним id в логах.
Фильтр регистрируется `LoggingConfig` и подключается `@Import` в каждом сервисе
(common не входит в component-scan).

### 2.7 Наблюдаемость

Actuator у всех сервисов: наружу открыт только `/actuator/health`,
включены probes (`/actuator/health/liveness`, `/actuator/health/readiness`) —
compose-файл гоняет их в healthcheck'ах, оттуда же возьмёт k8s, если понадобится.

### 2.8 Данные: две БД, две миграционные системы
| БД | Сервис | Миграции | Таблицы |
|---|---|---|---|
| `gossips` | security | **Liquibase** (xml-чейнджсеты + sql-скрипты) | users, refresh_tokens |
| `gossips_app` | app | **Flyway** (`db/migration/V*.sql`) | messages, profiles, conversations, conversation_members, contacts, user_keys, key_verifications |

Две БД потому, что два Flyway на одной схеме конфликтуют за `flyway_schema_history`; init-скрипт `docker/init-databases.sh` создаёт обе. jOOQ-код в app генерируется из Flyway-миграций офлайн (DDLDatabase, H2) — живая БД на сборку не нужна.

## 3. Потоки

### 3.1 Регистрация и логин (security, БД gossips)
```
POST :8080/api/auth/register {username, password}
  → gateway → security: bcrypt-хэш → users → 200 {token, refreshToken, username}

POST :8080/api/auth/login → проверка хэша → 200 {token (JWT 15м), refreshToken (опак, 7д)}
POST :8080/api/auth/refresh {refreshToken} → ротация пары (старый revoke)
POST :8080/api/auth/logout {refreshToken} → revoke → 204
```
JWT HS256, subject = username; refresh-токены хранятся хэшами в `refresh_tokens` (отзыв возможен). Время жизни токенов — в yml security (`gossips.security.jwt`).

### 3.2 Защищённый запрос (любой бизнес-эндпоинт)
```
client                    gateway                        app
  |-- Authorization: JWT ->|                            |
  |                        | isTokenValid? нет -> 401  |
  |                        | extractUsername(token)      |
  |                        |-- X-Username: ilya ------->|
  |                        |                            | бизнес-логика, X-Username — автор
  |<--- 200 / 4xx телом ---|<--- ErrorResponse ----------|
```
JWT никогда не уходит за gateway — внутри кластера доверие по `X-Username`.

### 3.3 Сообщение: отправка → доставка → прочтение
```
1. POST /api/conversations/{id}/messages {content, encrypted?}
   → gateway (владелец токена = автор) → app
2. app: requireMember(403) → INSERT messages (conversation_id NOT NULL с V5)
   → push: WebSocketPushNotifier рассылает всем участникам, кроме автора
3. GET /api/conversations/{id}/messages?beforeId&limit — история, курсорно
4. POST /api/conversations/{id}/read — помечает чужие непрочитанные read_at=now
```
Список диалогов (`GET /api/conversations`) — это «главная страница»: type, title (для групп), peer/displayName (только для pair), **lastMessage/lastMessageAt, unreadCount**, сортировка по активности. Пустых «глобальных» чатов нет — каждое сообщение принадлежит диалогу (V5, NOT NULL).

Групповые беседы (V6: `conversations.type` pair/group, `title`, `created_by`; участники — те же `conversation_members`):
- `POST /api/conversations/group` {usernames ≥ 2 (без создателя), title} → создатель добавляется первым, всем остальным летит CONVERSATION_UPDATED.
- `GET /api/conversations/{id}/members` → участники с displayName (нужны клиенту для шифрования на каждый ключ).
- unread/read/typing/realtime работают для N участников без изменений — рассылка всегда шла по `conversation_members`.
- findByPair фильтрует `type='pair'`, поэтому группа, где состоят оба пользователя, pair-диалог не перехватывает.

### 3.4 Realtime (WebSocket, app:8082)
```
ws://app:8082/ws?token=<JWT>
  → isTokenValid? нет -> close 4401
  → да: username → WebSocketSessionRegistry (username → множество сессий/устройств)
push: {"type":"NEW_MESSAGE","conversationId":"...","message":{...}}
push: {"type":"CONVERSATION_UPDATED","conversationId":"..."}   // собеседник создал диалог
push: {"type":"TYPING","conversationId":"...","username":"..."} // эфемерно, ничего не хранится
push: {"type":"MESSAGES_READ","conversationId":"...","username":"..."} // участник прочитал диалог
```

Typing: `POST /api/conversations/{id}/typing` (204) → broadcast остальным участникам как TYPING.
Ничего не персистится; клиент шлёт с throttle (3 с) и показывает «печатает…» 3,5 с.

MESSAGES_READ: `ConversationService.markRead` после апдейта статусов рассылает остальным
участникам — клиент ставит ✓✓ своим сообщениям живьём, без перезахода в диалог.
Канал односторонний (server→client): отправка сообщений — по HTTP (та же валидация), WS — чисто доставка. Мёртвые сессии вычищаются при отправке.

Клиент (`gossips-web/src/lib/realtime.ts`): reconnect с нарастающей паузой (1→13 с),
при reconnect токен заново берётся из `getValidAccessToken` (молчаливая ротация
refresh через `/api/auth/refresh`, single-flight) — протухший access не рвёт канал.
`NEW_MESSAGE` для активного чата — расшифровка и markRead на лету, для неактивного —
бейдж unread + превью (для E2E-сообщений — заглушка «🔒», флаг `lastMessageEncrypted`
в сводке). `CONVERSATION_UPDATED` — полный рефетч сводок. Пуш уходит всем участникам,
кроме автора (у автора сообщение уже на экране локальным эхом).

### 3.5 Профили и контакты
- `GET/PUT /api/profiles/me` — displayName (по умолчанию = логин), bio. displayName подставляется в списки диалогов/контактов (LEFT JOIN + coalesce к логину).
- `GET/POST /api/contacts`, `DELETE /api/contacts/{username}` — ростер, идемпотентно.

Клиент: вкладки «Чаты»/«Контакты» в сайдбаре, профиль — клик по своей плашке внизу.
Верификация ключей — иконка в шапке чата: диалог сверки отпечатков (группы по 6),
`PUT/DELETE /api/keys/{username}/verify`; verified привязан к fingerprint (rotation ключа сбрасывает).

Группы на клиенте: диалог «+» с переключателем Диалог/Группа (название + мультивыбор из
контактов, минимум 2). Шапка группы — название и подзаголовок со списком участников;
чужие сообщения подписаны автором (displayName из /members). Цветные аватарки —
детерминированный HSL из хэша имени (`lib/avatar.ts`).

### 3.6 E2E-инфраструктура
```
PUT  /api/keys/me                      публикация публичного ключа {publicKey, fingerprint}
GET  /api/keys/{username}              ключ собеседника + verified (верифицировал ли caller ЭТОТ отпечаток)
PUT  /api/keys/{username}/verify       safety number сверен → сохранить {fingerprint}
DELETE /api/keys/{username}/verify     снять верификацию (идемпотентно)
```
- Приватные ключи на сервер не попадают никогда.
- Сообщение с `encrypted: true` — `content` это base64-ciphertext: сервер хранит/пересылает, не видя plaintext.
- Верификация привязана к отпечатку: rotation ключа собеседником делает старую верификацию недействительной.
- Криптография живёт на клиенте (`gossips-web/src/lib/crypto.ts`): identity-пара ECDH P-256
  (приватный ключ неизвлекаем, IndexedDB), сообщение — AES-GCM с эфемерной парой
  (forward secrecy), две копии шифротекста: `toPeer` (для собеседника) и `toSelf`
  (автор читает историю после перезахода). Safety number — fingerprint публичного ключа.
- Форматы шифроконтейнера (в content при `encrypted: true`, base64(JSON)):
  - **v=1 (pair)**: `{v, eph, toPeer:{iv,ct}, toSelf:{iv,ct}}` — получатель выбирает копию
    по тому, своё сообщение или нет.
  - **v=2 (group)**: `{v, eph, iv, ct, keys: {username: {iv, ct}}}` — тело зашифровано ОДИН раз
    на случайный сеансовый ключ MK (32 байта, AES-GCM); MK зашит отдельно на публичный ключ
    каждого участника (включая автора) через тот же ECDH с эфемерной парой. Клиент при открытии
    группы тянет `/members` + `/api/keys/{username}` каждого: если у кого-то нет ключа —
    отправка заблокирована, показан список таких участников. Расшифровка: `keys[мой логин]`
    → MK → тело.

## 4. Конфигурация

| Где | Что |
|---|---|
| security `application.yml` | БД, `gossips.security.jwt` (secret, access 15m, refresh 7d) |
| app `application.yml` | БД, `gossips.pagination`, `gossips.security.jwt` (тот же secret — WS-валидация) |
| gateway `application.yml` | `gossips.clients.security/app` (адреса downstream), `gossips.security.jwt` |
| env | `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `GOSSIPS_CLIENTS_*` |

Secret: дефолт в yml — только для локалки, везде выше — `JWT_SECRET` env (в compose уже проброшен).

## 5. Запуск

```bash
# Всё целиком (postgres + 3 сервиса), билды через единый Dockerfile (ARG MODULE):
docker compose up --build

# Локальная разработка: контейнер postgres (5434) + сервисы из IDE/jar:
docker start gossips-postgres
DB_URL=jdbc:postgresql://localhost:5434/gossips      java -jar gossips-security/target/gossips-security-1.0.0.jar
DB_URL=jdbc:postgresql://localhost:5434/gossips_app  java -jar gossips-app/target/gossips-app-1.0.0.jar
java -jar gossips-gateway/target/gossips-gateway-1.0.0.jar

./mvnw clean install    # сборка + тесты всех модулей

# Здоровье сервисов (compose ждёт эти же эндпоинты в healthcheck'ах):
curl localhost:8080/actuator/health   # gateway
curl localhost:8081/actuator/health   # security
curl localhost:8082/actuator/health   # app
```

Клиенту нужны: gateway :8080 (HTTP) и app :8082 (WebSocket). Порты в компоузе проброшены как у локального контейнера разработки.

## 6. Соглашения (чек-лист перед новой фичей)

1. Эндпоинт сначала в спеке (app-api.yml и/или gateway-api.yml), операции — operationId.
2. Путь — константа в `ApiPaths` (вложенный класс на домен).
3. Контроллер: `implements XxxApi`, тело ≤ вызова сервиса.
4. Бизнес-логика — в `service/` (интерфейс) + `service/impl`. Проверки доступа, нормализация, маппинг — там.
5. База — порт в `adapter/out/persistence/`, реализация в `impl/`: `newRecord`/insert с `returning()`, чтение — `fetchOptionalInto/fetchInto` в record-зеркала; алиасы колонок для маппинга.
6. Новые таблицы — Flyway `V<N>__*.sql` в app / Liquibase xml в security.
7. ДТО-модели не писать руками — они генерируются в common из спеки.
8. Мапперы — MapStruct (`@Mapper(componentModel = "spring")`), сложные поля — `@Mapping(expression=...)`; мапперы лежат в `mapper/` на уровне модуля.
9. Тесты зелёные (`./mvnw clean install`), фича проверена живьём на поднятом стеке.

### 6.1 Тесты

- `*Tests` в `src/test/.../it/` — интеграционные на Testcontainers: `PostgresIT` поднимает
  `postgres:17-alpine` в Docker (singleton на JVM, параметры подключения —
  через `@DynamicPropertySource`), Flyway/Liquibase накатывают миграции на чистую БД.
  Классы с суффиксом `IT` surefire не подхватывает — именуем `*Tests`.
- Unit-заглушки без БД (`GossipsAppApplicationTests` и т.п.) — контекст с исключённым
  DataSource + `@MockitoBean` на `DSLContext`; живую БД они не трогают.
- Тесты всех классов делят одну БД на JVM — у каждого теста свои уникальные юзернеймы.
- CI: `.github/workflows/ci.yml` гоняет `./mvnw clean install` на каждый push/PR
  (на runner'ах GitHub Actions Docker есть, Testcontainers работает из коробки).

## 7. Что дальше (горизонт)

- Клиент с настоящей E2E-криптографией (генерация ключей, ECDH, шифрование, safety number — API готов)
- WS за gateway / брокер сообщений при горизонтальном масштабировании app
- Групповые беседы (members > 2), аватарки/файлы (отдельный сервис), поиск пользователей, push
