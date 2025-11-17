package com.ceyhun.strictguide

class GpsManager {
    // Заглушка для метода запуска GPS
    fun start() {
        // Здесь будет код для инициализации GPS
    }

    // Заглушка для метода остановки GPS
    fun stop() {
        // Здесь будет код для остановки GPS
    }

    // Заглушка для получения последнего известного местоположения
    fun getLastKnownLocation(callback: (location: String?) -> Unit) {
        // Здесь будет код для получения последнего известного местоположения
        // Временно вызываем callback с null
        callback(null)
    }
}