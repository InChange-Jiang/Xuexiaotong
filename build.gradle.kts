plugins {
    id("com.android.application") version "9.2.0" apply false
    // AGP 9.0+ 内置 Kotlin，不再需要 org.jetbrains.kotlin.android
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.21" apply false
}
