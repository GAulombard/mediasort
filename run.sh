#!/usr/bin/env bash
set -e

check_java() {
    if ! command -v java &> /dev/null; then
        echo "Java not found. Please install Java 21+."
        exit 1
    fi
    JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt 21 ]; then
        echo "Java 21+ required. Current version: $JAVA_VERSION"
        exit 1
    fi
}

build() {
    local needs_rebuild=false
    for arg in "$@"; do
        [ "$arg" == "--rebuild" ] && needs_rebuild=true
    done

    if [ ! -f target/mediasort.jar ] || [ "$needs_rebuild" == "true" ]; then
        echo "Building JAR..."
        mvn clean package -DskipTests -q
        echo "Build complete."
    fi
}

check_java
build "$@"
java -jar target/mediasort.jar "$@"
