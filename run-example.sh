#!/usr/bin/env sh
set -eu

BUILD_DIR="${TMPDIR:-/tmp}/property-agent-failure-classes"
mkdir -p "$BUILD_DIR"
find src/main/java -name '*.java' -print | xargs javac -d "$BUILD_DIR"
java -cp "$BUILD_DIR" learning.propertyagent.PropertyAgentExample
