import org.gradle.internal.deprecation.DeprecatableConfiguration
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import net.idlestate.gradle.duplicates.CheckDuplicateClassesTask
import com.github.gundy.semver4j.model.Version

plugins {
  kotlin("jvm") version "1.9.21"

  alias(libs.plugins.dependencyAnalysis)
  alias(libs.plugins.kotlinter)
  alias(libs.plugins.taskTree)
  alias(libs.plugins.versions)
  alias(libs.plugins.duplicateClasses)
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
      useJUnitJupiter()
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

fun Configuration.isDeprecated() = this is DeprecatableConfiguration && this.isDeprecatedForResolution

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
