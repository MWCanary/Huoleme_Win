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
version = "1.3.7 Dev"

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
    // A small initial heap and tighter caps cut the JVM's committed footprint
    // without changing what the app does. The heap still grows on demand up to
    // Xmx when the Compose window is shown.
    "-Xms12m",
    "-Xmx192m",
    "-XX:+UseSerialGC",
    // Return free heap to the OS promptly. -ShrinkHeapInSteps makes SerialGC
    // shrink to the target size in one step instead of over several GCs.
    "-XX:MaxHeapFreeRatio=10",
    "-XX:MinHeapFreeRatio=3",
    "-XX:-ShrinkHeapInSteps",
    // Smaller per-thread stacks (reminder + IPC + AWT are few and light).
    "-XX:ThreadStackSize=768",
    "-XX:MaxMetaspaceSize=96m",
    "-XX:CompressedClassSpaceSize=64m",
    "-XX:ReservedCodeCacheSize=20m",
    "-XX:TieredStopAtLevel=1",
    "-XX:CICompilerCount=2",
    "-XX:-UsePerfData",
    // Only disables external diagnostic agents attaching to the process.
    "-XX:+DisableAttachMechanism",
    "-Dsun.java2d.d3d=false",
    "-Dkotlinx.coroutines.scheduler.core.pool.size=1",
    "-Dkotlinx.coroutines.scheduler.max.pool.size=1"
)

compose.desktop {
    application {
        mainClass = "com.huoleme.app.MainKt"
        jvmArgs += runtimeJvmOptions

        nativeDistributions {
            // Compose's createDistributable requires WiX on Windows; use packagePortable below.
            targetFormats(TargetFormat.AppImage)
            packageName = "活了么"
            packageVersion = "1.3.7 Dev"
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
    // Version-conflict placeholders: these jars contain no classes, only a
    // manifest / kotlin_module / LICENSE. jpackage would otherwise add them to
    // the generated .cfg classpath and ship ~10 KB of dead weight.
    exclude("**/runtime-desktop-1.12.2.jar")
    exclude("**/runtime-saveable-desktop-1.12.2.jar")
    exclude("**/savedstate-compose-desktop-1.3.6.jar")
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
            "--compress=zip-9"
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
            // Windows jpackage requires a numeric --app-version (major.minor.micro);
            // strip any non-numeric suffix (e.g. "1.3.7 Dev" -> "1.3.7") for the PE
            // version resource. The human-visible version stays "1.3.7 Dev" everywhere else.
            "--app-version", Regex("""\d+(\.\d+)*""").find(project.version.toString())?.value
                ?: project.version.toString(),
            "--dest", jpackageOutputDir.get().asFile.absolutePath,
            "--input", jpackageInputDir.get().asFile.absolutePath,
            "--main-jar", tasks.named<Jar>("jar").get().archiveFileName.get(),
            "--main-class", "com.huoleme.app.MainKt",
            "--runtime-image", slimRuntimeDir.get().asFile.absolutePath,
            "--icon", file("src/main/resources/icon.ico").absolutePath,
            "--vendor", "活了么",
            "--description", "活了么 Windows 桌面端",
            "--copyright", "© 2026 活了么",
            // Embed a PerMonitorV2 DPI-aware manifest so Windows renders the window
            // at native resolution instead of bitmap-scaling it (the cause of the
            // previously reported blurry/fuzzy text on 125% / 150% displays).
            "--resource-dir", file("packaging").absolutePath,
        )
        runtimeJvmOptions.forEach { option ->
            args += "--java-options"
            args += option
        }
        commandLine(*args.toTypedArray())
    }

    // Trim development/optional files from the bundled runtime. None of them is
    // loaded by jpackage's launcher, and every MB counts for the installer.
    doLast {
        val runtimeRoot = jpackageOutputDir.get().asFile.resolve("活了么").resolve("runtime")
        listOf(
            "lib/jvm.lib",
            "lib/jrt-fs.jar",
            "lib/fontconfig.properties.src",
            "lib/psfont.properties.ja",
            "lib/psfontj2d.properties",
            "lib/security/public_suffix_list.dat",
            "lib/classlist",
            "legal"  // module licence texts, never read at runtime
        ).forEach { relative -> delete(runtimeRoot.resolve(relative)) }
    }
}
