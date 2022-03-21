# Kotlin Compiler Plugin

Experimental Kotlin Compiler Plugin.

## Setup Kotlin Compile Testing
This project uses Kotlin 1.6.20-RC and [kotlin-compile-testing](https://github.com/tschuchortdev/kotlin-compile-testing) is not yet released with that version.

So you need to setup a [patched kotlin-compile-testing](https://github.com/asodja/kotlin-compile-testing). Install it to local maven repo:
```
git clone git@github.com:asodja/kotlin-compile-testing.git
cd kotlin-compile-testing
./gradlew publishToMavenLocal -x sign
```

