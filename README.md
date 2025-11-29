<!--
SPDX-FileCopyrightText: Copyright © 2024 - 2025 Caleb Cushing

SPDX-License-Identifier: CC-BY-NC-4.0
-->

# README

## Usage

```kt
// settings.gradle.kts
dependencyResolutionManagement {
    versionCatalogs {
        create("sb") {
            from("com.xenoterracide.gradle.vc:version-catalog-spring-boot:3.5.0")
        }
    }
}
```

```kt
// build.gradle.kts
dependencies {
    // or however you want we aren't generating the BOM.
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.5.0"))
    implementation(sb.spring.boot.starter.data.jpa.test)
}
```

See the [version catalog documentation](https://docs.gradle.org/current/userguide/version_catalogs.html) for more details.

Available libraries can be found in the [Spring Boot documentation](https://docs.spring.io/spring-boot/3.5/appendix/dependency-versions/coordinates.html). Since this catalog does not supply versions, you must still use the `spring-boot-dependencies` BOM.

## FAQ

### Versions

The major and minor versions of this catalog align with the Spring Boot version. The patch version is incremented for updates to the catalog itself.

### Gradle Support

Gradle 7.2 or later is required. Stable support for version catalogs was introduced in Gradle 7.4.

## Development

### Languages

[asdf](https://asdf-vm.com) is suggested, you can use whatever you'd like to get

- Java 17+
- NodeJs
- Python 3.11+

add a way to export these to your `PATH` in your `~/.profile`

if you have `asdf` installed, you can run `asdf install` to install the versions`

### Build Tools

- [Gradle](https://docs.gradle.org/current/userguide/command_line_interface.html)
- [Yarn 4 - formatting commit hooks](https://yarnpkg.com/getting-started/install) (via Corepack)
- [PIP - reuse license header commit hooks](https://pip.pypa.io/en/stable/)

#### Fetching Dependencies

Yarn setup and manual postinstall:

```sh
# Enable Corepack, install Node dev tools, run postinstall, then verify Gradle deps
corepack enable
yarn install --immutable --inline-builds --check-resolutions
yarn run -T postinstall
./gradlew dependencies
```

If you need to run the postinstall step directly, you can recreate and use the Python lock file via pip-compile (PEP 621):

```sh
# Regenerate requirements.txt from PEP 621 dependencies in pyproject.toml
pip-compile -o requirements.txt pyproject.toml

# Then install and set up commit hooks
pip install -r requirements.txt && git config core.hooksPath .config/git/hooks
```

### Committing

Use [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/).

### Releasing

```sh
git tag -m "v0.12.1" -a v0.12.1 && git push --tags
```

## License

All licenses are documented explicitly using SPDX identifiers in their file

- Java: [GPLv3](https://choosealicense.com/licenses/gpl-3.0/)
  with [Classpath Exception](https://spdx.org/licenses/Classpath-exception-2.0.html)
- Gradle Kotlin and Config Files: [MIT](https://choosealicense.com/licenses/mit/)
- Documentation including Javadoc: [CC BY 4.0](https://choosealicense.com/licenses/cc-by-4.0/)

Copyright © 2024 - 2025 Caleb Cushing
