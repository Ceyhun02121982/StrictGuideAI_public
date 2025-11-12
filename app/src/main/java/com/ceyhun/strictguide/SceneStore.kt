package com.ceyhun.strictguide

class SceneStore {
    @Volatile
    private var lastScene: String = "камера пока ничего не распознала"

    fun update(scene: String) {
        lastScene = scene
    }

    fun getScene(): String = lastScene
}
