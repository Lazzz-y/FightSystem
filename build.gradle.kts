import io.izzel.taboolib.gradle.*
import java.net.URL

plugins {
    java
    `maven-publish`
    signing
    id("io.izzel.taboolib") version "2.0.22"
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("org.jetbrains.dokka") version "1.9.20"
    id("io.codearte.nexus-staging") version "0.30.0"
}

tasks.dokkaJavadoc.configure {
    suppressObviousFunctions.set(false)
    suppressInheritedMembers.set(true)
    dokkaSourceSets {
        configureEach {
            externalDocumentationLink {
                url.set(URL("https://doc.skillw.com/pouvoir/"))
            }
            externalDocumentationLink {
                url.set(URL("https://doc.skillw.com/attsystem/"))
            }
            externalDocumentationLink {
                url.set(URL("https://docs.oracle.com/javase/8/docs/api/"))
            }
            externalDocumentationLink {
                url.set(URL("https://doc.skillw.com/bukkit/"))
            }
        }
    }
}

tasks.javadoc {
    this.options {
        encoding = "UTF-8"
    }
}

val order: String? by project
val api: String? by project
task("api-add") {
    var version = project.version.toString() + (order?.let { "-$it" } ?: "")
    if (api != null && api == "common")
        version = "$version-api"
    project.version = version
}
task("info") {
    println(project.name + "-" + project.version)
    println(project.version.toString())
}
taboolib {
    description {
        contributors {
            name("Glom_")
        }
        dependencies {
            name("Pouvoir")
            name("AttributeSystem")
            name("SkillAPI").optional(true)
            name("Magic").optional(true)
            name("MythicMobs").optional(true)
        }
    }
    env {
        install(Bukkit, BukkitHook, XSeries)

        // NMS
        install( BukkitNMS, BukkitNMSDataSerializer,BukkitNMSItemTag,BukkitNMSUtil)
        // util
        install(MinecraftChat,CommandHelper,BukkitFakeOp,Metrics, BukkitUtil,BukkitNavigation)
    }
    classifier = null
    version {
        if(project.gradle.startParameter.taskNames.getOrNull(0) == "taboolibBuildApi" || api != null){
            println("api!")
            isSkipKotlinRelocate =true
            isSkipKotlin = true
        }
        taboolib = "6.2.2"
    }
}


repositories {
    mavenCentral()
    maven { url = uri("https://repo.spongepowered.org/maven") }
    maven(url = "https://mvn.lumine.io/repository/maven-public/")
    maven("https://maven.mohistmc.com/")
}
dependencies {
    dokkaHtmlPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:1.7.20")
//    compileOnly("me.deecaad:mechanicscore:2.4.9")
//    compileOnly("me.deecaad:weaponmechanics:2.6.1")
//   compileOnly("io.lumine:Mythic-Dist:5.4.1")
    compileOnly("ink.ptms.core:v12004:12004:universal")
    compileOnly("ink.ptms:nms-all:1.0.0")
    compileOnly(kotlin("stdlib"))
    compileOnly(fileTree("libs"))
}

tasks.withType<JavaCompile> {
//    destinationDir = file("E:\\Minecraft\\Server\\1.20.1 paper\\plugins")
    options.encoding = "UTF-8"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf("-Xjvm-default=all")
    }
}
tasks.javadoc {
    this.options {
        encoding = "UTF-8"
    }
}



tasks.register<Jar>("buildJavadocJar") {
    dependsOn(tasks.dokkaJavadoc)
    from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}

tasks.register<Jar>("buildSourcesJar") {
    dependsOn(JavaPlugin.CLASSES_TASK_NAME)
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}


publishing {
    repositories {
        maven {
            url = if (project.version.toString().contains("-SNAPSHOT")) {
                uri("https://s01.oss.sonatype.org/content/repositories/snapshots")
            } else {
                uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            }
            credentials {
                username = project.findProperty("username").toString()
                password = project.findProperty("password").toString()
            }
            authentication {
                create<BasicAuthentication>("basic")

            }
        }
        mavenLocal()
    }
    publications {
        create<MavenPublication>("library") {
            from(components["java"])
            artifact(tasks["buildJavadocJar"])
            artifact(tasks["buildSourcesJar"])
            version = project.version.toString()
            groupId = project.group.toString()
            pom {
                name.set(project.name)
                description.set("Bukkit Fight Engine Plugin.")
                url.set("https://github.com/Glom-c/FightSystem/")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://github.com/Glom-c/FightSystem/blob/main/LICENSE")
                    }
                }
                developers {
                    developer {
                        id.set("Skillw")
                        name.set("Glom_")
                        email.set("glom@skillw.com")
                    }
                }
                scm {
                    connection.set("scm:git:git:https://github.com/Glom-c/FightSystem.git")
                    developerConnection.set("scm:git:ssh:https://github.com/Glom-c/FightSystem.git")
                    url.set("https://github.com/Glom-c/FightSystem.git")
                }
            }
        }
    }
}

nexusStaging {
    serverUrl = "https://s01.oss.sonatype.org/service/local/"
    username = project.findProperty("username").toString()
    password = project.findProperty("password").toString()
    packageGroup = "com.skillw"
}

signing {
    sign(publishing.publications.getAt("library"))
}