# KtKit

![Build](https://github.com/smyrgeorge/ktkit/actions/workflows/ci.yml/badge.svg)
![Maven Central](https://img.shields.io/maven-central/v/io.github.smyrgeorge/ktkit)
![GitHub License](https://img.shields.io/github/license/smyrgeorge/ktkit)
![GitHub commit activity](https://img.shields.io/github/commit-activity/w/smyrgeorge/ktkit)
![GitHub issues](https://img.shields.io/github/issues/smyrgeorge/ktkit)
[![Kotlin](https://img.shields.io/badge/kotlin-2.3.0-blue.svg?logo=kotlin)](http://kotlinlang.org)

![](https://img.shields.io/static/v1?label=&message=Platforms&color=grey)
![](https://img.shields.io/static/v1?label=&message=Jvm&color=blue)
![](https://img.shields.io/static/v1?label=&message=Linux&color=blue)
![](https://img.shields.io/static/v1?label=&message=macOS&color=blue)
![](https://img.shields.io/static/v1?label=&message=Windows&color=blue)

A comprehensive Kotlin multiplatform toolkit for building server applications with Ktor.

📖 [Documentation](https://smyrgeorge.github.io/ktkit/)

🏠 [Homepage](https://smyrgeorge.github.io/) (under construction)

## Usage

```kotlin
implementation("io.github.smyrgeorge:ktkit:x.y.z")
```

## Overview

KtKit is a Kotlin multiplatform toolkit designed to speed up server-side application development with Ktor.
It brings together several libraries into a cohesive set of tools that handle the repetitive aspects of backend
development.

> [!NOTE]
> **Early Stage Project**: KtKit is actively evolving. APIs may change between versions as we refine the abstractions
> based on real-world usage. Production use is possible but expect some breaking changes. Feedback and contributions are
> highly appreciated!

**What it does:**

- Provides type-safe abstractions for REST APIs, database operations, and message queuing
- Integrates observability from the ground up with structured logging and OpenTelemetry tracing
- Manages dependency injection, configuration, and request context propagation automatically
- Enables functional error handling patterns with Arrow's Either monad
- Supports multiple platforms (JVM, Linux, macOS) through Kotlin Multiplatform

## Features

### Key Technologies

- **Ktor**: HTTP server framework
- **Koin**: Dependency injection
- **log4k**: Structured logging with tracing
- **sqlx4k**: Multiplatform database access
- **Arrow**: Functional error handling with Either monad

## Example

Check the example application [here](example/src/commonMain/kotlin/io/github/smyrgeorge/ktkit/example).

## Building & Development

### Build

```bash
# Build all modules, for Jvm and your current platform
./gradlew build

# Build all modules for all supported platforms
./gradlew build -Ptargets=all
#
```

### Docker Setup

The project includes a `docker-compose.yml` for PostgreSQL:

```bash
docker-compose up -d
```

## Contributing

This is an open-source project. Contributions are welcome!

## License

Check the repository for license information.

## Related Projects

- [log4k](https://github.com/smyrgeorge/log4k) – Multiplatform logging with tracing
- [sqlx4k](https://github.com/smyrgeorge/sqlx4k) – Multiplatform database access

## Author

Yorgos S. ([@smyrgeorge](https://github.com/smyrgeorge))
