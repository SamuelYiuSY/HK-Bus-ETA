/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2025. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2025. Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.loohp.hkbuseta.utils

import com.loohp.hkbuseta.common.appcontext.AppContext
import com.loohp.hkbuseta.common.utils.IO
import com.loohp.hkbuseta.common.utils.LocationPriority
import com.loohp.hkbuseta.common.utils.LocationResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


actual val shouldRecordLastLocation: Boolean = false

external fun getLocation(callback: (Double, Double) -> Unit, error: (Boolean) -> Unit)

actual fun checkLocationPermission(appContext: AppContext, askIfNotGranted: Boolean, callback: (Boolean) -> Unit) {
    getLocation(
        callback = { _, _ -> callback.invoke(true) },
        error = { callback.invoke(!it) }
    )
}

actual fun checkBackgroundLocationPermission(appContext: AppContext, askIfNotGranted: Boolean, callback: (Boolean) -> Unit) {
    callback.invoke(false)
}

actual fun getGPSLocationUnrecorded(appContext: AppContext, priority: LocationPriority): Deferred<LocationResult?> {
    val defer = CompletableDeferred<LocationResult?>()
    getLocation(
        callback = { lat, lng -> defer.complete(LocationResult.of(lat, lng)) },
        error = { defer.complete(if (it) null else LocationResult.FAILED_RESULT) }
    )
    return defer
}

actual fun getGPSLocationUnrecorded(appContext: AppContext, interval: Long, listener: (LocationResult) -> Unit): Deferred<() -> Unit> {
    val job = CoroutineScope(Dispatchers.IO).launch {
        while (true) {
            getLocation(
                callback = { lat, lng -> listener.invoke(LocationResult.of(lat, lng)) },
                error = { /* do nothing */ }
            )
            delay(interval)
        }
    }
    return CompletableDeferred { job.cancel() }
}

actual fun isGPSServiceEnabled(appContext: AppContext, notifyUser: Boolean): Boolean {
    return true
}