# Subsystem: `vault`

`denarii-vault` (`rust/crates/vault`) guards the database key. It ports v1's
`SecurityProfileService`: the key wrapped under the PIN and under the normalized security answer,
the rules for both, and the lockout that failed checks share (CS-10, CS-13, CS-14, CS-15). It
reads and writes the profile exactly as v1 stored it on phones (`docs/specs/android-v1-formats.md`,
section 3), so that the same PIN and answer open it after the update (ADR-0002, existing installs
upgrade in place). ADR-0007 (hardware-key operations stay in platform code, which Rust calls
directly), ADR-0022 (`jni-sys` in a crate of its own), and ADR-0023 (the RustCrypto crates,
`unicode-normalization`, and `serde_json`) shaped it. Built in M3.3 as described here; the API
reference is `cargo doc -p denarii-vault --open`, run from `rust/`. The bridge calls it from M3.5.

## Why this design

The profile is a contract: every phone with v1 holds one, sealed under its own Keystore keys, and
nothing can repair it by hand. So the port follows v1 step by step, and v1's own golden values
check it. Five choices follow from that.

- **Rust, not Dart.** Keys never enter Dart, because the Dart runtime can't wipe memory it has
  freed (`docs/01-architecture.md`, principle 4). The vault stretches each secret, asks the
  Keystore's device key to mix it, and unwraps the database key, all in Rust.
- **Traits for the platform.** The device key and the sealed profile live in the Android Keystore,
  which only a phone has. `DeviceKey` and `ProfileStore` are traits that
  `denarii_keystore::Keystore` implements with v1's keys, so the tests can stand in for the
  Keystore on the host, as v1's tests used a software HMAC and a map.
- **Time from the caller.** The lockout runs on time since boot and the boot count (CS-13). Rust
  reads no clock (`docs/01-architecture.md`, dependency rule 5), so each call that runs under the
  lockout takes a `MonotonicTime`, as the store takes each month's bounds (ADR-0019). The cost:
  the vault can't check that the caller read the right clock. M3.5 chooses where the app reads
  `SystemClock.elapsedRealtime()` and `Settings.Global.BOOT_COUNT`.
- **One lock for each call.** v1's methods were `@Synchronized`, so two checks never ran at once
  to get around the lockout. `SecurityProfile` holds a `std::sync::Mutex` for the whole of each
  call, PBKDF2 included, so a second sign-in waits for the first.
- **The types v1 wrote.** v1 wrote the profile's JSON with Android's `org.json`, which converts
  between types when it reads (a string `"210000"` reads as a number). The port reads only the
  types v1 wrote, strings and whole numbers, because v1 never wrote anything else.

What it trades away:

- **Two crypto libraries.** SQLCipher uses OpenSSL's `libcrypto` (ADR-0017), and the vault uses
  RustCrypto (ADR-0023), so each needs its own upgrades and reviews.
- **A newer Unicode.** Answers normalize with Unicode 18.0's NFKC and Unicode 17.0's lowercase,
  where each phone used its own Android release's ICU. Unicode never changes how a character it
  has assigned normalizes, but a case mapping, or a character an older phone didn't know, could
  differ. Fixture F7 checks this on devices; it doesn't exist yet.
- **Best-effort wiping.** Every secret this crate holds is wiped when dropped, but copies that the
  compiler makes when a value moves, and buffers that grow while text is built, aren't.

## The API

Built in M3.3 as described here.

```rust
pub struct SecurityProfile<D, S, R = OsRandom> { /* device_key, store, iterations, Mutex<state> */ }

impl<D: DeviceKey, S: ProfileStore> SecurityProfile<D, S> {
    pub fn new(device_key: D, store: S) -> Self; // OsRandom, Iterations::DEFAULT
}

impl<D: DeviceKey, S: ProfileStore, R: RandomSource> SecurityProfile<D, S, R> {
    pub fn with_random(device_key: D, store: S, random: R, iterations: Iterations) -> Self;
    pub fn state(&self) -> Result<ProfileState, VaultError>;
    pub fn set_up(&self, pin: &str, confirmation: &str, question: &str, answer: &str)
        -> Result<Setup, VaultError>;
    pub fn unlock_with_pin(&self, pin: &str, now: MonotonicTime) -> Result<SignIn, VaultError>;
    pub fn recover_pin(&self, answer: &str, new_pin: &str, confirmation: &str, now: MonotonicTime)
        -> Result<Recovery, VaultError>;
    pub fn lockout_remaining_millis(&self, now: MonotonicTime) -> Result<u64, VaultError>;
}

pub trait DeviceKey {
    fn prepare(&self) -> Result<(), KeystoreError>;
    fn mix(&self, message: &[u8]) -> Result<Zeroizing<[u8; 32]>, KeystoreError>;
}
pub trait ProfileStore {
    fn read(&self) -> Result<Option<Zeroizing<Vec<u8>>>, KeystoreError>;
    fn write(&self, plaintext: &[u8]) -> Result<(), KeystoreError>;
}
pub trait RandomSource {
    fn fill(&mut self, bytes: &mut [u8]) -> Result<(), RandomUnavailable>;
}

pub struct MonotonicTime { pub elapsed_millis: i64, pub boot_count: Option<i32> }
pub struct Iterations(u32); // MIN 10,000, MAX 1,000,000, DEFAULT 210,000
pub struct DatabaseKey(/* 32 bytes, wiped on drop */);

pub fn is_valid_pin(pin: &str) -> bool;
pub fn normalize_answer(answer: &str) -> Zeroizing<String>;
pub fn validate_setup(pin: &str, confirmation: &str, question: &str, answer: &str)
    -> Result<(), SetupProblem>;
pub fn lockout_duration_millis(failures: u32) -> u64;
```

The app makes the profile with `SecurityProfile::new(Keystore::vault(), Keystore::vault())`.
`with_random` exists for v1's golden tests, which hand out known bytes and use 10,000 iterations;
stored slots always open with their own iteration counts.

Each v1 method has its counterpart here:

| v1's `SecurityProfileService` | `SecurityProfile` | Result |
| :- | :- | :- |
| `getProfileState` | `state` | `ProfileState::NeedsSetup`, or `Configured` with the stored question |
| `setupProfile` | `set_up` | `Setup::Saved`, `AlreadyConfigured`, or `Invalid(SetupProblem)` |
| `unlockWithPin` | `unlock_with_pin` | `SignIn::Unlocked(DatabaseKey)`, `InvalidPinFormat`, `Rejected`, or `LockedOut` |
| `recoverPin` | `recover_pin` | `Recovery`, with v1's `RecoverPinResult` values; `LockedOut` carries the time left, which v1's `Vault` asked for separately |
| `lockoutRemainingMillis` | `lockout_remaining_millis` | Milliseconds |
| `validateSetup`, `normalizeAnswer`, `isValidPin`, `lockoutDurationMillis` | Free functions of the same names | |
| `migrateProfile`, the public `attempt` | Not ported: only the v1.0.x upgrade calls them (M3.4) | |
| `biometricWrap`, `setBiometricWrap`, `recordSuccessfulAuthentication` | Not ported: biometric sign-in (M3.6) | |

`unlock_with_pin` refuses a PIN that isn't 6 to 12 ASCII digits with `SignIn::InvalidPinFormat`,
without checking or counting it. v1's sign-in screen did that before it called the service, so
users see the same behavior, and CS-10 now lives with the other rules in Rust.

## The stored profile

The plaintext is v1's JSON object, with the keys in section 3 of the format spec. The
`ProfileStore` seals and opens it; on Android that is `Keystore::write_profile` and
`Keystore::read_profile` (`docs/specs/keystore.md`).

- **Reading.** A string key reads only a JSON string, and a number key only a whole JSON number
  that fits Kotlin's `Int` or `Long`. Anything else reads as absent, where `org.json` would have
  converted it. Base64 values are decoded as `java.util.Base64`'s basic decoder reads them:
  padding is optional, and any other character, a lone symbol in the last group, misplaced
  padding, or anything after the padding is refused.
- **Set up.** The profile counts as set up only when `profile_version` is 2, the question isn't
  blank by Kotlin's rule, and both slots have a wrap and a salt that aren't blank and an iteration
  count from 10,000 to 1,000,000. That is v1's check, so anything less reads as
  `ProfileState::NeedsSetup`, as it did in v1 (open question 7).
- **Damage.** A plaintext that isn't a JSON object is `VaultError::ProfileUnreadable`, never "no
  profile". A set-up profile whose slot can't be opened (a wrap or salt that isn't Base64, a wrap
  no longer than its IV, or a slot that opens to a key that isn't 32 bytes) is
  `VaultError::SlotDamaged`, and isn't counted as a failure. A wrap with room for an IV but not a
  tag fails AES-GCM's check, which v1 counted as a wrong PIN, and so does the port.
- **Writing.** Each change re-encrypts and writes the whole profile, as v1's `edit` did, and
  updates the copy the profile keeps only after the write succeeds, so a failed write changes
  nothing. Keys the vault doesn't know, such as `biometric_wrap`, are kept on every edit. Setup
  clears everything first. `serde_json` writes the object with its keys sorted, where `org.json`
  kept the order they were put in; only the new app reads the profile after the update.
- **Slots.** Setup takes 32 random bytes for the database key, then 16 for the PIN's salt, 12 for
  its IV, 16 for the answer's salt, and 12 for its IV, in that order, which the golden profile
  depends on. Each slot is sealed as the format spec's invariant 4 says, with `iterations` from
  the constructor, 210,000 in the app.

## The rules

- **PIN.** 6 to 12 ASCII digits, v1's `^[0-9]{6,12}$`, for setup, recovery, and sign-in (CS-10).
- **Answer.** `normalize_answer` applies v1's four steps: NFKC; Kotlin's `trim()`, with
  `denarii_core::is_whitespace`; each run of Unicode `White_Space` becomes one space, which is
  what Android's `\s` matched in M2.7's tests; and Rust's `str::to_lowercase`. Kotlin's trim and
  `White_Space` differ on U+001C to U+001F and U+0085, so those keep v1's quirks: the first four
  are trimmed but never collapsed, and U+0085 is collapsed but never trimmed.
- **Setup.** `validate_setup` checks v1's nine rules in v1's order, and returns the first
  `SetupProblem` broken. The answer's minimum counts code points, and its maximum and the
  question's count UTF-16 code units, as v1 did. Each `SetupProblem::code` is v1's
  `SetupProfileResult` name, such as `SECURITY_ANSWER_TOO_SHORT`, which the screens map to v1's
  messages in the ARB files (M3.5).

## The lockout

Failed PIN and answer checks share one counter, `failed_attempts`. The first four only count; the
fifth locks for 30 seconds, and each further failure doubles the lockout, up to 15 minutes from
the tenth. A rejected check reports how many failures are left before a lockout.

The timer is stored as v1 stored it: the duration, the time since boot when it started, and the
boot count then, or −1. At each check, a time earlier than the start, or a boot count that both
sides know and that differs, means the device rebooted, so the whole lockout starts again from
now and is saved. A lockout that has run out is cleared from the profile, but the counter stays,
so the next failure locks for longer. A success, by PIN or by answer, clears both. Because v1
stored all of this in the profile, a lockout that v1 started carries on after the update.

## Errors

| Variant | When | v1 |
| :- | :- | :- |
| `Keystore(KeystoreError)` | The device key or the store failed, such as `KeyMissing` when the device key is gone | `SecureStorageException` |
| `ProfileUnreadable` | The plaintext isn't a JSON object | `SecureStorageException` |
| `SlotDamaged` | A set-up profile's slot can't be opened, as above | `SecureStorageException` |
| `NotConfigured` | Sign-in was asked for before setup | `IllegalStateException`, a bug |
| `RandomUnavailable` | The operating system's random source failed | Not possible with `SecureRandom` |
| `Poisoned` | An earlier call panicked while it held the lock | Not possible in Kotlin |

v1's sign-in screen showed one message for every `SecureStorageException`, that the device's
secure storage can't be read, with a way to retry or wipe (BUG-05). The first three variants are
those failures; M3.5 maps them to the screens.

## Secrets in memory

The crate wipes, with `zeroize`, each secret it holds: the database key (`DatabaseKey`), the
normalized answer and each step's text, PBKDF2's output, the device key's input and output, the
key-encryption key, AES-GCM's plaintext, the profile's plaintext, and, through `aes-gcm`'s and
`sha2`'s `zeroize` features, the AES key schedules and SHA-256's state. It doesn't wipe the
caller's own strings, the 32-byte array that `Keystore::device_hmac` returns by value before it
is wrapped, or the `serde_json` values of the profile, which hold no plaintext secret: the wraps
are ciphertext, and the salts and the question aren't secret.

## Invariants

1. A profile that `set_up` writes from v1's golden input, with v1's counting random source and
   10,000 iterations, holds exactly v1's golden values.
2. v1's golden profile opens with its PIN, rejects another, and recovers with its answer however
   it is typed, keeping the answer's slot and wrapping the same key under the new PIN.
3. PBKDF2-HMAC-SHA256 and AES-256-GCM give fixture F1's known answers, and AES-GCM opens nothing
   that changed.
4. A slot sealed outside the crate as the format spec's invariant 4 says opens with its PIN.
5. Stored iteration counts from 10,000 to 1,000,000 are honored, and any other count, or a type
   v1 didn't write, reads as not set up.
6. The lockout's schedule, shared counter, timer, reboot restart, and reset match section 3 of the
   format spec, and concurrent wrong attempts are each counted.
7. A failing device key or store is an error, never a wrong PIN; a plaintext that isn't a JSON
   object is never "no profile"; and a failed write changes nothing.
8. A PIN of the wrong form is refused at sign-in without being checked or counted.
9. No call panics on any input or stored value, and every call after a panic inside one returns
   `Poisoned`.
10. Answers normalize as "The rules" describes.
11. Keys the vault doesn't know survive every edit except setup.

## Tests

| Invariant | Checked by |
| :- | :- |
| 1, 2 | `tests/profile/golden.rs`, v1's `SecurityProfileGoldenTest`, with the golden values unchanged |
| 3 | The unit tests in `src/crypto.rs`, v1's `CryptoKnownAnswerTest` (fixture F1) without its NFC samples, which belong to backups (M5.1) |
| 4, 5, 7, 11 | `tests/profile/damaged.rs`, which seals a slot with `pbkdf2`, `hmac`, and `aes-gcm` directly, and covers incomplete and damaged profiles and failing stores |
| 6, 8 | `tests/profile/service.rs`, v1's `SecurityProfileServiceTest`, and the unit tests in `src/lockout.rs` |
| 9 | `tests/poisoned.rs`, and the extreme stored values in `src/lockout.rs` and `src/stored.rs` |
| 10 | `tests/rules.rs`, with v1's normalization cases and the whitespace and case-mapping edges |

The Base64 codec has unit tests in `src/base64.rs`, with RFC 4648's vectors and the inputs Java's
decoder refuses.

Not covered: fixture F7, the answers that v1 normalized on API 26 and API 36, which doesn't exist
yet; and the `Keystore` implementations of the two traits on a device, which M3.5 checks when
sign-in opens a profile v1 wrote. Off Android, the unit test in `src/storage.rs` shows that both
reach the keystore crate, which returns `Unavailable`.
