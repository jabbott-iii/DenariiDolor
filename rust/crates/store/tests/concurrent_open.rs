/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! Stores opened on many threads at the same moment, in a process of their own.
//!
//! SQLCipher's process-wide setup has two races, which `Store::open` works around with a lock of
//! its own: a `PRAGMA key` that runs before SQLCipher has finished initializing fails with
//! "sqlcipher not initialized", and two `PRAGMA cipher_log` statements at once can call
//! `fclose(NULL)` and crash the process (`docs/specs/store.md`, "Opening"). Both can happen only at
//! the first opens in a process, so this test is the only one in its file: Cargo runs each test file
//! as its own process. The races are rare, so a pass here proves little on its own.

use std::fs;
use std::io;
use std::path::{Path, PathBuf};
use std::sync::{Arc, Barrier};
use std::thread;

use denarii_store::{DatabaseKey, Store};

const FIXTURE: &str = concat!(
    env!("CARGO_MANIFEST_DIR"),
    "/tests/fixtures/v1-sqlcipher-android-4.19.1.db"
);

/// The fixture's key, a test value: the bytes 0x00 to 0x1f.
const FIXTURE_KEY: [u8; DatabaseKey::LEN] = [
    0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f,
    0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f,
];

const THREADS: usize = 64;

/// A copy of the fixture in a fresh folder of its own.
fn fixture_copy(name: &str) -> io::Result<PathBuf> {
    let dir = Path::new(env!("CARGO_TARGET_TMPDIR")).join(format!("store-concurrent-{name}"));
    match fs::remove_dir_all(&dir) {
        Err(error) if error.kind() != io::ErrorKind::NotFound => return Err(error),
        _ => {}
    }
    fs::create_dir_all(&dir)?;
    let path = dir.join("denarii_dolor.db");
    fs::copy(FIXTURE, &path)?;
    Ok(path)
}

#[test]
fn stores_opened_on_many_threads_at_once_all_open() {
    let paths: Vec<PathBuf> = (0..THREADS)
        .map(|i| fixture_copy(&i.to_string()).unwrap())
        .collect();
    let start = Arc::new(Barrier::new(THREADS));
    let workers: Vec<_> = paths
        .into_iter()
        .map(|path| {
            let start = Arc::clone(&start);
            thread::spawn(move || {
                start.wait();
                let store = Store::open(&path, &DatabaseKey::from_bytes(FIXTURE_KEY)).unwrap();
                assert_eq!(store.user_version().unwrap(), 2);
                store.close().unwrap();
            })
        })
        .collect();
    for worker in workers {
        worker.join().unwrap();
    }
}
