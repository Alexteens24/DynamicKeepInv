plugins {
    `java-library`
}

group = "xyz.superez"
version = "1.2.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile> {
    options.release.set(17)
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/") }
    maven { url = uri("https://repo.cwhead.dev/repository/maven-public/") }
    maven { url = uri("https://repo.artillex-studios.com/releases/") }
}

dependencies {
    // Paper API
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    testImplementation("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")

    // Vault API
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
    testImplementation("com.github.MilkBowl:VaultAPI:1.7")

    // Lands API
    compileOnly("com.github.angeschossen:LandsAPI:7.10.13")
    testImplementation("com.github.angeschossen:LandsAPI:7.10.13")

    // GriefPrevention API
    compileOnly("com.github.GriefPrevention:GriefPrevention:16.18.2")
    testImplementation("com.github.GriefPrevention:GriefPrevention:16.18.2")

    // PlaceholderAPI
    compileOnly("me.clip:placeholderapi:2.11.6")
    testImplementation("me.clip:placeholderapi:2.11.6")

    // GravesX API
    compileOnly("com.ranull:GravesX:4.9.9.2-api")
    testImplementation("com.ranull:GravesX:4.9.9.2-api")

    // AxGraves API - Jitpack fallback
    compileOnly("com.github.Artillex-Studios:AxGraves:1.24.0") {
        exclude(group = "com.artillexstudios.axapi", module = "axapi")
    }
    testImplementation("com.github.Artillex-Studios:AxGraves:1.24.0") {
        exclude(group = "com.artillexstudios.axapi", module = "axapi")
    }

    // SQLite JDBC
    compileOnly("org.xerial:sqlite-jdbc:3.45.1.0")
    testImplementation("org.xerial:sqlite-jdbc:3.45.1.0")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("com.github.seeseemelk:MockBukkit-v1.20:3.86.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    val props = mapOf("project" to mapOf("version" to version))
    inputs.properties(props)
    filteringCharset = "UTF-8"

    filesMatching(listOf("**/*.yml", "**/*.yaml", "**/*.txt")) {
        expand(props)
    }
}
