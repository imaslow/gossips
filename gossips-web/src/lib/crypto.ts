/**
 * E2E-криптография на WebCrypto.
 *
 * Схема: identity-пара ECDH P-256 (приватный ключ неизвлекаем и живёт только в IndexedDB),
 * публичный публикуется на сервер. Каждое сообщение шифруется свежей эфемерной парой:
 *   keyPeer  = ECDH(ephPriv, pub собеседника)
 *   keySelf  = ECDH(ephPriv, свой pub)          — чтобы автор мог читать историю после перезахода
 * Текст шифруется AES-GCM на обоих ключах; в content уходит base64(JSON) по спеке app-api.yml.
 *
 * Групповые сообщения (v=2) — один сеансовый ключ на сообщение, зашитый на ключ
 * каждого участника (см. encryptGroupMessage): сервер хранит одну строку,
 * читать её может любой участник своей копией.
 *
 * Forward secrecy: эфемерная пара одноразовая, компрометация identity-ключа
 * не раскрывает уже отправленные сообщения.
 */

const IDB_NAME = "gossips-crypto";
const IDB_STORE = "identity";
const IDB_KEY_ID = "ecdh-p256";
const ECDH_PARAMS: EcKeyImportParams = { name: "ECDH", namedCurve: "P-256" };
const PAYLOAD_VERSION = 1;

export interface IdentityKeyPair {
  publicKey: CryptoKey;
  privateKey: CryptoKey;
}

interface MessagePayload {
  v: number;
  /** Эфемерный публичный ключ (base64 raw) — собеседник строит из него общий секрет. */
  eph: string;
  /** Копия для собеседника. */
  toPeer: { iv: string; ct: string };
  /** Копия для автора (читаемая после перезахода). */
  toSelf: { iv: string; ct: string };
}

/**
 * Формат группового сообщения (v=2): тело шифруется ОДИН раз на случайный
 * сеансовый ключ MK, а MK зашивается отдельно на публичный ключ КАЖДОГО
 * участника (включая автора — чтобы читать историю с другого устройства).
 * ECDH(ephPriv, pub участника) == ECDH(priv участника, ephPub) — тот же
 * трюк, что и в pair-формате.
 */
interface GroupPayload {
  v: number;
  eph: string;
  iv: string;
  ct: string;
  /** username -> зашитый сеансовый ключ {iv, ct}. */
  keys: Record<string, { iv: string; ct: string }>;
}

/** Контекст расшифровки: isOwn — сообщение текущего пользователя. */
export interface DecryptContext {
  isOwn: boolean;
  /** Логин получателя (для группового формата — ключ его копии в keys). */
  username: string;
}

// ---------- IndexedDB ----------

function openDb(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(IDB_NAME, 1);
    request.onupgradeneeded = () => request.result.createObjectStore(IDB_STORE);
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

async function loadIdentity(): Promise<IdentityKeyPair | null> {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(IDB_STORE, "readonly");
    const request = tx.objectStore(IDB_STORE).get(IDB_KEY_ID);
    request.onsuccess = () => resolve((request.result as IdentityKeyPair) ?? null);
    request.onerror = () => reject(request.error);
  });
}

async function storeIdentity(pair: IdentityKeyPair): Promise<void> {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(IDB_STORE, "readwrite");
    tx.objectStore(IDB_STORE).put(pair, IDB_KEY_ID);
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

// ---------- Identity ----------

/**
 * Single-flight на загрузку/генерацию пары: параллельные вызовы (например,
 * двойной mount эффекта в StrictMode) должны получить ОДНУ пару. Иначе гонка
 * «прочитали-пусто → сгенерировали свою» даст две пары: на сервер уйдёт одна,
 * в IndexedDB останется другая — и расшифровка рассыплется.
 */
let identityPromise: Promise<IdentityKeyPair> | null = null;

/**
 * Возвращает identity-пару текущего пользователя: загружает из IndexedDB
 * или генерирует новую (приватный ключ неизвлекаем — export невозможен даже у нас).
 */
export function ensureIdentity(): Promise<IdentityKeyPair> {
  identityPromise ??= loadIdentity().then(async (stored) => {
    if (stored) {
      return stored;
    }
    const generated = await crypto.subtle.generateKey(ECDH_PARAMS, false, ["deriveBits"]);
    const pair: IdentityKeyPair = { publicKey: generated.publicKey, privateKey: generated.privateKey };
    await storeIdentity(pair);
    return pair;
  });
  // при ошибке сбрасываем кэш, чтобы следующий вызов попробовал заново
  identityPromise.catch(() => {
    identityPromise = null;
  });
  return identityPromise;
}

/** Публичный ключ в base64 (raw, 65 байт) — то, что публикуется на сервер. */
export async function exportPublicKeyBase64(pair: IdentityKeyPair): Promise<string> {
  const raw = await crypto.subtle.exportKey("raw", pair.publicKey);
  return toBase64(raw);
}

/** Отпечаток публичного ключа: base64(SHA-256(raw)) — safety number для верификации. */
export async function fingerprintOf(pair: IdentityKeyPair): Promise<string> {
  const raw = await crypto.subtle.exportKey("raw", pair.publicKey);
  const digest = await crypto.subtle.digest("SHA-256", raw);
  return toBase64(digest);
}

/** Отпечаток для сверки по телефону: группы по 6 символов (как safety number в Signal). */
export function formatFingerprint(fingerprint: string): string {
  return fingerprint.replace(/(.{6})/g, "$1 ").trim();
}

// ---------- Сообщения ----------

/**
 * Шифрует plaintext для собеседника с опубликованным публичным ключом.
 * Возвращает base64(JSON) — значение для content с encrypted=true.
 */
export async function encryptMessage(
  myPair: IdentityKeyPair,
  peerPublicKeyBase64: string,
  plaintext: string,
): Promise<string> {
  const peerPublic = await importPublicKey(peerPublicKeyBase64);
  const ephemeral = await crypto.subtle.generateKey(ECDH_PARAMS, true, ["deriveBits"]);
  const ephPublicRaw = await crypto.subtle.exportKey("raw", ephemeral.publicKey);

  const keyPeer = await deriveAesKey(ephemeral.privateKey, peerPublic);
  const keySelf = await deriveAesKey(ephemeral.privateKey, myPair.publicKey);

  const payload: MessagePayload = {
    v: PAYLOAD_VERSION,
    eph: toBase64(ephPublicRaw),
    toPeer: await seal(keyPeer, plaintext),
    toSelf: await seal(keySelf, plaintext),
  };
  return toBase64(new TextEncoder().encode(JSON.stringify(payload)));
}

/**
 * Шифрует plaintext для группы: члены с опубликованными публичными ключами
 * (автор обязан быть в списке — иначе он не прочитает свою же историю).
 * Возвращает base64(JSON) — значение для content с encrypted=true.
 */
export async function encryptGroupMessage(
  members: { username: string; publicKey: string }[],
  plaintext: string,
): Promise<string> {
  const ephemeral = await crypto.subtle.generateKey(ECDH_PARAMS, true, ["deriveBits"]);
  const ephPublicRaw = await crypto.subtle.exportKey("raw", ephemeral.publicKey);

  // Сеансовый ключ: телеграмма одна на всех, расходы на N шифрований — только на обёртки
  const messageKeyBytes = crypto.getRandomValues(new Uint8Array(32));
  const messageKey = await crypto.subtle.importKey("raw", messageKeyBytes, { name: "AES-GCM" }, false, [
    "encrypt",
    "decrypt",
  ]);

  const iv = crypto.getRandomValues(new Uint8Array(12));
  const ct = await crypto.subtle.encrypt(
    { name: "AES-GCM", iv },
    messageKey,
    new TextEncoder().encode(plaintext),
  );

  const keys: GroupPayload["keys"] = {};
  for (const member of members) {
    const memberPublic = await importPublicKey(member.publicKey);
    const wrapKey = await deriveAesKey(ephemeral.privateKey, memberPublic);
    keys[member.username] = await seal(wrapKey, toBase64(messageKeyBytes));
  }

  const payload: GroupPayload = {
    v: 2,
    eph: toBase64(ephPublicRaw),
    iv: toBase64(iv),
    ct: toBase64(ct),
    keys,
  };
  return toBase64(new TextEncoder().encode(JSON.stringify(payload)));
}

/**
 * Расшифровывает content сообщения. Pair-формат: isOwn выбирает toSelf/toPeer.
 * Групповой (v=2): из keys берётся копия для username получателя, сеансовый ключ
 * распаковывается ECDH(мой приватный, ephPub), им расшифровывается тело.
 */
export async function decryptMessage(
  myPair: IdentityKeyPair,
  contentBase64: string,
  context: DecryptContext,
): Promise<string> {
  const parsed = JSON.parse(
    new TextDecoder().decode(fromBase64(contentBase64)),
  ) as MessagePayload & GroupPayload;
  const ephPublic = await importPublicKey(parsed.eph);
  // Для обеих схем общий секрет один: ECDH(ephPriv, мой публичный) == ECDH(мой приватный, ephPub)
  const key = await deriveAesKey(myPair.privateKey, ephPublic);

  if (parsed.v === 2) {
    const box = parsed.keys?.[context.username];
    if (!box) {
      throw new Error("Нет зашитой копии ключа для этого участника");
    }
    const wrappedKeyBase64 = await open(key, box);
    const messageKey = await crypto.subtle.importKey(
      "raw",
      fromBase64(wrappedKeyBase64),
      { name: "AES-GCM" },
      false,
      ["encrypt", "decrypt"],
    );
    const plain = await crypto.subtle.decrypt(
      { name: "AES-GCM", iv: fromBase64(parsed.iv) },
      messageKey,
      fromBase64(parsed.ct),
    );
    return new TextDecoder().decode(plain);
  }

  if (parsed.v !== PAYLOAD_VERSION) {
    throw new Error(`Неизвестная версия шифросообщения: ${parsed.v}`);
  }
  const box = context.isOwn ? parsed.toSelf : parsed.toPeer;
  return open(key, box);
}

async function seal(key: CryptoKey, plaintext: string): Promise<{ iv: string; ct: string }> {
  const iv = crypto.getRandomValues(new Uint8Array(12));
  const ct = await crypto.subtle.encrypt(
    { name: "AES-GCM", iv },
    key,
    new TextEncoder().encode(plaintext),
  );
  return { iv: toBase64(iv), ct: toBase64(ct) };
}

async function open(key: CryptoKey, box: { iv: string; ct: string }): Promise<string> {
  const plain = await crypto.subtle.decrypt(
    { name: "AES-GCM", iv: fromBase64(box.iv) },
    key,
    fromBase64(box.ct),
  );
  return new TextDecoder().decode(plain);
}

async function importPublicKey(base64: string): Promise<CryptoKey> {
  return crypto.subtle.importKey("raw", fromBase64(base64), ECDH_PARAMS, false, []);
}

async function deriveAesKey(privateKey: CryptoKey, publicKey: CryptoKey): Promise<CryptoKey> {
  const shared = await crypto.subtle.deriveBits(
    { name: "ECDH", public: publicKey },
    privateKey,
    256,
  );
  return crypto.subtle.importKey("raw", shared, { name: "AES-GCM" }, false, [
    "encrypt",
    "decrypt",
  ]);
}

// ---------- base64 ----------

function toBase64(buffer: ArrayBuffer | Uint8Array): string {
  const bytes = buffer instanceof Uint8Array ? buffer : new Uint8Array(buffer);
  let binary = "";
  for (const byte of bytes) {
    binary += String.fromCharCode(byte);
  }
  return btoa(binary);
}

function fromBase64(base64: string): Uint8Array<ArrayBuffer> {
  const binary = atob(base64);
  const bytes = new Uint8Array(new ArrayBuffer(binary.length));
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes;
}
