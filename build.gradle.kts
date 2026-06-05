plugins {
    id("java")
    id("com.gradleup.shadow") version "9.4.1"
}

group = "com.cesarcosmico"
version = "2.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
    maven {
        name = "papermc-repo"
        setUrl("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "extendedclip-repo"
        setUrl("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.53-stable")
    compileOnly("me.clip:placeholderapi:2.12.2")
    implementation("com.saicone:rtag:1.5.16")
    implementation("com.saicone:rtag-item:1.5.16")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveClassifier.set("")
    mergeServiceFiles()
    relocate("com.saicone.rtag", "com.cesarcosmico.switchskin.libs.rtag")
}

tasks.named("build") {
    dependsOn("shadowJar")
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("paper-plugin.yml") {
        expand("version" to project.version)
    }
}
