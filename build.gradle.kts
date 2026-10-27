import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.roundToInt

plugins {
    id("java-library")
    id("maven-publish")
    id("info.solidsoft.pitest") version "1.19.0" // mutation testing
}

group = "com.rox"
version = "0.3.0"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(26)
    }
    withSourcesJar()
    withJavadocJar()
}

// The demo (simulated NES + standalone window) lives beside the library but isn't published
val demo: SourceSet by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}
configurations[demo.implementationConfigurationName].extendsFrom(configurations.implementation.get())
configurations[demo.runtimeOnlyConfigurationName].extendsFrom(configurations.runtimeOnly.get())

val lwjglVersion = "3.4.3"
val lwjglNatives = "natives-linux"
val skijaVersion = "0.143.17"

dependencies {
    // Skija and LWJGL types (Canvas, GLFW keys) are part of the public API
    api(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
    api("org.lwjgl:lwjgl")
    api("org.lwjgl:lwjgl-glfw")
    api("org.lwjgl:lwjgl-opengl")
    runtimeOnly("org.lwjgl:lwjgl::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-glfw::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-opengl::$lwjglNatives")
    api("io.github.humbleui:skija-linux-x64:$skijaVersion")

    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.register<JavaExec>("run") {
    description = "Runs the standalone memory debugger demo on simulated NES data."
    group = "application"
    classpath = demo.runtimeClasspath
    mainClass = "com.rox.roundabout.demo.DebugWindowDemo"
    javaLauncher = javaToolchains.launcherFor(java.toolchain)
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    listOf("roundabout.screenshot", "roundabout.screenshot.frame").forEach { key ->
        System.getProperty(key)?.let { systemProperty(key, it) }
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging { exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
}

// Mutation testing: `./gradlew pitest`, report in build/reports/pitest. Narrow it while iterating with
// e.g. `-PpitestScope=com.rox.roundabout.memory.*`
val pitestScope = (findProperty("pitestScope") as String?)?.split(",") ?: listOf("com.rox.roundabout.*")

pitest {
    junit5PluginVersion = "1.2.2"
    targetClasses = pitestScope
    targetTests = pitestScope
    // needs a display and an OpenGL context
    excludedClasses = listOf("com.rox.roundabout.window.GlfwWindow*")
    jvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
    threads = Runtime.getRuntime().availableProcessors()
    outputFormats = listOf("HTML", "XML")
    timestampedReports = false
}

// Writes build/reports/pitest-badge/pitest.json, a shields.io endpoint badge; CI publishes it to GitHub Pages
tasks.register("pitestBadge") {
    description = "Writes the mutation score as a shields.io badge."
    dependsOn("pitest")
    doLast {
        val mutations = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(file("build/reports/pitest/mutations.xml"))
            .getElementsByTagName("mutation")
        val statuses = (0 until mutations.length).map { mutations.item(it).attributes.getNamedItem("status").nodeValue }
        // Pitest's "detected" statuses: mutants a test caught (not merely ran)
        val detected = statuses.count { it in setOf("KILLED", "TIMED_OUT", "NON_VIABLE", "MEMORY_ERROR", "RUN_ERROR") }
        val score = if (statuses.isEmpty()) 0 else (100.0 * detected / statuses.size).roundToInt()
        val color = when {
            score >= 80 -> "brightgreen"
            score >= 60 -> "yellow"
            else -> "red"
        }
        val badge = file("build/reports/pitest-badge/pitest.json")
        badge.parentFile.mkdirs()
        badge.writeText("""{"schemaVersion": 1, "label": "mutation coverage", "message": "$score%", "color": "$color"}""")
        println("Mutation coverage: $score%")
    }
}

publishing {
    publications {
        create<MavenPublication>("library") {
            artifactId = "roundabout"
            from(components["java"])
            pom {
                name = "Roundabout"
                description = "GPU-rendered, immediate-mode UI toolkit for real-time debugging tools (Skija + LWJGL/GLFW)"
                url = "https://github.com/rossdrew/roundabout"
                licenses {
                    license {
                        name = "MIT License"
                        url = "https://opensource.org/licenses/MIT"
                    }
                }
            }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/rossdrew/roundabout")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
                password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

// Fail on broken links, HTML and syntax. "missing" is off: it would demand @param/@return on every
// method, which one-line comments here make redundant.
tasks.javadoc {
    (options as StandardJavadocDocletOptions).apply {
        addBooleanOption("Xdoclint:all,-missing", true)
        addBooleanOption("Xwerror", true)
    }
}
