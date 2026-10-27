import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("fabric-loom") version "1.10.5"
    kotlin("jvm") version "2.2.20"
}

version = property("mod_version") as String
group = property("maven_group") as String

base {
    archivesName.set(property("archives_base_name") as String)
}

repositories {
    maven("https://maven.parchmentmc.org") { name = "ParchmentMC" }
    maven("https://maven.impactdev.net/repository/development/") { name = "Cobblemon" }
    maven("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/") { name = "GeckoLib" }
    maven("https://api.modrinth.com/maven") { name = "Modrinth" }
    mavenCentral()
}

loom {
    splitEnvironmentSourceSets()

    mods {
        register("shifustrike") {
            sourceSet(sourceSets["main"])
            sourceSet(sourceSets["client"])
        }
    }

    runs {
        create("gametest") {
            server()
            name("GameTest")
            vmArg("-Dfabric-api.gametest")
            vmArg("-Dfabric-api.gametest.report-file=${layout.buildDirectory.get()}/gametest/junit.xml")
            runDir("build/gametest")
        }
    }
}

val devIcu: Configuration by configurations.creating {
    isTransitive = false
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-${property("minecraft_version")}:${property("parchment_version")}@zip")
    })
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin_version")}")
    modImplementation("com.cobblemon:fabric:${property("cobblemon_version")}")

    // Só no ambiente de dev: o PointBlank (All Rights Reserved) nunca entra no nosso jar.
    modLocalRuntime("software.bernie.geckolib:geckolib-fabric-1.21.1:${property("geckolib_version")}")
    modLocalRuntime(files("libs/pointblank-fabric-1.21-2.2.0.jar"))
    // Flan (claims) para testar PvP dentro de claim. A 1.12.x do servidor foi feita com um Loom mais novo que
    // o nosso e não carrega aqui; a 1.11.3 tem o mesmo bloqueio de dano (Entity.isInvulnerableTo → preventDamage).
    modLocalRuntime("maven.modrinth:flan:1.21.1-1.11.3-fabric")

    // O Loom tira do runServer as bibliotecas que o Minecraft só usa no cliente (ICU4J é uma delas),
    // mas o Showdown do Cobblemon precisa dela. Em produção o Cobblemon traz a própria cópia embutida (71.1).
    devIcu("com.ibm.icu:icu4j:71.1")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
    options.encoding = "UTF-8"
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${project.base.archivesName.get()}" }
    }
}

// GameTest sempre num mundo novo (o Flan guarda dados de jogadores falsos e quebra na próxima subida; claims não sobram)
tasks.matching { it.name == "runGametest" }.configureEach {
    doFirst { delete(layout.buildDirectory.dir("gametest/world")) }
}

// Permite mandar comandos pelo console ao rodar ./gradlew runServer; o Showdown precisa do ICU4J também no gametest
tasks.matching { it.name == "runServer" || it.name == "runGametest" }.configureEach {
    (this as JavaExec).standardInput = System.`in`
    classpath(devIcu)
}
