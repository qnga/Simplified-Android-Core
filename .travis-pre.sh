#!/bin/sh

fatal()
{
  echo "fatal: $1" 1>&2
  echo
  echo "dumping log: " 1>&2
  echo
  cat .travis/pre.txt
  exit 1
}

info()
{
  echo "info: $1" 1>&2
}

if [ -z "${NYPL_NEXUS_USER}" ]
then
  fatal "NYPL_NEXUS_USER is not defined"
fi

if [ -z "${NYPL_NEXUS_PASSWORD}" ]
then
  fatal "NYPL_NEXUS_PASSWORD is not defined"
fi

if [ -z "${NYPL_GITHUB_ACCESS_TOKEN}" ]
then
  fatal "NYPL_GITHUB_ACCESS_TOKEN is not defined"
fi

mkdir -p .travis || fatal "could not create .travis"

info "dumping environment"
export ANDROID_SDK_ROOT="${ANDROID_HOME}"
env | sort -u

#------------------------------------------------------------------------
# Clone repos

info "cloning credentials"

git clone \
  "https://${NYPL_GITHUB_ACCESS_TOKEN}@github.com/NYPL-Simplified/Certificates" \
  ".travis/credentials" \
  >> .travis/pre.txt 2>&1 \
  || fatal "could not clone credentials"

info "installing certificate"

cp -v .travis/credentials/SimplyE/Android/ReaderClientCert.sig \
  simplified-app-simplye/src/main/assets/ReaderClientCert.sig

info "installing keystore"

cp -v ".travis/credentials/SimplyE/APK Signing/nypl-keystore.jks" \
  simplified-app-simplye/keystore.jks

#------------------------------------------------------------------------
# Download avdmanager

info "downloading avdmanager"

yes | sdkmanager tools \
  >> .travis/pre.txt 2>&1 \
  || fatal "could not download avdmanager"

info "avdmanager: $(which avdmanager)"

COMPONENTS="
build-tools;28.0.3
platform-tools
platforms;android-28
tools
"

for COMPONENT in ${COMPONENTS}
do
  info "downloading ${COMPONENT}"

  yes | sdkmanager "${COMPONENT}" \
    >> .travis/pre.txt 2>&1 \
    || fatal "could not download emulator"
done

info "updating all"

yes | sdkmanager --update \
  >> .travis/pre.txt 2>&1 \
  || fatal "could not update platform"

info "agreeing to licenses"

yes | sdkmanager --licenses \
  >> .travis/pre.txt 2>&1 \
  || fatal "could not agree to licenses"

