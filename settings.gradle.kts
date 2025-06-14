enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        mavenLocal {
            content {
                includeGroup("io.github.libxposed")
            }
        }
    }
    versionCatalogs {
        create("libs") {
            from(files("core/gradle/libs.versions.toml"))
        }
        create("lspatch") {
            from(files("gradle/lspatch.versions.toml"))
        }
    }
}

rootProject.name = "LSPatch"
include(
    ":apache",
    ":apkzlib",
    ":axml",
    ":core",
    ":hiddenapi:bridge",
    ":hiddenapi:stubs",
    ":jar",
    ":libxposed:api",
    ":libxposed:api:checks",
    ":libxposed:service",
    ":libxposed:service:interface",
    ":manager",
    ":meta-loader",
    ":patch",
    ":patch-loader",
    ":services:daemon-service",
    ":services:manager-service",
    ":share:android",
    ":share:java",
)

project(":apache").projectDir = file("core/apache")
project(":axml").projectDir = file("core/axml")
project(":core").projectDir = file("core/core")
project(":hiddenapi:bridge").projectDir = file("core/hiddenapi/bridge")
project(":hiddenapi:stubs").projectDir = file("core/hiddenapi/stubs")
project(":libxposed:api").projectDir = file("libxposed/api/api")
project(":libxposed:api:checks").projectDir = file("libxposed/api/checks")
project(":libxposed:service").projectDir = file("libxposed/service/service")
project(":libxposed:service:interface").projectDir = file("libxposed/service/interface")
project(":services:daemon-service").projectDir = file("core/services/daemon-service")
project(":services:manager-service").projectDir = file("core/services/manager-service")

buildCache { local { removeUnusedEntriesAfterDays = 1 } }
