#!/usr/bin/env sh

set -e

# Builds the ktkit Gradle plugin and the OpenAPI compiler plugin and publishes them to mavenLocal.
#
# The example module applies the ktkit Gradle plugin by id from mavenLocal (and the plugin pulls
# the compiler-plugin artifact from there too), so both must be published before the main build
# can even configure — run this after a clean checkout and after every version bump.
# --configure-on-demand keeps Gradle from configuring the example module (which would need the
# not-yet-published plugin).
./gradlew :ktkit-gradle-plugin:publishToMavenLocal :ktkit-openapi-compiler-plugin:publishToMavenLocal --configure-on-demand
