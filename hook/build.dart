/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

import 'package:flutter_rust_bridge_hooks/flutter_rust_bridge_hooks.dart';

/// Flutter's native-assets build hook (ADR-0014). Flutter runs it during every build and `flutter
/// test` run, for each target, and it runs `cargo build --release` on the bridge crate with
/// Flutter's NDK, then hands Flutter `libdenarii_bridge.so` to package.
void main(List<String> args) async {
  await build(args, (input, output) async {
    await const FlutterRustBridgeNativeAssetsBuilder(
      // The generated library that loads the Rust code, relative to lib/.
      assetName: 'platform/rust/frb_generated.io.dart',
      cratePath: 'rust/crates/bridge',
    ).run(input: input, output: output);
  });
}
