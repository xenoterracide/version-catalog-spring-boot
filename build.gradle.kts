import org.semver4j.Semver

// SPDX-FileCopyrightText: Copyright © 2024 - 2025 Caleb Cushing
//
// SPDX-License-Identifier: MIT

buildscript { dependencyLocking { lockAllConfigurations() } }

plugins {
  `java-base`
  alias(libs.plugins.semver)
}

group = "com.xenoterracide.gradle"

dependencyLocking {
  lockAllConfigurations()
}

version =
  providers
    .environmentVariable("IS_PUBLISHING")
    .flatMap { semver.provider }
    .getOrElse(Semver.ZERO)

// Generate a TOON file from the attached Spring Boot dependencies.html table
// Output format (per user request):
//
// gav[2]{group,artifact,version}:
//   group,artifact,version
//   ...
tasks.register("generateSpringBootDependenciesToon") {
  group = "generation"
  description = "Parses dependencies.html and writes spring-boot-4.0-dependencies.toon in TOON format (GAV)."
  doLast {
    val inputFile = project.file("dependencies.html")
    require(inputFile.exists()) { "File not found: $inputFile" }

    val html = inputFile.readText()
    val tbody =
      html
        .substringAfter("<tbody>", missingDelimiterValue = "")
        .substringBefore("</tbody>", missingDelimiterValue = "")
    require(tbody.isNotEmpty()) { "Could not locate <tbody> in dependencies.html" }

    val codeRegex = Regex("""<code>([^<]+)</code>""")
    val tokens = codeRegex.findAll(tbody).map { it.groupValues[1].trim() }.toList()
    require(tokens.size % 3 == 0) { "Expected tokens to be multiple of 3 (group, artifact, version), got ${tokens.size}" }

    val triples =
      tokens
        .chunked(3)
        .map { chunk ->
          Triple(chunk[0], chunk[1], chunk[2])
        }.sortedWith(compareBy({ it.first }, { it.second }))

    val out =
      buildString {
        appendLine("gav[2]{group,artifact,version}:")
        for (t in triples) {
          appendLine("  ${t.first},${t.second},${t.third}")
        }
      }

    val outFile = project.file("spring-boot-4.0-dependencies.toon")
    outFile.writeText(out)
    println("Wrote TOON file: $outFile")
  }
}
