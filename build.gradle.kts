plugins {
    id("java")
    id("application")
}

group = "com.rox"
version = "0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(26)
    }
}

val lwjglVersion = "3.4.3"
val lwjglNatives = "natives-linux"
val skijaVersion = "0.143.17"

dependencies {
    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
    implementation("org.lwjgl:lwjgl")
    implementation("org.lwjgl:lwjgl-glfw")
    implementation("org.lwjgl:lwjgl-opengl")
    runtimeOnly("org.lwjgl:lwjgl::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-glfw::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-opengl::$lwjglNatives")
    implementation("io.github.humbleui:skija-linux-x64:$skijaVersion")

    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass = "com.rox.ui.demo.DebugWindowDemo"
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}

tasks.named<JavaExec>("run") {
    listOf("roxui.screenshot", "roxui.screenshot.frame").forEach { key ->
        System.getProperty(key)?.let { systemProperty(key, it) }
    }
}

tasks.test {
    useJUnitPlatform()
}
