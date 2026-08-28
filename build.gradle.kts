// Top-level build file. Individual module build scripts declare their own
// plugin versions; this file just needs the plugin resolution strategy.
plugins {
    id("com.android.application") version "9.3.2" apply false
    id("com.google.devtools.ksp") version "2.3.11" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" apply false
}
