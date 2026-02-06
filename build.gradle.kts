import com.vanniktech.maven.publish.DeploymentValidation
import com.vanniktech.maven.publish.VersionCatalog
import com.xenoterracide.gradle.convention.publish.GithubPublicRepositoryConfiguration
import org.semver4j.Semver

// SPDX-FileCopyrightText: Copyright © 2024-2026 Caleb Cushing
//
// SPDX-License-Identifier: MIT

buildscript { dependencyLocking { lockAllConfigurations() } }

plugins {
  `lifecycle-base`
  `version-catalog`
  alias(libs.plugins.publish)
  alias(libs.plugins.semver)
  alias(libs.plugins.vanniktech.maven.publish)
}

group = "com.xenoterracide.gradle.vc"

dependencyLocking {
  lockAllConfigurations()
}

val isPublishing =
  providers
    .environmentVariable("IS_PUBLISHING")

version =
  isPublishing
    .flatMap { semver.provider }
    .getOrElse(Semver.ZERO)

description = "Spring Boot Version Catalog - All Dependencies"

repositoryHost(GithubPublicRepositoryConfiguration())
repositoryHost.namespace.set("xenoterracide")

publicationLegal {
  inceptionYear.set(2025)
  spdxLicenseIdentifiers.add("Apache-2.0")
}

mavenPublishing {
  // signAllPublications()
  configure(VersionCatalog())
  publishToMavenCentral(false, DeploymentValidation.VALIDATED)
}
publishing {
  publications {
    this.withType<MavenPublication>().configureEach {
      pom {
        name.set(project.name)
        description.set(project.description)
        url.set(
          repositoryHost.repository.websiteUrl
            .zip(git.tag.map { "/tree/$it" }.orElse("")) { uri, tag ->
              uri.toString() + tag
            },
        )
        scm { tag.set(git.tag) }
      }
    }
  }
}

tasks.register("stagingPath") {
  description = "Print path to staging repository for the primary publication"
  group = "Publishing"
  val stagingDir = layout.buildDirectory.dir("repo")
  val groupPath = project.group.toString().replace(".", "/")
  val artifactId = project.name
  val setVersion = version

  doLast {
    print(
      stagingDir
        .get()
        .asFile
        .resolve("$groupPath/$artifactId/$setVersion")
        .absolutePath,
    )
  }
}

catalog {
  // Build the version catalog programmatically from the TOON file.
  // We intentionally DO NOT declare versions here; consumers should use Spring Boot's platform/BOM
  // (e.g., implementation(platform("org.springframework.boot:spring-boot-dependencies:<ver>"))).
  versionCatalog {
    // implementation written by Junie
    // Helper: aliasing and registration (shared for any parser)
    fun normalizeToken(token: String): String = token.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')

    fun registerEntries(pairs: List<Pair<String, String>>) {
      val used = mutableMapOf<String, Int>()

      fun uniqueAlias(base: String): String {
        val n = used.getOrDefault(base, 0)
        return if (n == 0) {
          used[base] = 1
          base
        } else {
          val next = n + 1
          used[base] = next
          "$base-$next"
        }
      }
      val artifactCounts = pairs.groupingBy { it.second }.eachCount()
      pairs.forEach { (group, artifact) ->
        val artifactNorm = normalizeToken(artifact)
        val baseAlias =
          if ((artifactCounts[artifact] ?: 0) > 1) {
            val groupKey = normalizeToken(group)
            "$groupKey-$artifactNorm"
          } else {
            artifactNorm
          }
        val alias = uniqueAlias(baseAlias)
        library(alias, group, artifact).withoutVersion()
      }
    }

    // Parse dependencies.html leniently (no strict XML). Works with imperfect HTML.
    val htmlFile = file("dependencies.html")
    if (!htmlFile.exists()) {
      throw GradleException("dependencies.html not found at ${htmlFile.absolutePath}. Provide the Spring Boot dependencies table HTML.")
    }

    try {
      val html = htmlFile.readText()
      // Extract the first <tbody> ... </tbody> block (case-insensitive, dotall)
      val tbodyMatch = Regex("(?is)<tbody[^>]*>(.*?)</tbody>").find(html)
      val tbody =
        tbodyMatch?.groupValues?.get(1)
          ?: throw GradleException("Could not locate <tbody> ... </tbody> in dependencies.html")

      // Collect all <code>...</code> tokens in order (group, artifact, version repeating)
      val codeRegex = Regex("(?is)<code[^>]*>(.*?)</code>")
      val tokens =
        codeRegex
          .findAll(tbody)
          .map { m ->
            // Strip any nested tags and trim
            m.groupValues[1].replace(Regex("(?is)<[^>]+>"), "").trim()
          }.toList()

      if (tokens.isEmpty() || tokens.size % 3 != 0) {
        throw GradleException(
          "Expected triples of <code>group</code>, <code>artifact</code>, <code>version</code> within <tbody>. Found ${tokens.size} tokens.",
        )
      }

      val pairs =
        tokens.chunked(3).map { chunk ->
          val group = chunk[0]
          val artifact = chunk[1]
          group to artifact
        }

      require(pairs.isNotEmpty()) { "No dependency rows parsed from dependencies.html" }

      registerEntries(pairs.sortedWith(compareBy({ it.first }, { it.second })))
      logger.lifecycle("Version catalog: loaded ${pairs.size} entries from dependencies.html via lenient HTML parsing")
    } catch (e: Throwable) {
      throw GradleException("Failed to parse dependencies.html using lenient HTML parsing", e)
    }
  }
}
