plugins {
    java
    kotlin("jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    compileOnly("com.android.tools.lint:lint-api:31.3.2")
    compileOnly("com.android.tools.lint:lint-checks:31.3.2")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:1.9.23")
}

