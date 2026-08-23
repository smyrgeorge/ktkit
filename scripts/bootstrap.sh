#!/usr/bin/env sh

set -e

# Builds the ktkit Gradle plugin and the OpenAPI compiler plugin and publishes them to mavenLocal.
#
# The example module applies the ktkit Gradle plugin by id from mavenLocal (and the plugin pulls
# the compiler-plugin artifact from there too), so both must be published before the main build
# can even configure — run this after a clean checkout and after every version bump.
# --configure-on-demand keeps Gradle from configuring the example module (which would need the
# not-yet-published plugin). RELEASE_SIGNING_ENABLED=false leaves signing out of the publications
# entirely: mavenLocal artifacts need no signatures, and the CI build job has no signing keys
# (excluding the sign tasks with -x is NOT enough — publishing then fails on the missing .asc
# files on a clean workspace).
./gradlew \
    :ktkit-compiler-openapi:publishToMavenLocal \
    :ktkit-gradle-plugin:publishToMavenLocal \
    --configure-on-demand \
    -PRELEASE_SIGNING_ENABLED=false
