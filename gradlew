#!/bin/sh
# Lightweight launcher: uses system Gradle (CI installs Gradle 8.7 via setup-gradle).
exec gradle "$@"
