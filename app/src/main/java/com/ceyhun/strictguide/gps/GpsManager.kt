package com.ceyhun.strictguide.gps

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat

/**
 * GpsManager — безопасный и простой класс для получения координат.
 *
 * ВАЖНО: ПУБЛИЧНЫЙ API НЕ ИЗМЕНЁН
 *  - fun hasLocationPermission(): Boolean
 *  - fun startLocationUpdates(callback: (Location?) -> Unit)
 *  - fun stopLocationUpdates()
 *
 * Внутри:
 *  - проверка разрешений;
 *  - проверка включенности провайдеров (GPS / NETWORK);
 *  - мгновенная отдача lastKnownLocation, если есть;
 *  - подписка на обновления сразу от нескольких провайдеров;
 *  - защита от двойной подписки и утечек listener’а.
 */
class GpsManager(private val context: Context) {

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private var listener: LocationListener? = null

    private val TAG = "GpsManager"

    /**
     * Проверяет, выданы ли разрешения ACCESS_FINE_LOCATION или ACCESS_COARSE_LOCATION.
     */
    fun hasLocationPermission(): Boolean {
        val fineGranted = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineGranted || coarseGranted
    }

    /**
     * Запускает получение обновлений местоположения.
     *
     * 1. Проверяет разрешения.
     * 2. Проверяет, включены ли провайдеры (GPS / NETWORK).
     * 3. Если есть lastKnownLocation — сразу отдаёт её в callback.
     * 4. Подписывается на обновления от доступных провайдеров.
     *
     * Если что-то не так (нет прав, выключен GPS/NETWORK) — пишет в лог
     * и вызывает callback(null).
     */
    @Suppress("MissingPermission", "DEPRECATION")
    fun startLocationUpdates(callback: (Location?) -> Unit) {
        Log.d(TAG, "startLocationUpdates()")

        if (!hasLocationPermission()) {
            Log.e(TAG, "Нет разрешений на доступ к местоположению.")
            callback(null)
            return
        }

        val gpsEnabled =
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val networkEnabled =
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!gpsEnabled && !networkEnabled) {
            Log.e(TAG, "Ни GPS, ни NETWORK провайдер не включены.")
            callback(null)
            return
        }

        // Если уже был listener — аккуратно отписываем
        listener?.let {
            Log.w(TAG, "Уже был активный listener, удаляем его перед новой подпиской.")
            locationManager.removeUpdates(it)
        }

        listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                Log.d(TAG, "onLocationChanged: $location")
                callback(location)
            }

            @Deprecated("Deprecated in Android API 30")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                // Игнорируем устаревший callback, но подавляем warning
                Log.d(TAG, "onStatusChanged: provider=$provider status=$status")
            }

            override fun onProviderEnabled(provider: String) {
                Log.d(TAG, "Провайдер включен: $provider")
            }

            override fun onProviderDisabled(provider: String) {
                Log.w(TAG, "Провайдер отключен: $provider")
                callback(null) // Добавлено для обработки отключенного провайдера
            }
        }

        // Сначала пробуем lastKnownLocation — это даёт мгновенный результат, пока ждём живые обновления.
        val lastGps = if (gpsEnabled) {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        } else null

        val lastNetwork = if (networkEnabled) {
            locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } else null

        val bestLast = lastGps ?: lastNetwork
        if (bestLast != null) {
            Log.d(TAG, "Есть lastKnownLocation: $bestLast")
            callback(bestLast)
        } else {
            Log.d(TAG, "lastKnownLocation отсутствует, ждём живые обновления.")
        }

        // Подписываемся на доступные провайдеры
        listener?.let { l ->
            if (gpsEnabled) {
                Log.d(TAG, "Подписка на обновления от GPS_PROVIDER")
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L,   // минимум 1 секунда
                    1f,      // минимум 1 метр
                    l
                )
            }

            if (networkEnabled) {
                Log.d(TAG, "Подписка на обновления от NETWORK_PROVIDER")
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    2000L,   // можно реже, чем GPS
                    5f,      // и с чуть большей дистанцией
                    l
                )
            }
        }
    }

    /**
     * Останавливает все обновления местоположения и очищает listener.
     */
    fun stopLocationUpdates() {
        listener?.let {
            Log.d(TAG, "stopLocationUpdates(): удаляем обновления локации")
            locationManager.removeUpdates(it)
        }
        listener = null
    }
}