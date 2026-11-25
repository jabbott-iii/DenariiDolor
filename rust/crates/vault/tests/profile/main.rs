/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! The security profile through its public API, over stand-ins for the Keystore: v1's golden
//! profile, v1's service tests, and stored profiles that are damaged or that v1 left mid-lockout.

mod damaged;
mod golden;
mod service;
mod support;
