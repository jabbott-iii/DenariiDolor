/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.util

/**
 * A file the platform hands over: one the user chose in the system file picker, or the copy of a report offered to the share
 * sheet. Common code only passes it between the platform's pickers and the code that reads or writes the file. On Android it
 * is android.net.Uri (PlatformUri.android.kt), so Android code keeps passing Uri.
 */
expect abstract class PlatformUri {
    // android.net.Uri declares toString() abstract, so the declaration it implements has to as well.
    abstract override fun toString(): String
}
