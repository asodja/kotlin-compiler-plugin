plugins {
    id("compiler-plugin.kotlin-common-conventions")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.20-RC")
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.6.20-RC")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.4.8-SNAPSHOT")
}
