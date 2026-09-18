#!/bin/sh
#
# Gradle start up script for UN*X
#
# Generated for TranslateLens / Gradle 8.5

APP_HOME=$(cd "$(dirname "$0")" && pwd -P)
APP_BASE_NAME=$(basename "$0")
APP_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

exec "$JAVACMD" -jar "$APP_JAR" "$@"
