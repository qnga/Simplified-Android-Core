#!/bin/sh

GP_GIT_COMMIT=$(git rev-list --max-count=1 HEAD) || exit 1
SIMPLYE_VERSION=$(grep versionCode simplified-app-vanilla/version.properties | sed 's/versionCode=//g') || exit 1

cat <<EOF
Build of ${GP_GIT_COMMIT}

Git commit: ${GP_GIT_COMMIT}
Version code: ${SIMPLYE_VERSION}
Build number: ${TRAVIS_BUILD_NUMBER}

Commit link: https://www.github.com/NYPL-Simplified/android/commit/${GP_GIT_COMMIT}
EOF
