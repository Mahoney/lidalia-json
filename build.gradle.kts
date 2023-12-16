import org.gradle.internal.deprecation.DeprecatableConfiguration
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import net.idlestate.gradle.duplicates.CheckDuplicateClassesTask
import com.github.gundy.semver4j.model.Version

plugins {
  kotlin("jvm") version "1.9.21"

  id("com.autonomousapps.dependency-analysis") version "1.28.0"
  id("org.jmailen.kotlinter") version "4.1.0"
  id("com.dorongold.task-tree") version "2.1.1"
  id("com.github.ben-manes.versions") version "0.50.0"
  id("net.idlestate.gradle-duplicate-classes-check") version "1.2.0"
}

buildscript {
  repositories {
    mavenCentral()
  }
  dependencies {
    "classpath"(group = "com.github.gundy", name = "semver4j", version = "0.16.4")
  }
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

kotlin {
  jvmToolchain(17)
}

dependencyLocking {
  lockAllConfigurations()
}

testing {
  suites {
    @Suppress("UnstableApiUsage")
    val test by getting(JvmTestSuite::class) {
      useJUnitJupiter("5.9.3")
    }
  }
}

val test by tasks.named<Test>("test") {
  systemProperty("kotest.framework.classpath.scanning.config.disable", "true")
  systemProperty("kotest.framework.classpath.scanning.autoscan.disable", "true")
}

kotlinter {
  reporters = arrayOf("checkstyle", "plain", "html")
}

tasks {
  check {
    dependsOn("buildHealth")
    dependsOn("installKotlinterPrePushHook")
  }
  checkForDuplicateClasses {
    excludeConfigurations(
      "projectHealth",
      "projectHealthClasspath",
      "projectHealthElements",
      "testImplementationDependenciesMetadata",
      "kotlinBuildToolsApiClasspath",
    )
  }
}

fun CheckDuplicateClassesTask.excludeConfigurations(vararg configurationNames: String) {
  val configs = configurations.filterNot { it.name in configurationNames }
  configurationsToCheck(
    configs,
  )
}

dependencyAnalysis {
  issues {
    // configure for all projects
    all {
      // set behavior for all issue types
      onAny {
        severity("fail")
        exclude("org.jetbrains.kotlin:kotlin-stdlib")
      }
    }
  }
}

tasks.withType<DependencyUpdatesTask> {
  rejectVersionIf {
    candidate.version.isPreRelease()
  }
}

fun String.isPreRelease(): Boolean = try {
  Version.fromString(this).preReleaseIdentifiers.isNotEmpty()
} catch (e: IllegalArgumentException) {
  false
}

fun Configuration.isDeprecated() = this is DeprecatableConfiguration

fun ConfigurationContainer.resolveAll() = this
  .filter { it.isCanBeResolved && !it.isDeprecated() }
  .map { it.incoming.artifactView { isLenient = true } }
  .forEach { it.files.files }

tasks.register("downloadDependencies") {
  doLast {
    configurations.resolveAll()
    buildscript.configurations.resolveAll()
  }
}
