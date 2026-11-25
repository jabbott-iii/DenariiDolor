/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! The primitives the profile is built from, as v1's `CryptoPrimitives` gave them: PBKDF2 with
//! HMAC-SHA256 and a 32-byte output, and AES-256-GCM with a 12-byte nonce and a 16-byte tag
//! (`docs/specs/android-v1-formats.md`, section 3), from the RustCrypto crates (ADR-0023).
//!
//! v1's `CryptoKnownAnswerTest` (fixture F1) pins their exact bytes in the tests below, because
//! every profile on a phone was sealed with them. The key and nonce sizes are in the types, so the
//! wrong sizes v1 refused at run time don't compile here.

use aes_gcm::Aes256Gcm;
use aes_gcm::aead::{AeadInOut, KeyInit, Tag};
use sha2::Sha256;
use zeroize::Zeroizing;

/// The length of an AES-256 key and of PBKDF2's output, in bytes.
pub(crate) const KEY_LEN: usize = 32;
/// The length of an AES-GCM nonce (the IV), in bytes.
pub(crate) const NONCE_LEN: usize = 12;
/// The length of an AES-GCM tag, in bytes.
pub(crate) const TAG_LEN: usize = 16;

/// PBKDF2-HMAC-SHA256 over the UTF-8 bytes of `secret`, 32 bytes long, as Java's
/// `PBKDF2WithHmacSHA256` derived it.
pub(crate) fn pbkdf2_hmac_sha256(
    secret: &str,
    salt: &[u8],
    iterations: u32,
) -> Zeroizing<[u8; KEY_LEN]> {
    let mut derived = Zeroizing::new([0; KEY_LEN]);
    pbkdf2::pbkdf2_hmac::<Sha256>(secret.as_bytes(), salt, iterations, derived.as_mut_slice());
    derived
}

/// `plaintext` sealed with AES-256-GCM: the ciphertext followed by the tag.
#[expect(
    clippy::expect_used,
    reason = "AES-GCM refuses only a message longer than about 64 GiB, and the profile seals 32-byte keys"
)]
pub(crate) fn aes_gcm_seal(
    key: &[u8; KEY_LEN],
    nonce: &[u8; NONCE_LEN],
    associated_data: &[u8],
    plaintext: &[u8],
) -> Vec<u8> {
    let cipher = Aes256Gcm::new(key.into());
    let mut sealed = Vec::with_capacity(plaintext.len().saturating_add(TAG_LEN));
    sealed.extend_from_slice(plaintext);
    let tag = cipher
        .encrypt_inout_detached(nonce.into(), associated_data, sealed.as_mut_slice().into())
        .expect("a message this short always seals");
    sealed.extend_from_slice(&tag);
    sealed
}

/// The plaintext that [`aes_gcm_seal`] sealed, or `None` when the tag doesn't verify: a wrong
/// key, a changed nonce, associated data, ciphertext, or tag, or `sealed` shorter than a tag.
pub(crate) fn aes_gcm_open(
    key: &[u8; KEY_LEN],
    nonce: &[u8; NONCE_LEN],
    associated_data: &[u8],
    sealed: &[u8],
) -> Option<Zeroizing<Vec<u8>>> {
    let (ciphertext, tag) = sealed.split_last_chunk::<TAG_LEN>()?;
    let tag: &Tag<Aes256Gcm> = tag.into();
    let mut plaintext = Zeroizing::new(ciphertext.to_vec());
    Aes256Gcm::new(key.into())
        .decrypt_inout_detached(
            nonce.into(),
            associated_data,
            plaintext.as_mut_slice().into(),
            tag,
        )
        .ok()?;
    Some(plaintext)
}

#[cfg(test)]
mod tests {
    use std::fmt::Write;

    use super::*;

    struct Pbkdf2Vector {
        secret: &'static str,
        salt: &'static str,
        iterations: u32,
        expected: &'static str,
    }

    struct GcmVector {
        key: &'static str,
        nonce: &'static str,
        associated_data: &'static str,
        plaintext: &'static str,
        sealed: &'static str,
    }

    const SALT_A: &str = "101112131415161718191a1b1c1d1e1f";

    /// v1's `CryptoKnownAnswerTest.PBKDF2`, computed outside the app with Python's `hashlib`.
    const PBKDF2: [Pbkdf2Vector; 8] = [
        // RFC 7914, section 11 (the first 32 bytes).
        Pbkdf2Vector {
            secret: "passwd",
            salt: "73616c74",
            iterations: 1,
            expected: "55ac046e56e3089fec1691c22544b605f94185216dde0465e68b9d57c20dacbc",
        },
        Pbkdf2Vector {
            secret: "Password",
            salt: "4e61436c",
            iterations: 80_000,
            expected: "4ddcd8f60b98be21830cee5ef22701f9641a4418d04c0414aeff08876b34ab56",
        },
        // A PIN at the profile's iteration count, then answers in the app's scripts, then a
        // backup passphrase.
        Pbkdf2Vector {
            secret: "135790",
            salt: "000102030405060708090a0b0c0d0e0f",
            iterations: 210_000,
            expected: "cf62abfb966669173e02e9ab9fc17779418759795964dc97fab43a4f4c5611c1",
        },
        Pbkdf2Vector {
            secret: "café müller",
            salt: SALT_A,
            iterations: 10_000,
            expected: "8dba8f749cf51925cff988ab7d988d121e9b4de1d21fe0ccb548d1a42e5f22dc",
        },
        Pbkdf2Vector {
            secret: "मेरा पहला स्कूल",
            salt: SALT_A,
            iterations: 10_000,
            expected: "8f680ec12b07ee92c17aaf510797641f9d1cbc8cbab2c0627899ac01754fdb39",
        },
        Pbkdf2Vector {
            secret: "我的第一所学校",
            salt: SALT_A,
            iterations: 10_000,
            expected: "bdefaf22acba2d1bdd9e16f1d8936acfb34430647feb1a7f8a7336d74d43ae55",
        },
        Pbkdf2Vector {
            secret: "مدرستي الأولى",
            salt: SALT_A,
            iterations: 10_000,
            expected: "7b9f34d40d08f7208db2bbd229417891c1b5cca9d0e73de498388b1a31859cc8",
        },
        Pbkdf2Vector {
            secret: "café au lait chaud",
            salt: "202122232425262728292a2b2c2d2e2f",
            iterations: 100_000,
            expected: "09fffbd097fe4966169d167b7a17a5322cbbdb6bef60477aeb1b6c01732b43e7",
        },
    ];

    /// v1's `CryptoKnownAnswerTest.GCM`, computed outside the app with OpenSSL.
    const GCM: [GcmVector; 3] = [
        // Wrapping a database key under the PIN slot's associated data, as the profile does.
        GcmVector {
            key: "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f",
            nonce: "6465666768696a6b6c6d6e6f",
            associated_data: "64656e617269692d646f6c6f723a70696e3a7632",
            plaintext: "c8c9cacbcccdcecfd0d1d2d3d4d5d6d7d8d9dadbdcdddedfe0e1e2e3e4e5e6e7",
            sealed: "80d214adb5249851eeb38d3b0eb0bc2a9a1bdcd157b12dad47304eab1f4643af6a868555997136136d4d3486340707b9",
        },
        GcmVector {
            key: "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f",
            nonce: "6465666768696a6b6c6d6e6f",
            associated_data: "",
            plaintext: "",
            sealed: "609bd50f6ad76df665f048ba8c5fd706",
        },
        // The GCM specification's test case 14.
        GcmVector {
            key: "0000000000000000000000000000000000000000000000000000000000000000",
            nonce: "000000000000000000000000",
            associated_data: "",
            plaintext: "00000000000000000000000000000000",
            sealed: "cea7403d4d606b6e074ec5d3baf39d18d0d1c8a799996bf0265b98b5d48ab919",
        },
    ];

    fn bytes(hex: &str) -> Vec<u8> {
        hex.as_bytes()
            .chunks(2)
            .map(|pair| u8::from_str_radix(std::str::from_utf8(pair).unwrap(), 16).unwrap())
            .collect()
    }

    fn hex(bytes: &[u8]) -> String {
        bytes.iter().fold(String::new(), |mut text, byte| {
            write!(text, "{byte:02x}").unwrap();
            text
        })
    }

    fn array<const N: usize>(hex: &str) -> [u8; N] {
        bytes(hex).try_into().unwrap()
    }

    fn seal(vector: &GcmVector) -> Vec<u8> {
        aes_gcm_seal(
            &array(vector.key),
            &array(vector.nonce),
            &bytes(vector.associated_data),
            &bytes(vector.plaintext),
        )
    }

    #[test]
    fn pbkdf2_gives_its_known_answers() {
        for vector in &PBKDF2 {
            let derived = pbkdf2_hmac_sha256(vector.secret, &bytes(vector.salt), vector.iterations);
            assert_eq!(
                hex(derived.as_slice()),
                vector.expected,
                "{:?} {}",
                vector.secret,
                vector.iterations
            );
        }
    }

    #[test]
    fn aes_gcm_seals_to_its_known_answers() {
        for vector in &GCM {
            assert_eq!(hex(&seal(vector)), vector.sealed);
        }
    }

    #[test]
    fn aes_gcm_opens_what_it_sealed() {
        for vector in &GCM {
            let opened = aes_gcm_open(
                &array(vector.key),
                &array(vector.nonce),
                &bytes(vector.associated_data),
                &bytes(vector.sealed),
            );
            assert_eq!(
                opened.map(|plaintext| hex(&plaintext)),
                Some(vector.plaintext.to_owned())
            );
        }
    }

    /// A wrong key and a changed nonce, associated data, ciphertext, or tag all fail the same way:
    /// `None`, never a plaintext.
    #[test]
    fn aes_gcm_opens_nothing_that_changed() {
        let vector = &GCM[0];
        let parts = [
            bytes(vector.key),
            bytes(vector.nonce),
            bytes(vector.associated_data),
            bytes(vector.sealed),
        ];
        for part in 0..parts.len() {
            for index in [0, parts[part].len() - 1] {
                let mut changed = parts.clone();
                changed[part][index] ^= 1;
                let opened = aes_gcm_open(
                    &changed[0].clone().try_into().unwrap(),
                    &changed[1].clone().try_into().unwrap(),
                    &changed[2],
                    &changed[3],
                );
                assert!(opened.is_none(), "part {part} byte {index}");
            }
        }
    }

    #[test]
    fn aes_gcm_opens_nothing_shorter_than_a_tag() {
        let vector = &GCM[1];
        let sealed = bytes(vector.sealed);
        for length in [0, 1, TAG_LEN - 1] {
            assert!(
                aes_gcm_open(
                    &array(vector.key),
                    &array(vector.nonce),
                    &[],
                    &sealed[..length]
                )
                .is_none(),
                "length {length}"
            );
        }
    }
}
