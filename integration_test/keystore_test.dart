/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

import 'package:denarii_dolor/platform/rust/api/keystore_check.dart';
import 'package:denarii_dolor/platform/rust/frb_generated.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';

/// Checks on a device that Rust reaches the Android Keystore through the Kotlin code, with v1's
/// key parameters and only test-only aliases (M3.2, ADR-0022): Dart → bridge → JNI → Kotlin →
/// Keystore.
void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  setUpAll(RustLib.init);

  /// Each check and the outcome v1's Keystore code would give, in the order the bridge runs them
  /// (`rust/crates/bridge/src/api/keystore_check.rs`).
  const expected = [
    'clean_start: ok',
    'read_without_entry: none',
    'hmac_without_device_key: KeyMissing',
    'ensure_device_key: ok',
    'hmac: 32 bytes',
    'hmac_repeats: same',
    'hmac_differs_by_message: different',
    'hmac_of_empty_message: 32 bytes',
    'ensure_device_key_again: ok',
    // Ensuring the key again keeps it, so a stored PIN slot still opens.
    'hmac_after_ensuring_again: same',
    'other_ensure_device_key: ok',
    'hmac_differs_by_device_key: different',
    'write: ok',
    'read: matches',
    'rewrite: ok',
    'read_rewritten: matches',
    'write_empty: ok',
    'read_empty: matches',
    'write_64_kib: ok',
    'read_64_kib: matches',
    'other_write: ok',
    // AES-GCM's check fails under another storage key.
    'read_with_other_storage_key: Unreadable',
    'delete_keys: ok',
    'read_without_storage_key: KeyMissing',
    // A profile without its key stays as it is: no new storage key replaces the lost one.
    'write_without_storage_key: KeyMissing',
    'read_after_refused_write: KeyMissing',
    'hmac_after_delete_keys: KeyMissing',
    'delete_keys_again: ok',
    'delete_profile: ok',
    'read_after_delete_profile: none',
    'delete_profile_again: ok',
    'write_after_delete_profile: ok',
    'read_with_new_storage_key: matches',
    'clean_end: ok',
    'read_after_clean_end: none',
  ];

  test(
    'each Keystore call gives v1\'s outcome under test-only aliases',
    () async {
      expect(
        await keystoreSelfCheck(aliasPrefix: 'denarii_test_keystore_'),
        expected,
      );
    },
  );

  test(
    'eight checks at once, on the bridge\'s worker threads, all pass',
    () async {
      final reports = await Future.wait([
        for (var run = 0; run < 8; run++)
          keystoreSelfCheck(aliasPrefix: 'denarii_test_keystore_${run}_'),
      ]);

      for (final report in reports) {
        expect(report, expected);
      }
    },
  );

  test('refuses an alias prefix that could reach a user\'s keys', () async {
    await expectLater(
      keystoreSelfCheck(aliasPrefix: 'denarii_vault_'),
      throwsA(KeystoreCheckError.notATestPrefix),
    );
    await expectLater(
      keystoreSelfCheck(aliasPrefix: 'denarii_test_../'),
      throwsA(KeystoreCheckError.invalidPrefix),
    );
  });
}
