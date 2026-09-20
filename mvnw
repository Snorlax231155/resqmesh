#!/usr/bin/env sh
DIR="$(cd "$(dirname "$0")" && pwd)"
exec "$DIR/.tools/maven/bin/mvn" -Dmaven.repo.local="$DIR/.m2/repository" "$@"
