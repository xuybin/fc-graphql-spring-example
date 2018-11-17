import com.moowork.gradle.node.npm.NpmTask
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

buildscript {
    mapOf(
        "fc-graphql-spring" to "0.1.0"
    ).entries.forEach {
        extra.set(it.key, parent?.extra?.run { if (has(it.key)) get(it.key) else null } ?: it.value)
    }
}

plugins {
    application
    kotlin("jvm") version "1.3.10"
    kotlin("plugin.spring") version "1.3.10"
    id("com.moowork.node") version "1.2.0"
}


version = "0.1.0"
group = "com.github.xuybin"

application {
    mainClassName = "com.github.xuybin.fc.graphql.spring.MainKt"
}

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    compile("com.github.xuybin:fc-graphql-spring:${extra["fc-graphql-spring"]}")
}

node {
    version = "8.12.0"
    distBaseUrl = "https://npm.taobao.org/mirrors/node"
    download = true
}

tasks {
    register("funDeploy", NpmTask::class) {
        group = "Node"
        project.copy {
            from("${project.projectDir}")
            include("*.tpl.yml")
            rename{
                it.replace(".tpl.yml",".yml")
            }
            expand(project.properties)
            into("${project.projectDir}")
        }
        doFirst {
            setNpmCommand("install", "@alicloud/fun", "-g")
        }
        doLast {
            Files.walkFileTree(Paths.get("${node.workDir}"), object : SimpleFileVisitor<java.nio.file.Path>() {
                override fun visitFile(file: java.nio.file.Path, attrs: BasicFileAttributes): FileVisitResult {
                    if ("$file".endsWith(
                            org.gradle.internal.os.OperatingSystem.current().getScriptName("fun").replace(
                                ".bat",
                                ".cmd"
                            )
                        )
                    ) {
                        exec {
                            commandLine("$file", "-v")
                        }
                        exec {
                            commandLine("$file", "deploy")
                        }
                        return FileVisitResult.TERMINATE
                    } else {
                        return FileVisitResult.CONTINUE
                    }
                }
            })
        }
    }
    jar {
        manifest {
            attributes(
                mapOf(
                    "Main-Class" to project.application.mainClassName
                    , "Implementation-Title" to project.name
                    , "Implementation-Version" to project.version
                )
            )
        }
        into("lib") {
            from(configurations.compile.get().resolve().map { if (it.isDirectory) it else it })
        }
    }

    processResources {
        filesMatching("**/defaults.properties") {
            expand(project.properties)
        }
        filesMatching("logback.xml") {
            expand(project.properties)
        }
    }

    compileKotlin {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "1.8"
        }
    }

    compileTestKotlin {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "1.8"
        }
        serviceLoaderGen("com.github.xuybin.fc.graphql.GSchema")
        serviceLoaderGen("com.github.xuybin.fc.graphql.GQuery")
        serviceLoaderGen("com.github.xuybin.fc.graphql.GMutation")
        serviceLoaderGen("com.github.xuybin.fc.graphql.GSubscription")
    }
}

fun serviceLoaderGen(serviceName: String) {
    val servicePath =
        "${project.buildDir}${File.separator}resources${File.separator}main${File.separator}META-INF${File.separator}services${File.separator}$serviceName"
    var serviceImpls = mutableSetOf<String>()
    project.sourceSets["main"].allSource.forEach {
        if (it.name.endsWith(".kt")) {
            val srcTxt = it.readText()
            var packageName: String = ""
            "package\\s+([a-zA-Z_0-9.]+)\\s+".toRegex().findAll(srcTxt).forEach {
                packageName = it.groupValues[1]
                return@forEach
            }
            "class\\s+([a-zA-Z_0-9\\(\\)]+)\\s*:\\s*[a-zA-Z_0-9\\(\\)|\\s|,]*${serviceName.substringAfterLast(".")}".toRegex()
                .findAll(srcTxt)
                .forEach {
                    serviceImpls.add("$packageName.${it.groupValues[1]}")
                }
        }
    }
    if (serviceImpls.size > 0) {
        File(servicePath).also {
            it.parentFile.mkdirs()
            println("由${serviceName}的实现类生成${it}")
            it.writeText(serviceImpls.joinToString("\n"))
        }
    }
}
