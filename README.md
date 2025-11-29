<!--
SPDX-FileCopyrightText: Copyright © 2024 2025 Caleb Cushing
SPDX-FileCopyrightText: Copyright © 2025 Caleb Cushing

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
    implementation(sb.spring.boot.starter.data.jpa.test)
}
```

## FAQ

### Versions

In order to make life easier the first 2 numbers of the version catalog will match the Spring Boot version. The 3rd number will be incremented for any patches or minor changes needed.

### Gradle Support

Gradle Versions older than 7.2 will not work. Gradle 7.4 was the first stable release for version catalogs.

## Development

### Languages

[asdf](https://asdf-vm.com) is suggested, you can use whatever you'd like to get

- Java 17+
- NodeJs
- Python 3.11+

add a way to export these to your `PATH` in your `~/.profile`

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
