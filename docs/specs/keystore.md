# Subsystem: `keystore`

`denarii-keystore` (`rust/crates/keystore`) is how Rust reaches the Android Keystore keys that v1
left on phones. Only code in the app's Java runtime can use those keys, so a Kotlin object,
`VaultKeystore`, runs each operation, and this crate calls it through the Java Native Interface
(JNI). Its safe API, `Keystore`, uses v1's aliases and parameters, never replaces a key that a
stored profile needs, and wipes every Java array it passes or receives. ADR-0007 (hardware-key
operations stay in platform code, which Rust calls directly) and ADR-0022 (`jni-sys` in a crate of
its own, with a test that reaches it from Dart) shaped it. Built in M3.2 as described here; the
API reference is `cargo doc -p denarii-keystore --open`, run from `rust/`. The vault (M3.3) is its
first caller.

## Why this design

The Android Keystore holds keys that never leave the phone (`docs/specs/android-v1-formats.md`,
section 2). v1 mixed every PIN and answer guess with one of them, the device key, so a guess can
be checked only on that phone (CS-15), and it sealed the profile under another, the storage key.
The new app must open both as v1 left them (ADR-0002), and only the app's Java runtime can use
them.

ADR-0007 keeps the vault's rules in Rust and leaves only the operations that need a key in
Kotlin. Rust calls Kotlin through JNI, so no stretched secret or key passes through Dart, as it
would if Dart called Kotlin for each operation. The bridge can't hold this code: the vault calls
it, and no crate may depend on the bridge.

What it trades away:

- **Hand-written `unsafe` code.** `src/jni.rs` has 742 lines with 49 `unsafe` blocks, the only
  `unsafe` code in the workspace that a person wrote. Each block holds one operation and has a
  `// SAFETY:` comment, which two Clippy lints check.
- **Code the host can't run.** `src/jni.rs` compiles only for Android, so `cargo test` and the
  coverage figures never see it. CI lints it with Clippy for an Android target, and only
  `integration_test/keystore_test.dart` runs it, on an emulator.
- **Blocking calls.** Each call blocks its thread while the Keystore works: an HMAC took 585 µs
  on the API 26 emulator and 1,015 µs on API 36 in M3.1's spike (ADR-0007).

## The API

Built in M3.2 as described here.

```rust
pub struct Keystore { /* alias_prefix, profile_file */ }

impl Keystore {
    pub const VAULT_ALIAS_PREFIX: &str = "denarii_vault_";
    pub const VAULT_PROFILE_FILE: &str = "vault_profile";
    pub const MAX_NAME_BYTES: usize = 64;

    pub fn vault() -> Self;
    pub fn new(alias_prefix: &str, profile_file: &str) -> Result<Self, KeystoreError>;
    pub fn alias_prefix(&self) -> &str;
    pub fn profile_file(&self) -> &str;

    pub fn ensure_device_key(&self) -> Result<(), KeystoreError>;
    pub fn device_hmac(&self, message: &[u8]) -> Result<[u8; 32], KeystoreError>;
    pub fn read_profile(&self) -> Result<Option<Vec<u8>>, KeystoreError>;
    pub fn write_profile(&self, plaintext: &[u8]) -> Result<(), KeystoreError>;
    pub fn delete_profile(&self) -> Result<(), KeystoreError>;
    pub fn delete_keys(&self) -> Result<(), KeystoreError>;
}

#[non_exhaustive]
pub enum KeystoreError {
    InvalidName, ReservedName, Unavailable, KeyMissing, Unreadable, Failed, Unexpected,
}
```

- **Names.** A `Keystore` names one set of keys, the prefix followed by `storage`, `device`, and
  `biometric`, and one preferences file, `shared_prefs/<profile_file>.xml`. `Keystore::vault()`
  names v1's. `Keystore::new` takes 1 to 64 bytes of lowercase ASCII letters, digits, and `_` for
  each name, which holds no path separator and crosses JNI unchanged, and refuses a prefix that
  starts with `denarii_vault_` or one of v1's four preferences files (`vault_profile`,
  `secure_prefs`, `db_key_prefs`, and `ui_prefs`) with `ReservedName`. So only the vault reaches a
  user's keys and files.
- **The device key.** `ensure_device_key` creates the HMAC-SHA256 key unless it exists, as v1 did
  before it wrote a new profile. `device_hmac` returns HMAC-SHA256 of the message under it, and
  never creates it: without the key it fails with `KeyMissing`.
- **The profile.** `read_profile` opens the `profile` entry of the file, Base64 of a 12-byte IV,
  the ciphertext, and a 16-byte tag, sealed with AES-256-GCM under the storage key with the
  associated data `denarii-dolor:profile:v2`, or returns `None` when there is no entry.
  `write_profile` seals with a new IV, which the Keystore chooses, and saves the entry with a
  synchronous `commit()`. It creates the storage key only when the file has no entry; with an
  entry and no key it fails with `KeyMissing` and leaves the entry as it was, because a new key
  would hide a profile that can't be opened. This is the format in section 3 of the format spec.
- **Deleting.** `delete_profile` empties the file and then deletes it, as v1 did, because Android
  keeps a deleted file's contents in memory. `delete_keys` deletes all three keys, and tries every
  one even when one fails (CS-17). A file or key that doesn't exist counts as deleted. Creating
  the biometric key comes in M3.6.
- **Secrets.** What `device_hmac` and `read_profile` return is secret, and the caller overwrites
  it with zeros once it is used. M3.3 chooses the crate for that (`docs/02-dependency-policy.md`).
- **Off Android** every call returns `Unavailable`, so the crate builds and its name checks run
  on the host.

### Errors

| Variant | When |
| :- | :- |
| `InvalidName` | A name given to `Keystore::new` breaks the character or length rule |
| `ReservedName` | A name given to `Keystore::new` is one of v1's |
| `Unavailable` | No Java runtime: not Android, or `JNI_OnLoad` hasn't run or couldn't find the Kotlin code |
| `KeyMissing` | The device key for an HMAC, or the storage key for a stored profile, doesn't exist |
| `Unreadable` | The entry isn't Base64, is shorter than an IV and a tag, or fails AES-GCM's check |
| `Failed` | Any other Keystore or storage failure, such as a `commit()` that returns false |
| `Unexpected` | A JNI call failed, or Kotlin threw something it doesn't name, such as an `OutOfMemoryError` |

v1 reported all of the Keystore and storage failures as one `SecureStorageException`. The vault
(M3.3) decides what each variant means to the user.

## The Kotlin side

`android/app/src/main/kotlin/com/denariidolor/VaultKeystore.kt` holds `VaultKeystore`, an
`object` whose six `@JvmStatic` methods only Rust calls: `deviceHmac`, `ensureDeviceKey`,
`readProfile`, `writeProfile`, `deleteProfile`, and `deleteKeys`. Each takes the alias prefix or
the file name as a `String`, and the bytes as a `byte[]`. They create keys with v1's
`KeyGenParameterSpec`s (`KeystoreSecrets.kt` at `6d57526`): the storage key as AES-256 for GCM
with no padding, and the device key as HMAC-SHA256 for signing, neither needing the user to
authenticate.

- **Failures.** Kotlin catches the Keystore's and storage's exceptions, as v1's `keystoreCall`
  did, and throws `KeystoreException`, whose `int code` field is a `KeystoreFailure`: 1 for
  `KEY_MISSING`, 2 for `UNREADABLE`, and 3 for `FAILED`. Rust reads the code and maps it to the
  `KeystoreError` variant of the same name; any other code or exception is `Unexpected`.
- **One caller at a time.** `ensureDeviceKey`, `writeProfile`, `deleteProfile`, and `deleteKeys`
  are `@Synchronized` on the object, so two threads can't both create a key, which would let the
  second overwrite the first.
- **The context.** `MainActivity.onCreate` hands `VaultKeystore` the application context before
  Flutter starts Dart; the profile methods need it to open preferences files.
- **R8.** No Java or Kotlin code calls the six methods, so `android/app/proguard-rules.pro` keeps
  the two class names, the six methods, and the `code` field, and nothing else of them.

## How a call crosses

1. At startup, `MainActivity`'s class initializer loads `libdenarii_bridge.so` with
   `System.loadLibrary`, so the runtime calls `JNI_OnLoad`, which this crate exports through the
   bridge's library. On that thread, `FindClass` sees the app's classes. `JNI_OnLoad` keeps the
   `JavaVM`, global references to `VaultKeystore` and `KeystoreException`, and the IDs of the six
   methods and the field. A thread attached from native code sees only the system's classes, so
   looking them up later would fail with `ClassNotFoundException` (M3.1's spike). If anything is
   missing, it stores nothing, and every call returns `Unavailable`; loading still succeeds, so
   the app still starts. Dart later opens the same loaded library by name.
2. A call checks its names, attaches its thread to the runtime if the thread isn't attached, and
   detaches it at the end.
3. It makes a `String` for each name and a `byte[]` for each byte argument, calls the static
   method, and takes any exception before anything else, because JNI allows only a few functions
   while one is pending.
4. It copies a returned `byte[]` out. Before it returns, every `byte[]` it made or received is
   overwritten with zeros, and every local reference is deleted, even on an error.

## Lints and `unsafe`

The crate repeats the workspace's lints, because Cargo can't relax one inherited lint, with
`unsafe_code = "deny"` instead of `forbid`, `unsafe_op_in_unsafe_fn = "deny"`, and Clippy's
`multiple_unsafe_ops_per_block` and `undocumented_unsafe_blocks`. `src/lib.rs` exempts the `jni`
module alone, with an `#[expect(unsafe_code, reason = "…")]` on its `mod` line. Inside it:

- `Env` is a thread's environment, valid only on that thread while it stays attached; its lifetime
  ties it to the `Attached` guard or to the `JNI_OnLoad` call, and it is neither `Send` nor `Sync`.
- `Local` owns one local reference and deletes it on drop. `ByteArray` owns a `byte[]` and
  overwrites it with zeros on drop.
- Each `Env` method leaves no Java exception pending, which every other JNI call relies on.
- `JNI_OnLoad` runs inside `catch_unwind`, so a panic never unwinds into the Java runtime.

The declarations are `jni-sys` 0.4.1's, of the JNI specification's `jni.h`, pinned exactly in
`rust/Cargo.toml` (ADR-0022). Every function used is in JNI 1.6, which Android's runtime implements
on API 26 and later.

## Invariants

1. Only `Keystore::vault()` names an alias that starts with `denarii_vault_`, or one of v1's
   preferences files.
2. `device_hmac` and `read_profile` never create a key.
3. `write_profile` creates the storage key only when the file has no `profile` entry.
4. The same device key gives the same HMAC for the same message, and `ensure_device_key` never
   replaces an existing key.
5. A profile written under one storage key reads back unchanged under it, and is `Unreadable`
   under another.
6. `delete_keys` leaves none of the three keys, and `delete_profile` leaves no entry.
7. Every call returns `Unavailable` off Android.
8. No call panics, and no panic reaches the Java runtime.
9. The release build keeps every class, method, and field name that `JNI_OnLoad` looks up.

## Tests

| Invariant | Checked by |
| :- | :- |
| 1 | `tests/keystore.rs`, which covers empty, 64- and 65-byte names, 14 kinds of bad characters, v1's prefix and four files, and names close to them |
| 2, 3, 4, 5, 6 | `integration_test/keystore_test.dart` on the `Denarii_API_26` and `Denarii_API_36` emulators, through `keystore_self_check` in the bridge (`docs/specs/bridge.md`), once alone and eight times at once |
| 7 | `tests/keystore.rs`, and `rust/crates/bridge/tests/keystore_check.rs` |
| 8 | Review of `src/jni.rs`, whose JNI calls run only in the integration test; the unit tests in `src/lib.rs` cover the 32-byte check and the wiping |
| 9 | A release build: `dexdump` of `classes.dex` lists the two classes, the six methods, and the field under their names (checked in M3.2) |

Not covered: a stored entry that isn't Base64 or is too short, because nothing in the API writes
one; arm64 phones; and the profile and keys v1 wrote on a phone, which M3.2's remaining check
covers.
