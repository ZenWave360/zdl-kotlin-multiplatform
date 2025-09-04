plugins {
    kotlin("multiplatform") version "2.0.21"
    id("com.strumenta.antlr-kotlin") version "1.0.3"
}

repositories {
    mavenCentral()
}

kotlin {
    jvm()
    js(IR) {
        nodejs()
        binaries.library()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("com.strumenta:antlr-kotlin-runtime:1.0.3")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
            }
        }
        val jvmMain by getting {
            dependencies {
//                implementation("com.jayway.jsonpath:json-path:2.9.0")
            }
        }
        val jvmTest by getting
        val jsMain by getting
        val jsTest by getting {
            dependencies {
                implementation(npm("fs", "0.0.1-security"))
                implementation("org.jetbrains.kotlin-wrappers:kotlin-node:18.16.12-pre.610")
            }
        }
    }
}

val generateKotlinGrammarSource = tasks.register<com.strumenta.antlrkotlin.gradle.AntlrKotlinTask>("generateKotlinGrammarSource") {
    dependsOn("cleanGenerateKotlinGrammarSource")

    source = fileTree(layout.projectDirectory.dir("src/commonMain/antlr")) {
        include("**/*.g4")
    }

    val pkgName = "io.zenwave360.zdl.antlr"
    packageName = pkgName
    arguments = listOf("-visitor")

    val outDir = "generatedAntlr/${pkgName.replace(".", "/")}"
    outputDirectory = layout.buildDirectory.dir(outDir).get().asFile
}

kotlin {
    sourceSets {
        val commonMain by getting
        commonMain.kotlin.srcDir(generateKotlinGrammarSource)
    }
}

