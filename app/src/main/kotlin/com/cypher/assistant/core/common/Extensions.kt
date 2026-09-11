package com.cypher.assistant.core.common

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

// ─────────────────────────────────────────────────────────────────────────────
// Context extensions
// ─────────────────────────────────────────────────────────────────────────────

/** Attempts to start an Activity; returns false if no app can handle the Intent. */
fun Context.safeStartActivity(intent: Intent): Boolean = try {
    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    true
} catch (e: Exception) {
    false
}

/** Returns true if [packageName] is installed on this device. */
fun Context.isPackageInstalled(packageName: String): Boolean = try {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(packageName, 0)
    }
    true
} catch (_: PackageManager.NameNotFoundException) {
    false
}

/** Shows a short Toast on the main thread. */
fun Context.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

// ─────────────────────────────────────────────────────────────────────────────
// Flow extensions
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Wraps each emission in [CypherResult.Success], catching any upstream exception
 * and emitting it as [CypherResult.Error]. Useful for mapping DB / network flows
 * directly into UI-ready state.
 */
fun <T> Flow<T>.asResult(): Flow<CypherResult<T>> =
    map<T, CypherResult<T>> { CypherResult.Success(it) }
        .catch { emit(CypherResult.Error(it.message ?: "Unknown error", it)) }

// ─────────────────────────────────────────────────────────────────────────────
// String extensions
// ─────────────────────────────────────────────────────────────────────────────

/** Capitalizes the first character; no-op on empty strings. */
fun String.capitalizeFirst(): String =
    if (isEmpty()) this else this[0].uppercaseChar() + substring(1)

/** Truncates to [maxLength] characters, appending "…" if truncated. */
fun String.truncate(maxLength: Int): String =
    if (length <= maxLength) this else take(maxLength - 1) + "…"
