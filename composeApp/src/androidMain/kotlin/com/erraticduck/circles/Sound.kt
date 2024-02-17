package com.erraticduck.circles

enum class Sound(
    val resourcePath: String,
    val cachePath: String,
) {
    Ding("files/sounds/ding.mp3", "sounds/ding.mp3"),
    Buzzer("files/sounds/buzzer.mp3", "sounds/buzzer.mp3")
}