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
        val isGranted = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "GPS Permission granted: $isGranted")
        return isGranted
    }

    /**
     * Проверяет, есть ли разрешение на доступ к камере.
     *
     * @param context Контекст приложения.
     * @return true, если разрешение предоставлено, иначе false.
     */
    fun checkCameraPermission(context: Context): Boolean {
        val isGranted = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "Camera Permission granted: $isGranted")
        return isGranted
    }

    /**
     * Проверяет, есть ли разрешение на доступ к микрофону.
     *
     * @param context Контекст приложения.
     * @return true, если разрешение предоставлено, иначе false.
     */
    fun checkMicPermission(context: Context): Boolean {
        val isGranted = ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "Microphone Permission granted: $isGranted")
        return isGranted
    }
}