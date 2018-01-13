import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "at.reisisoft"
version = "1.0-SNAPSHOT"

buildscript {
    var kotlin_version: String by extra
    kotlin_version = "1.2.10"

    repositories {
        mavenCentral()
    }

    dependencies {
        classpath(kotlinModule("gradle-plugin", kotlin_version))
    }

}

apply {
    plugin("kotlin")
    plugin("application")
}

val kotlin_version: String by extra

repositories {
    mavenCentral()
    maven {
        setUrl("https://dl.bintray.com/xenomachina/maven/")
    }
}

configure<ApplicationPluginConvention> {
    mainClassName = "at.reisisoft.reisishot.image2db.CLI"
    applicationName = "Reisishot Image 2 DB"
}

dependencies {
    //SQLite
    compile("org.xerial", "sqlite-jdbc", "3.20+")
    compile("com.xenomachina", "kotlin-argparser", "2.0+")
    compile(kotlinModule("stdlib-jdk8", kotlin_version))
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

