#!/bin/sh

##############################################################################
# Gradle wrapper script
##############################################################################

GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
WRAPPER_JAR="gradle/wrapper/gradle-wrapper.jar"
WRAPPER_PROPERTIES="gradle/wrapper/gradle-wrapper.properties"

# Check if wrapper jar exists
if [ ! -f "$WRAPPER_JAR" ]; then
    echo "Downloading Gradle Wrapper..."
    
    if command -v curl > /dev/null; then
        curl -fsSL "https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradle/wrapper/gradle-wrapper.jar" -o "$WRAPPER_JAR" || {
            echo "Failed to download Gradle wrapper jar"
            exit 1
        }
    elif command -v wget > /dev/null; then
        wget -q "https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradle/wrapper/gradle-wrapper.jar" -O "$WRAPPER_JAR" || {
            echo "Failed to download Gradle wrapper jar"
            exit 1
        }
    else
        echo "Error: Neither curl nor wget found. Please install Gradle manually."
        exit 1
    fi
fi

# Execute Gradle
exec java -jar "$WRAPPER_JAR" "$@"
