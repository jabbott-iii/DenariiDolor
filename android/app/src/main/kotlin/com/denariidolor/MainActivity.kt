/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor

import android.os.Bundle
import io.flutter.embedding.android.FlutterActivity

class MainActivity : FlutterActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Before Flutter starts Dart, which reaches VaultKeystore through Rust.
        VaultKeystore.attach(this)
        super.onCreate(savedInstanceState)
    }

    private companion object {
        init {
            // Loading the bridge through the Java runtime runs its JNI_OnLoad, which finds VaultKeystore with this class's
            // class loader (ADR-0022). Dart opens the same loaded library later, by name.
            System.loadLibrary("denarii_bridge")
        }
    }
}
