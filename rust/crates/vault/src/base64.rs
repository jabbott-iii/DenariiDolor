/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! Base64 as `java.util.Base64`'s basic encoder writes it and its basic decoder reads it, which
//! is how v1 stored every byte string in the profile (`docs/specs/android-v1-formats.md`,
//! section 3).
//!
//! The encoder writes RFC 4648's standard alphabet with `=` padding and no line breaks. The
//! decoder accepts what Java's accepts, padded or not, and refuses what Java's refuses: any other
//! character, a lone symbol in the last group, padding in the wrong place, and anything after the
//! padding. Like Java's, it ignores leftover bits in the last symbol.

/// The standard alphabet, in order of value.
const ALPHABET: &[u8; 64] = b"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

/// `bytes` as padded Base64.
pub(crate) fn encode(bytes: &[u8]) -> String {
    let mut text = String::with_capacity(bytes.len().div_ceil(3).saturating_mul(4));
    for group in bytes.chunks(3) {
        let first = group.first().copied().unwrap_or(0);
        let second = group.get(1).copied().unwrap_or(0);
        let third = group.get(2).copied().unwrap_or(0);
        text.push(symbol(first >> 2));
        text.push(symbol(((first & 0x03) << 4) | (second >> 4)));
        text.push(if group.len() > 1 {
            symbol(((second & 0x0f) << 2) | (third >> 6))
        } else {
            '='
        });
        text.push(if group.len() > 2 {
            symbol(third & 0x3f)
        } else {
            '='
        });
    }
    text
}

/// The bytes `text` encodes, or `None` where Java's decoder throws.
pub(crate) fn decode(text: &str) -> Option<Vec<u8>> {
    let mut input = text.bytes();
    let mut bytes = Vec::with_capacity((text.len() / 4).saturating_mul(3).saturating_add(2));
    let mut bits: u32 = 0;
    // Where the next symbol's six bits go: 18, 12, 6, or 0, so 18 means the group is empty.
    let mut shift: u32 = 18;
    while let Some(byte) = input.next() {
        if byte == b'=' {
            // Java refuses padding in an empty group, and a single `=` after two symbols.
            match shift {
                18 => return None,
                6 if input.next() != Some(b'=') => return None,
                _ => {}
            }
            if input.next().is_some() {
                return None;
            }
            break;
        }
        bits |= u32::from(value(byte)?) << shift;
        if shift == 0 {
            let [_, first, second, third] = bits.to_be_bytes();
            bytes.extend_from_slice(&[first, second, third]);
            bits = 0;
            shift = 18;
        } else {
            shift = shift.saturating_sub(6);
        }
    }
    let [_, first, second, _] = bits.to_be_bytes();
    match shift {
        // A lone symbol holds six bits, too few for a byte.
        12 => return None,
        6 => bytes.push(first),
        0 => bytes.extend_from_slice(&[first, second]),
        _ => {}
    }
    Some(bytes)
}

/// The symbol for a six-bit value.
#[expect(
    clippy::indexing_slicing,
    reason = "the mask keeps the index below 64, the alphabet's length"
)]
fn symbol(sextet: u8) -> char {
    char::from(ALPHABET[usize::from(sextet & 0x3f)])
}

/// The six-bit value of a symbol of the alphabet.
fn value(symbol: u8) -> Option<u8> {
    let index = ALPHABET.iter().position(|&candidate| candidate == symbol)?;
    u8::try_from(index).ok()
}

#[cfg(test)]
mod tests {
    use super::{decode, encode};

    #[test]
    fn encodes_rfc_4648_test_vectors() {
        assert_eq!(encode(b""), "");
        assert_eq!(encode(b"f"), "Zg==");
        assert_eq!(encode(b"fo"), "Zm8=");
        assert_eq!(encode(b"foo"), "Zm9v");
        assert_eq!(encode(b"foob"), "Zm9vYg==");
        assert_eq!(encode(b"fooba"), "Zm9vYmE=");
        assert_eq!(encode(b"foobar"), "Zm9vYmFy");
    }

    #[test]
    fn encodes_every_byte_value_with_both_extra_symbols() {
        let bytes: Vec<u8> = (0..=255).collect();
        let text = encode(&bytes);
        assert!(text.starts_with("AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8g"));
        assert!(text.ends_with("+/w=="));
        assert_eq!(decode(&text), Some(bytes));
    }

    #[test]
    fn decodes_what_it_encodes_at_every_length() {
        for length in 0..64 {
            let bytes: Vec<u8> = (0..length).map(|i: u8| i.wrapping_mul(37) ^ 0x5a).collect();
            assert_eq!(decode(&encode(&bytes)), Some(bytes), "length {length}");
        }
    }

    #[test]
    fn decodes_without_padding_as_java_does() {
        assert_eq!(decode("Zg"), Some(b"f".to_vec()));
        assert_eq!(decode("Zm8"), Some(b"fo".to_vec()));
        assert_eq!(decode("Zm9vYg"), Some(b"foob".to_vec()));
    }

    #[test]
    fn ignores_leftover_bits_as_java_does() {
        assert_eq!(decode("Zh=="), Some(b"f".to_vec()));
        assert_eq!(decode("Zm9="), Some(b"fo".to_vec()));
    }

    #[test]
    fn refuses_what_java_refuses() {
        for text in [
            "Z",
            "Zm9vY",
            "=",
            "==",
            "Zg=",
            "Zg=x",
            "Zg===",
            "Zm8==",
            "Zm9v=",
            "Zm9vZg==Zg==",
            "Zm 9v",
            "Zm9v\n",
            "Zm-_",
            "Zm9v\u{e9}",
            "Zg==\0",
        ] {
            assert_eq!(decode(text), None, "{text:?}");
        }
    }
}
