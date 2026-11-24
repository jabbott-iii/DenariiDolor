/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! Opening the database. The fixture is a file that v1's own code wrote with `sqlcipher-android`
//! 4.19.1 on an emulator (`tests/fixtures/README.md`); every test opens a copy of it, so the
//! committed file never changes.

use std::fs;
use std::io;
use std::path::{Path, PathBuf};
use std::sync::Arc;
use std::thread;

use denarii_store::{DatabaseKey, Store, StoreError};

const FIXTURE: &str = concat!(
    env!("CARGO_MANIFEST_DIR"),
    "/tests/fixtures/v1-sqlcipher-android-4.19.1.db"
);

/// The fixture's key, a test value: the bytes 0x00 to 0x1f.
const FIXTURE_KEY: [u8; DatabaseKey::LEN] = [
    0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f,
    0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f,
];

/// A fresh folder for one test, under Cargo's temporary folder for integration tests.
fn scratch_dir(test: &str) -> io::Result<PathBuf> {
    let dir = Path::new(env!("CARGO_TARGET_TMPDIR")).join(format!("store-open-{test}"));
    match fs::remove_dir_all(&dir) {
        Err(error) if error.kind() != io::ErrorKind::NotFound => return Err(error),
        _ => {}
    }
    fs::create_dir_all(&dir)?;
    Ok(dir)
}

fn copy_fixture(dir: &Path) -> io::Result<PathBuf> {
    let path = dir.join("denarii_dolor.db");
    fs::copy(FIXTURE, &path)?;
    Ok(path)
}

fn fixture_key() -> DatabaseKey {
    DatabaseKey::from_bytes(FIXTURE_KEY)
}

#[test]
fn opens_the_database_v1_wrote_at_schema_version_2() {
    let dir = scratch_dir("opens_the_database_v1_wrote_at_schema_version_2").unwrap();
    let store = Store::open(copy_fixture(&dir).unwrap(), &fixture_key()).unwrap();
    assert_eq!(store.user_version().unwrap(), 2);
    store.close().unwrap();
}

#[test]
fn opening_reading_and_closing_leave_every_byte_of_the_file_unchanged() {
    let dir =
        scratch_dir("opening_reading_and_closing_leave_every_byte_of_the_file_unchanged").unwrap();
    let path = copy_fixture(&dir).unwrap();
    let store = Store::open(&path, &fixture_key()).unwrap();
    assert_eq!(store.user_version().unwrap(), 2);
    store.close().unwrap();
    assert_eq!(fs::read(&path).unwrap(), fs::read(FIXTURE).unwrap());
}

#[test]
fn rejects_another_key_and_leaves_the_file_unchanged() {
    let dir = scratch_dir("rejects_another_key_and_leaves_the_file_unchanged").unwrap();
    let path = copy_fixture(&dir).unwrap();
    let mut wrong = FIXTURE_KEY;
    wrong[31] ^= 1; // One bit away from the right key.
    let result = Store::open(&path, &DatabaseKey::from_bytes(wrong));
    assert!(matches!(result, Err(StoreError::KeyRejected)), "{result:?}");
    assert_eq!(fs::read(&path).unwrap(), fs::read(FIXTURE).unwrap());
}

#[test]
fn rejects_a_file_that_is_not_a_database() {
    let dir = scratch_dir("rejects_a_file_that_is_not_a_database").unwrap();
    let path = dir.join("denarii_dolor.db");
    let noise: Vec<u8> = (0..8192_u32)
        .map(|i| u8::try_from(i.wrapping_mul(2_654_435_761) >> 24).unwrap())
        .collect();
    fs::write(&path, &noise).unwrap();
    let result = Store::open(&path, &fixture_key());
    assert!(matches!(result, Err(StoreError::KeyRejected)), "{result:?}");
    assert_eq!(fs::read(&path).unwrap(), noise);
}

#[test]
fn rejects_a_copy_cut_off_inside_its_first_page() {
    let dir = scratch_dir("rejects_a_copy_cut_off_inside_its_first_page").unwrap();
    let path = dir.join("denarii_dolor.db");
    let fixture = fs::read(FIXTURE).unwrap();
    for length in [15, 16, 100, 4095] {
        fs::write(&path, &fixture[..length]).unwrap();
        let result = Store::open(&path, &fixture_key());
        assert!(
            matches!(result, Err(StoreError::KeyRejected)),
            "{length} bytes: {result:?}"
        );
        assert_eq!(fs::read(&path).unwrap(), &fixture[..length]);
    }
}

#[test]
fn reports_a_copy_cut_off_after_its_first_page_as_malformed() {
    let dir = scratch_dir("reports_a_copy_cut_off_after_its_first_page_as_malformed").unwrap();
    let path = dir.join("denarii_dolor.db");
    let fixture = fs::read(FIXTURE).unwrap();
    // The first page opens with the key, but the schema's other pages are missing.
    for length in [4096, 8192] {
        fs::write(&path, &fixture[..length]).unwrap();
        let result = Store::open(&path, &fixture_key());
        assert!(
            matches!(
                &result,
                Err(StoreError::Sqlite {
                    source: rusqlite::Error::SqliteFailure(error, _),
                    ..
                }) if error.code == rusqlite::ErrorCode::DatabaseCorrupt
            ),
            "{length} bytes: {result:?}"
        );
        assert_eq!(fs::read(&path).unwrap(), &fixture[..length]);
    }
}

/// SQLite reads an empty file, and a 1-byte one too, as an empty database, which is what `open`
/// reports. M2.2 must not take the 1-byte file for a new install (the roadmap's note under M2.2).
#[test]
fn opens_an_empty_or_one_byte_file_as_an_empty_database_without_writing_to_it() {
    let dir = scratch_dir("opens_an_empty_or_one_byte_file_as_an_empty_database").unwrap();
    let path = dir.join("denarii_dolor.db");
    let fixture = fs::read(FIXTURE).unwrap();
    for length in [0, 1] {
        fs::write(&path, &fixture[..length]).unwrap();
        let store = Store::open(&path, &fixture_key()).unwrap();
        assert_eq!(store.user_version().unwrap(), 0, "{length} bytes");
        store.close().unwrap();
        assert_eq!(fs::read(&path).unwrap(), &fixture[..length]);
    }
}

#[test]
fn rejects_a_fixture_with_one_byte_of_its_first_page_changed() {
    let dir = scratch_dir("rejects_a_fixture_with_one_byte_of_its_first_page_changed").unwrap();
    let path = dir.join("denarii_dolor.db");
    let mut bytes = fs::read(FIXTURE).unwrap();
    bytes[100] ^= 0x80;
    fs::write(&path, &bytes).unwrap();
    let result = Store::open(&path, &fixture_key());
    assert!(matches!(result, Err(StoreError::KeyRejected)), "{result:?}");
}

#[test]
fn one_store_serves_several_threads() {
    let dir = scratch_dir("one_store_serves_several_threads").unwrap();
    let store = Arc::new(Store::open(copy_fixture(&dir).unwrap(), &fixture_key()).unwrap());
    let workers: Vec<_> = (0..8)
        .map(|_| {
            let store = Arc::clone(&store);
            thread::spawn(move || {
                for _ in 0..50 {
                    assert_eq!(store.user_version().unwrap(), 2);
                }
            })
        })
        .collect();
    for worker in workers {
        worker.join().unwrap();
    }
    Arc::into_inner(store).unwrap().close().unwrap();
}

#[test]
fn store_and_its_error_can_cross_threads() {
    fn assert_send_sync<T: Send + Sync>() {}
    assert_send_sync::<Store>();
    assert_send_sync::<StoreError>();
    assert_send_sync::<DatabaseKey>();
}
