import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Sync
import org.gradle.jvm.tasks.Jar
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.4.10"
    kotlin("plugin.serialization") version "2.4.10"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10"
    id("org.jetbrains.compose") version "1.12.2"
}

group = "com.huoleme.app"
version = "1.3.6"

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.compose.material3:material3:1.9.0")
    // Only the small core icon set is used. material-icons-extended would add
    // ~37 MB of unused vector data to every portable build.
    implementation("org.jetbrains.compose.material:material-icons-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")

    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.4")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

// Shared by `gradlew run` and the packaged launcher.
// - ClearType / DPI flags keep text sharp on scaled displays.
// - A bounded heap, SerialGC and a small coroutine pool keep the resident
//   memory of this tray app low instead of letting the JVM grab a quarter of RAM.
val runtimeJvmOptions = listOf(
    "-Dfile.encoding=UTF-8",
    "--enable-native-access=ALL-UNNAMED",
    "-Dsun.java2d.dpiaware=true",
    "-Dsun.java2d.uiScale.enabled=true",
    "-Dawt.useSystemAAFontSettings=lcd",
    "-Dswing.aatext=true",
    "-Xms32m",
    "-Xmx384m",
    "-XX:+UseSerialGC",
    "-XX:MaxMetaspaceSize=128m",
    "-XX:ReservedCodeCacheSize=64m",
    "-Dkotlinx.coroutines.scheduler.core.pool.size=1",
    "-Dkotlinx.coroutines.scheduler.max.pool.size=2"
)

compose.desktop {
    application {
        mainClass = "com.huoleme.app.MainKt"
        jvmArgs += runtimeJvmOptions

        nativeDistributions {
            // Compose's createDistributable requires WiX on Windows; use packagePortable below.
            targetFormats(TargetFormat.AppImage)
            packageName = "活了么"
            packageVersion = "1.3.6"
            description = "活了么 Windows 桌面端"
            copyright = "© 2026 活了么"
            vendor = "活了么"

            windows {
                menu = true
                shortcut = true
                perUserInstall = true
                dirChooser = true
                iconFile.set(project.file("src/main/resources/icon.ico"))
                upgradeUuid = "7f3c2f7e-9a1b-4c2d-8e3f-1a2b3c4d5e6f"
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

val jpackageInputDir = layout.buildDirectory.dir("jpackage-input")
// -PdistDir=<name> puts the image in a different folder, which is useful when
// the default dist folder is currently locked by a running instance.
val jpackageOutputDir = layout.buildDirectory.dir(
    (project.findProperty("distDir") as? String)?.takeIf { it.isNotBlank() } ?: "dist"
)
val slimRuntimeDir = layout.buildDirectory.dir("slim-runtime")

val prepareJpackageInput = tasks.register<Sync>("prepareJpackageInput") {
    group = "distribution"
    dependsOn(tasks.named("jar"))
    from(tasks.named<Jar>("jar"))
    from(configurations.runtimeClasspath)
    // Empty compatibility stubs produced by mixed lifecycle/navigationevent
    // versions: no classes, only a manifest. Skip them in the image.
    exclude("**/lifecycle-runtime-compose-desktop-2.9.6.jar")
    exclude("**/navigationevent-compose-desktop-1.1.0.jar")
    into(jpackageInputDir)
}

/**
 * Compose's createRuntimeImage produces an uncompressed 49 MB jimage.
 * Re-link the same module set with zip-6 compression: ~30 MB smaller on disk,
 * identical classes at runtime.
 */
val createSlimRuntime = tasks.register<Exec>("createSlimRuntime") {
    group = "distribution"
    description = "Build a compressed minimal JRE image with jlink."

    doFirst {
        val javaHome = System.getenv("JAVA_HOME")
        check(!javaHome.isNullOrBlank()) { "JAVA_HOME must point to a full JDK 21+." }
        val jlink = file("$javaHome/bin/jlink.exe")
        check(jlink.isFile) { "jlink.exe not found under JAVA_HOME=$javaHome" }

        delete(slimRuntimeDir.get().asFile)
        commandLine(
            jlink.absolutePath,
            "--module-path", file("$javaHome/jmods").absolutePath,
            "--add-modules", listOf(
                "java.base",
                "java.datatransfer",
                "java.xml",
                "java.prefs",
                "java.desktop",
                "java.logging",
                "jdk.crypto.ec",
                "jdk.unsupported"
            ).joinToString(","),
            "--output", slimRuntimeDir.get().asFile.absolutePath,
            "--strip-debug",
            "--no-header-files",
            "--no-man-pages",
            "--compress=zip-6"
        )
    }
}

tasks.register<Exec>("packagePortable") {
    group = "distribution"
    description = "Build a directory-based Windows app image with a compressed minimal runtime (no WiX required)."
    dependsOn(prepareJpackageInput, createSlimRuntime)

    doFirst {
        val javaHome = System.getenv("JAVA_HOME")
        check(!javaHome.isNullOrBlank()) {
            "JAVA_HOME must point to a full JDK 21+ containing bin/jpackage.exe."
        }
        val jpackage = file("$javaHome/bin/jpackage.exe")
        check(jpackage.isFile) {
            "jpackage.exe not found under JAVA_HOME=$javaHome. " +
                "Android Studio's JBR is missing jpackage; use a full JDK 21."
        }

        delete(jpackageOutputDir.get().asFile)
        val args = mutableListOf(
            jpackage.absolutePath,
            "--type", "app-image",
            "--name", "活了么",
            "--app-version", project.version.toString(),
            "--dest", jpackageOutputDir.get().asFile.absolutePath,
            "--input", jpackageInputDir.get().asFile.absolutePath,
            "--main-jar", tasks.named<Jar>("jar").get().archiveFileName.get(),
            "--main-class", "com.huoleme.app.MainKt",
            "--runtime-image", slimRuntimeDir.get().asFile.absolutePath,
            "--icon", file("src/main/resources/icon.ico").absolutePath,
            "--vendor", "活了么",
            "--description", "活了么 Windows 桌面端",
            "--copyright", "© 2026 活了么"
        )
        runtimeJvmOptions.forEach { option ->
            args += "--java-options"
            args += option
        }
        commandLine(*args.toTypedArray())
    }
}
