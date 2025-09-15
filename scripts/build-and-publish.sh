#!/bin/bash

set -e # stop script when error occurs
set -u # stop when undefined variable is used

TOP=$(dirname $0)
SRC_ROOT="`( cd \"$TOP/..\" && pwd )`"

function USAGE
{
    echo ""
    echo "Usage: sh build-and-publish.sh [options] repository"
    echo ""
    echo "    This tool helps with library publication to Maven Central"
    echo "    or to local maven cache."
    echo ""
    echo "repository is:"
    echo ""
    echo "  central             Publish Android SDK to Maven Central"
    echo "  local               Publish Android SDK to local Maven cache"
    echo ""
    echo "options:"
    echo ""
    echo "    -s version | --snapshot version"
    echo "                      Set version to version-SNAPSHOT and exit"
    echo ""
    echo "    -ns | --no-sign"
    echo "                      Don't sign artifacts when publishing"
    echo "                      to local Maven cache"
    echo ""
    echo "    -h | --help       print this help information"
    echo ""
    exit $1
}

function FAILURE
{
	echo "Error: $@" 1>&2
	exit 1
}

PUBLISH_GRADLE_TASK=''
TARGET_REPO=''
DO_SIGN=1
GRADLE_PARAMS=''

while [[ $# -gt 0 ]]
do
    opt="$1"
    case "$opt" in
        -ns | --no-sign)
            DO_SIGN=0 ;;
        central | local)
            TARGET_REPO=$opt ;;
        -h | --help)
            USAGE 0 ;;
        *)
            USAGE 1 ;;
    esac
    shift
done

source "library/gradle.properties"
echo -e "\n|----------------------------------------------------------"
echo "| Publishing $ARTIFACT_ID to $TARGET_REPO repository"

echo "| - Version: $VERSION_NAME"
if [ x$DO_SIGN == x1 ]; then
    echo "| - Signing artifacts is enabled."
else
    echo "| - Signing artifacts is disabled."
fi
echo -e "|----------------------------------------------------------\n"
unset VERSION_NAME
unset GROUP_ID
unset ARTIFACT_ID

case "$TARGET_REPO" in
    local)
        PUBLISH_GRADLE_TASK='publishReleasePublicationToMavenLocal'
        ;;  
    central)
        PUBLISH_GRADLE_TASK='publishReleasePublicationToSonatypeRepository'
        ;;
    *)
        FAILURE "You must specify repository where publish to."
esac

# Load signing and releasing credentials
if [ x$DO_SIGN == x1 ]; then
    # Find proper signing tool
    set +e
    HAS_GPG=`which gpg`
    HAS_GPG2=`which gpg2`
    set -e
    
    [[ -z $HAS_GPG ]] && [[ -z $HAS_GPG2 ]] && FAILURE "gpg or gpg2 tool is missing."
    
    # Load and validate API credentials (if exist)
    if [ -f ".credentials" ]; then
        source ".credentials"
    elif [ -f ~/.lime/credentials ]; then # legacy support
        source ~/.lime/credentials
    fi
    [[ x$NEXUS_USER == x ]] && FAILURE "Missing NEXUS_USER variable in API credentials."
    [[ x$NEXUS_PASSWORD == x ]] && FAILURE "Missing NEXUS_PASSWORD variable in API credentials."
    [[ x$SIGN_GPG_KEY_ID == x ]] && FAILURE "Missing SIGN_GPG_KEY_ID variable in API credentials."
    [[ x$SIGN_GPG_KEY_PASS == x ]] && FAILURE "Missing SIGN_GPG_KEY_PASS variable in API credentials."
    [[ x$NEXUS_STAGING_PROFILE_ID == x ]] && FAILURE "Missing NEXUS_STAGING_PROFILE_ID variable in API credentials."

    # Configure gpg for gradle task
    GRADLE_PARAMS+=" -Psigning.gnupg.keyName=$SIGN_GPG_KEY_ID"
    GRADLE_PARAMS+=" -Psigning.gnupg.passphrase=$SIGN_GPG_KEY_PASS"
    if [ ! -z $HAS_GPG ] && [ -z $HAS_GPG2 ]; then
        GRADLE_PARAMS+=" -Psigning.gnupg.executable=gpg"
    fi
    # Configure nexus credentials
    GRADLE_PARAMS+=" -Pnexus.user=${NEXUS_USER}"
    GRADLE_PARAMS+=" -Pnexus.password=${NEXUS_PASSWORD}"
    GRADLE_PARAMS+=" -Pnexus.stagingProfileId=${NEXUS_STAGING_PROFILE_ID}"
else
    [[ $TARGET_REPO == 'central' ]] && FAILURE "Signing is required for publishing to Maven Central."
fi

pushd "${SRC_ROOT}"

GRADLE_CMD_LINE="$GRADLE_PARAMS clean assembleRelease $PUBLISH_GRADLE_TASK"
./gradlew $GRADLE_CMD_LINE

popd
