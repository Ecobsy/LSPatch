import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.gradle.BaseExtension
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import com.android.build.gradle.LibraryExtension

plugins {
    alias(libs.plugins.agp.lib) apply false
    alias(libs.plugins.agp.app) apply false
    alias(lspatch.plugins.kotlin.android) apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.eclipse.jgit:org.eclipse.jgit:6.3.0.202209071007-r")
    }
}

val commitCount = run {
    val repo = FileRepository(rootProject.file(".git"))
    val refId = repo.refDatabase.exactRef("refs/remotes/origin/master")?.objectId
        ?: repo.refDatabase.exactRef("HEAD")?.objectId
    if (refId != null) {
        Git(repo).log().add(refId).call().count()
    } else {
        1000 // valor por defecto si no se puede obtener
    }
}

val (coreCommitCount, coreLatestTag) = (4200 to "1.0")

// sync from https://github.com/JingMatrix/LSPosed/blob/master/build.gradle.kts
val defaultManagerPackageName by extra("org.lsposed.lspatch")
val apiCode by extra(93)
val verCode by extra(commitCount)
val verName by extra("0.7")
val coreVerCode by extra(coreCommitCount)
val coreVerName by extra(coreLatestTag)
val androidMinSdkVersion by extra(28)
val androidTargetSdkVersion by extra(35)
val androidCompileSdkVersion by extra(35)
val androidCompileNdkVersion by extra("29.0.13113456")
val androidBuildToolsVersion by extra("35.0.0")
val androidSourceCompatibility by extra(JavaVersion.VERSION_21)
val androidTargetCompatibility by extra(JavaVersion.VERSION_21)

tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}

listOf("Debug", "Release").forEach { variant ->
    tasks.register("build$variant") {
        description = "Build LSPatch with $variant"
        dependsOn(":jar:build$variant")
        dependsOn(":manager:build$variant")
    }
}

tasks.register("buildAll") {
    dependsOn("buildDebug", "buildRelease")
}

fun Project.configureBaseExtension() {
    extensions.findByType(BaseExtension::class)?.run {
        compileSdkVersion(androidCompileSdkVersion)
        ndkVersion = androidCompileNdkVersion

        externalNativeBuild.cmake {
            version = "3.28.1+"
            buildStagingDirectory = layout.buildDirectory.get().asFile
        }

        defaultConfig {
            minSdk = androidMinSdkVersion
            targetSdk = androidTargetSdkVersion
            versionCode = verCode
            versionName = verName

            signingConfigs.create("config") {
                val androidStoreFile = project.findProperty("androidStoreFile") as String?
                if (!androidStoreFile.isNullOrEmpty()) {
                    storeFile = rootProject.file(androidStoreFile)
                    storePassword = project.property("androidStorePassword") as String
                    keyAlias = project.property("androidKeyAlias") as String
                    keyPassword = project.property("androidKeyPassword") as String
                }
            }

            externalNativeBuild {
                cmake {
                    arguments += "-DEXTERNAL_ROOT=${File(rootDir.absolutePath, "core/external")}"
                    arguments += "-DCORE_ROOT=${File(rootDir.absolutePath, "core/core/src/main/jni")}"
                    abiFilters("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
                    val flags = arrayOf(
                        "-Wall",
                        "-Qunused-arguments",
                        "-Wno-gnu-string-literal-operator-template",
                        "-fno-rtti",
                        "-fvisibility=hidden",
                        "-fvisibility-inlines-hidden",
                        "-fno-exceptions",
                        "-fno-stack-protector",
                        "-fomit-frame-pointer",
                        "-Wno-builtin-macro-redefined",
                        "-Wno-unused-value",
                        "-Wno-unused-variable",
                        "-Wno-extra-semi",
                        "-Wno-shift-count-overflow",
                        "-D__FILE__=__FILE_NAME__",
                    )
                    cppFlags("-std=c++20", *flags)
                    cFlags("-std=c18", *flags)
                    arguments(
                        "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON",
                    )
                }
            }
        }

        compileOptions {
            targetCompatibility(androidTargetCompatibility)
            sourceCompatibility(androidSourceCompatibility)
        }

        buildTypes {
            all {
                signingConfig = if (signingConfigs["config"].storeFile != null) signingConfigs["config"] else signingConfigs["debug"]
            }
            named("debug") {
                externalNativeBuild {
                    cmake {
                        arguments.addAll(
                            arrayOf(
                                "-DCMAKE_CXX_FLAGS_DEBUG=-Og",
                                "-DCMAKE_C_FLAGS_DEBUG=-Og",
                            )
                        )
                    }
                }
            }
            named("release") {
                externalNativeBuild {
                    cmake {
                        val flags = arrayOf(
                            "-Wl,--exclude-libs,ALL",
                            "-ffunction-sections",
                            "-fdata-sections",
                            "-Wl,--gc-sections",
                            "-fno-unwind-tables",
                            "-fno-asynchronous-unwind-tables",
                            "-flto=thin",
                            "-Wl,--thinlto-cache-policy,cache_size_bytes=300m",
                            "-Wl,--thinlto-cache-dir=${layout.buildDirectory.get().asFile.absolutePath}/.lto-cache",
                        )
                        cppFlags.addAll(flags)
                        cFlags.addAll(flags)
                        val configFlags = arrayOf(
                            "-Oz",
                            "-DNDEBUG"
                        ).joinToString(" ")
                        arguments.addAll(
                            arrayOf(
                                "-DCMAKE_CXX_FLAGS_RELEASE=$configFlags",
                                "-DCMAKE_CXX_FLAGS_RELWITHDEBINFO=$configFlags",
                                "-DCMAKE_C_FLAGS_RELEASE=$configFlags",
                                "-DCMAKE_C_FLAGS_RELWITHDEBINFO=$configFlags",
                                "-DDEBUG_SYMBOLS_PATH=${layout.buildDirectory.get().asFile.absolutePath}/symbols",
                            )
                        )
                    }
                }
            }
        }
    }

    extensions.findByType(ApplicationExtension::class)?.lint {
        abortOnError = true
        checkReleaseBuilds = false
    }

    extensions.findByType(ApplicationAndroidComponentsExtension::class)?.let { androidComponents ->
        val optimizeReleaseRes = task("optimizeReleaseRes").doLast {
            val aapt2 = File(
                androidComponents.sdkComponents.sdkDirectory.get().asFile,
                "build-tools/${androidBuildToolsVersion}/aapt2"
            )
            val zip = java.nio.file.Paths.get(
                layout.buildDirectory.get().asFile.path,
                "intermediates",
                "optimized_processed_res",
                "release",
                "optimizeReleaseResources",
                "resources-release-optimize.ap_"
            )
            val optimized = File("${zip}.opt")
            val execResult = providers.exec {
                commandLine(
                    aapt2, "optimize",
                    "--collapse-resource-names",
                    "--enable-sparse-encoding",
                    "-o", optimized,
                    zip
                )
                isIgnoreExitValue = false
            }
            if (execResult.result.get().exitValue == 0) {
                delete(zip)
                optimized.renameTo(zip.toFile())
            }
        }

        tasks.configureEach {
            if (name == "optimizeReleaseResources") {
                finalizedBy(optimizeReleaseRes)
            }
        }
    }
}

subprojects {
    plugins.withId("com.android.application") {
        configureBaseExtension()
    }
    plugins.withId("com.android.library") {
        configureBaseExtension()
    }
    
    // Configure Java compilation options to show deprecation and unchecked warnings
    tasks.withType(JavaCompile::class.java) {
        options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
    }
}


project(":core") {
    afterEvaluate {
        if (property("android") is LibraryExtension) {
            val android = property("android") as LibraryExtension
            android.run {
                buildTypes {
                    release { proguardFiles(rootProject.file("share/lspatch-rules.pro")) }
                }
            }
        }
    }
}
