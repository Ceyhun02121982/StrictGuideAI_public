package com.ceyhun.strictguide.permissions

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.util.Log

/**
 * PermissionsManager предоставляет функции для проверки разрешений приложения.
 */
object PermissionsManager {

    private const val TAG = "PermissionsManager"

    /**
     * Проверяет, есть ли разрешение на доступ к GPS.
     *
     * @param context Контекст приложения.
     * @return true, если разрешение предоставлено, иначе false.
     */
    fun checkGpsPermission(context: Context): Boolean {
        return checkPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
    }

    /**
     * Проверяет, есть ли разрешение на доступ к камере.
     *
     * @param context Контекст приложения.
     * @return true, если разрешение предоставлено, иначе false.
     */
    fun checkCameraPermission(context: Context): Boolean {
        return checkPermission(context, android.Manifest.permission.CAMERA)
    }

    /**
     * Проверяет, есть ли разрешение на доступ к микрофону.
     *
     * @param context Контекст приложения.
     * @return true, если разрешение предоставлено, иначе false.
     */
    fun checkMicPermission(context: Context): Boolean {
        return checkPermission(context, android.Manifest.permission.RECORD_AUDIO)
    }

    private fun checkPermission(context: Context, permission: String): Boolean {
        val isGranted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "$permission granted: $isGranted")
        return isGranted
    }
}