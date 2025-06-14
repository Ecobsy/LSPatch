val androidSourceCompatibility: JavaVersion by rootProject.extra
val androidTargetCompatibility: JavaVersion by rootProject.extra

plugins {
    id("java-library")
}

java {
    sourceCompatibility = androidSourceCompatibility
    targetCompatibility = androidTargetCompatibility
}

dependencies {
    implementation(projects.axml)
    implementation(projects.apkzlib)
    implementation(projects.share.java)

    implementation(lspatch.commons.io)
    implementation(lspatch.beust.jcommander)
    implementation(lspatch.google.gson)
}
