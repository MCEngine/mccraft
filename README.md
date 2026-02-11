# MCEngine Plugin Template (Paper)

A multi-module Gradle template for building PaperMC plugins with a clean separation between:

- **API** (public interfaces)
- **Common** (shared/internal logic)
- **Engine** (platform implementation; PaperMC in this template)

This template also includes **MCExtension** support, allowing you to ship optional/plug-in “extensions” that can be discovered and loaded at runtime.

## Project layout

- **`api/`**
  - Public interfaces intended for consumption by other modules and/or extensions.
- **`common/`**
  - Shared implementation utilities used by engines and extensions.
- **`platform/papermc/engine/`**
  - The Paper plugin module (the JAR you drop into your server’s `plugins/` folder).
- **`buildSrc/`**
  - Shared Gradle build logic (versioning + module conventions).

Modules are wired in `settings.gradle`.

## Requirements

- **Java 21** (recommended for Paper 1.21+)
- **Gradle Wrapper** (included: `./gradlew`)

## Build

From the repository root:

```bash
./gradlew build
```

The Paper plugin JAR is produced by the engine module:

- `platform/papermc/engine/build/libs/`

## Run on a Paper server

- Copy the built engine JAR from:
  - `platform/papermc/engine/build/libs/`
- Into your Paper server:
  - `plugins/`
- Start the server.

The plugin generates a default configuration on first run:

- `plugins/<PluginName>/config.yml`

## Configuration

The default `config.yml` includes an example database section supporting SQLite and MySQL.

## MCExtension support

This template’s Paper engine initializes an `MCExtensionManager` and loads extensions on startup.

- The engine registers `MCExtensionManager` as a Bukkit service.
- Extensions are loaded during plugin enable and disabled during plugin disable.

### Dependency / repository note

The engine module depends on **MCExtension**:

- `io.github.mcengine:mcextension:2026.0.3-1-SNAPSHOT`

It is resolved from GitHub Packages. You will likely need credentials available as either:

- Gradle properties:
  - `gpr.user`
  - `gpr.key`
- Or environment variables:
  - `USER_GITHUB_NAME` / `USER_GITHUB_TOKEN`
  - (fallback) `GITHUB_ACTOR` / `GITHUB_TOKEN`

### Example extension

See the example extension repository:

- [Example Extension](https://github.com/MCEngine/mcextension-example)

## Publishing

This repository is set up with `maven-publish` in modules. The root build coordinates publish task ordering so API publishes before Common, then Engine.
