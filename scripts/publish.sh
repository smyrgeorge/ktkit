#!/usr/bin/env sh

set -e

# Publish the ktkit Gradle/compiler plugins to mavenLocal first — the example module needs them
# at the (possibly just bumped) current version before the build can configure.
./scripts/bootstrap.sh

./gradlew build

./gradlew :dokka:dokkaGenerate
rm -rf ./docs/*
cp -R ./dokka/build/dokka/html/* ./docs/

version=$(./gradlew properties -q | awk '/^version:/ {print $2}')
git add --all
git commit -m "Added documentation for '$version'."
git push

git tag "$version" -f
git push --tags -f

./gradlew publishAllPublicationsToMavenCentralRepository
git checkout .
