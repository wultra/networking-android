#!/bin/bash
###############################################################################
# Include common functions...
# -----------------------------------------------------------------------------
TOP=$(dirname $0)
source "${TOP}/common-functions.sh"
SRC_ROOT="`( cd \"$TOP/..\" && pwd )`"

# -----------------------------------------------------------------------------
# Global variables

GRADLE_PROP="library/gradle.properties"

# -----------------------------------------------------------------------------
# USAGE prints help and exits the script with error code from provided parameter
# Parameters:
#   $1   - error code to be used as return code from the script
# -----------------------------------------------------------------------------
function USAGE
{
    echo ""
    echo "Usage:  $CMD [options] repository"
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
    echo "    -r version | --release version"
    echo "                      Set version to 'version' and exit"
    echo ""
    echo "    -ns | --no-sign"
    echo "                      Don't sign artifacts when publishing"
    echo "                      to local Maven cache"
    echo ""
    echo "    -nc | --no-clean"
    echo "                      Don't clean build before publishing"
    echo ""
    echo "    -v0               turn off all prints to stdout"
    echo "    -v1               print only basic log about build progress"
    echo "    -v2               print full build log with rich debug info"
    echo "    -h | --help       print this help information"
    echo ""
    exit $1
}

# -----------------------------------------------------------------------------
# MAKE_VER sets version or version-SNAPSHOT to gradle.properties file
# Parameters:
#   $1   - version to set
#   $2   - version suffix, for example "SNAPSHOT", or empty
# -----------------------------------------------------------------------------
function MAKE_VER
{
    local VER=$1
    local VER_SUFFIX=$2
    if [ ! -z "$VER_SUFFIX" ]; then
        VER_SUFFIX="-$VER_SUFFIX"
    fi
    local NEW_VER=${VER}${VER_SUFFIX}
    local CUR_VER=$(LOAD_CURRENT_VERSION)
    local PROP_PATH="${SRC_ROOT}/${GRADLE_PROP}"

    VALIDATE_AND_SET_VERSION_STRING "$VER"
    
    [[ "$CUR_VER" == "-1" ]] && FAILURE "Failed to load version from gradle.properties file."
    
    PUSH_DIR "${SRC_ROOT}"
    ####
    # patch version file 
    sed -e "s/$CUR_VER/$NEW_VER/g" "${PROP_PATH}" > "${PROP_PATH}.new"
    $MV "${PROP_PATH}.new" "${PROP_PATH}"
    git add ${GRADLE_PROP}
    ####
    POP_DIR
    
    LOG_LINE
    LOG "Version changed to:"
    PRINT_CURRENT_VERSION 'local'
    LOG_LINE
}

# -----------------------------------------------------------------------------
# LOAD_CURRENT_VERSION loads version from gradle.properties file and prints
# it to stdout.
# -----------------------------------------------------------------------------
function LOAD_CURRENT_VERSION
{
    local PROP_PATH="$SRC_ROOT/${GRADLE_PROP}"
    local V="-1"
    if [ -f "$PROP_PATH" ]; then
        local ver=$(GET_PROPERTY "$PROP_PATH" 'VERSION_NAME')
        if [ ! -z "$ver" ]; then
            V="${ver}"
        fi
    fi
    echo $V
}

# -----------------------------------------------------------------------------
# PRINT_CURRENT_VERSION loads and prints version from gradle.properties file
# Parameters:
#   $1   - target repository (local | central)
# -----------------------------------------------------------------------------
function PRINT_CURRENT_VERSION
{
    local REPO=$1
    local PROP_PATH="$SRC_ROOT/${GRADLE_PROP}"
    
    local VERSION_NAME=$(LOAD_CURRENT_VERSION)
    local GROUP_ID=$(GET_PROPERTY "$PROP_PATH" 'GROUP_ID')
    local ARTIFACT_ID=$(GET_PROPERTY "$PROP_PATH" 'ARTIFACT_ID')
    
    [[ "$VERSION_NAME" == "-1" ]] && FAILURE "Failed to load version from gradle.properties file."
        
    LOG_LINE
    if [ $REPO == 'local' ]; then
        LOG "Going to publish library to local Maven cache"
    else
        LOG "Going to publish library to Sonatype Repository"
    fi
    LOG " - Version     : ${VERSION_NAME}"
    LOG " - Dependency  : ${GROUP_ID}:${ARTIFACT_ID}:${VERSION_NAME}"
    if [ x$DO_SIGN == x1 ]; then
        LOG " - Signed      : YES"
    else
        LOG " - Signed      : NO"
    fi
    if [ x$DO_CLEAN == x ]; then
        LOG " - Clean build : NO"
    else
        LOG " - Clean build : YES"
    fi
}

###############################################################################
# Script's main execution starts here...
# -----------------------------------------------------------------------------
DO_CLEAN='clean'
DO_PUBLISH=''
DO_REPO=''
DO_SIGN=1
GRADLE_PARAMS=''

while [[ $# -gt 0 ]]
do
    opt="$1"
    case "$opt" in
        -s | --snapshot)
            MAKE_VER "$2" 'SNAPSHOT'
            EXIT_SUCCESS
            ;;
        -r | --release)
            MAKE_VER "$2" ""
            EXIT_SUCCESS
            ;;
        -nc | --no-clean)
            DO_CLEAN='' ;;
        -ns | --no-sign)
            DO_SIGN=0 ;;
        central | local)
            DO_REPO=$opt ;;
        -v*)
            SET_VERBOSE_LEVEL_FROM_SWITCH $opt ;;
        -h | --help)
            USAGE 0 ;;
        *)
            USAGE 1 ;;
    esac
    shift
done

case "$DO_REPO" in
    local)
        DO_PUBLISH='publishReleasePublicationToMavenLocal'
        ;;  
    central)
        DO_PUBLISH='publishReleasePublicationToSonatypeRepository'
        ;;
    *)
        FAILURE "You must specify repository where publish to."
esac

if [ $VERBOSE == 2 ]; then
    GRADLE_PARAMS+=' --debug'
fi

# Load signing and releasing credentials
if [ x$DO_SIGN == x1 ]; then
    # Find proper signing tool
    set +e
    HAS_GPG=`which gpg`
    HAS_GPG2=`which gpg2`
    set -e
    
    [[ -z $HAS_GPG ]] && [[ -z $HAS_GPG2 ]] && FAILURE "gpg or gpg2 tool is missing."
    
    # Load and validate API credentials
    LOAD_API_CREDENTIALS
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
    [[ $DO_REPO == 'central' ]] && FAILURE "Signing is required for publishing to Maven Central."
fi

PRINT_CURRENT_VERSION $DO_REPO

PUSH_DIR "${SRC_ROOT}"
####
GRADLE_CMD_LINE="$GRADLE_PARAMS $DO_CLEAN assembleRelease $DO_PUBLISH"
DEBUG_LOG "Gradle command line >> ./gradlew $GRADLE_CMD_LINE"
./gradlew $GRADLE_CMD_LINE
####
POP_DIR

EXIT_SUCCESS