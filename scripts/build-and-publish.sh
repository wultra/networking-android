#!/bin/bash

set -e # stop script when error occurs
set -u # stop when undefined variable is used

##############################
#
# This script will build and publish the library to the specified repository.
#
# Usage:
#   ./scripts/build-and-publish.sh <repository>
#    where <repository> is either "central" or "local"
#
# To be able to publish to Maven Central, you need `.credentials` file in your home directory
# with required properties or optionally in "~/.wultra/.credentials".
#
##############################

TOP=$(dirname $0)
SRC_ROOT="`( cd \"${TOP}/..\" && pwd )`"

pushd "${SRC_ROOT}" # move to the repo root

TARGET_REPO=$1

source "library/gradle.properties" # load project properties to get version and artifact id
echo -e "\n|----------------------------------------------------------"
echo "| Publishing $ARTIFACT_ID to $TARGET_REPO repository"
echo "| Version: $VERSION_NAME"
echo -e "|----------------------------------------------------------\n"
# Clean up environment from loaded properties
unset VERSION_NAME
unset GROUP_ID
unset ARTIFACT_ID

if [ "${TARGET_REPO}" == "central" ] ; then
    PUBLISH_GRADLE_TASK="publishReleasePublicationToSonatypeRepository"
elif [ "${TARGET_REPO}" == "local" ] ; then
    PUBLISH_GRADLE_TASK="publishReleasePublicationToMavenLocal"
else
    echo "You must specify repository where publish to."
    exit 1
fi

GRADLE_CMD_LINE="assembleRelease $PUBLISH_GRADLE_TASK"
./gradlew $GRADLE_CMD_LINE

popd
